from SPARQLWrapper import SPARQLWrapper, JSON
from socket import timeout
import multiprocessing
import os
import re
import subprocess
import sys
import time
import traceback

# Usage:
# python benchmark.py <ENGINE> <QUERIES_FILE_ABSOLUTE_PATH> <LIMIT> <PREFIX_NAME>
# LIMIT = 0 will not add a limit

# Db engine that will execute queries
ENGINE       = sys.argv[1]
QUERIES_FILE = sys.argv[2]
LIMIT        = sys.argv[3]
PREFIX_NAME  = sys.argv[4]

###################### EDIT THIS PARAMETERS ######################
TIMEOUT = 60 # Max time per query in seconds
BENCHMARK_ROOT = '/data2/benchmark'

# Path to needed output and input files
RESUME_FILE = f'{BENCHMARK_ROOT}/results/{PREFIX_NAME}_{ENGINE}_limit_{LIMIT}.csv'
ERROR_FILE  = f'{BENCHMARK_ROOT}/results/errors/{PREFIX_NAME}_{ENGINE}_limit_{LIMIT}.log'

SERVER_LOG_FILE  = f'{BENCHMARK_ROOT}/scripts/log/{PREFIX_NAME}_{ENGINE}_limit_{LIMIT}.log'

VIRTUOSO_LOCK_FILE = f'{BENCHMARK_ROOT}/virtuoso/wikidata/virtuoso.lck'

# use absolute paths to avoid problems with current directory
ENGINES_PATHS = {
    'BLAZEGRAPH': f'{BENCHMARK_ROOT}/blazegraph/service',
    'JENA':       f'{BENCHMARK_ROOT}/jena',
    'JENA-HDT':   f'{BENCHMARK_ROOT}/jena-hdt',
    'VIRTUOSO':   f'{BENCHMARK_ROOT}/virtuoso',
    'QLEVER':     f'{BENCHMARK_ROOT}/qlever',
    'RDF4J':      f'{BENCHMARK_ROOT}/rdf4j',
}

ENGINES_PORTS = {
    'BLAZEGRAPH': 9999,
    'JENA':       3030,
    'JENA-HDT':   3030,
    'VIRTUOSO':   1111,
    'QLEVER':     7001,
    'RDF4J':      7001,
}

ENDPOINTS = {
    'BLAZEGRAPH': 'http://localhost:9999/bigdata/namespace/wdq/sparql',
    'JENA':       'http://localhost:3030/jena/sparql',
    'JENA-HDT':   'http://localhost:3030/wikidata-hdt-service/sparql',
    'VIRTUOSO':   'http://localhost:8890/sparql',
    'QLEVER':     'http://localhost:7001/sparql',
    'RDF4J':      'http://localhost:7001/sparql',
}

SERVER_CMD = {
    'BLAZEGRAPH': ['./runBlazegraph.sh'],
    'JENA': f'java -Xmx64g -jar apache-jena-fuseki-4.1.0/fuseki-server.jar --loc=apache-jena-4.1.0/wikidata --timeout={TIMEOUT*1000} /jena'.split(' '),
    'JENA-HDT': f'java -Xmx64g -jar apache-jena-fuseki-4.1.0-hdt/fuseki-server.jar --timeout={TIMEOUT*1000} /jena'.split(' '),
    'VIRTUOSO': ['bin/virtuoso-t', '-c', 'wikidata.ini', '+foreground'],
    'QLEVER': f'TIMEOUT=600; PORT=7001; docker run --rm -v $QLEVER_HOME/qlever-indices/wikidata:/index  -p $PORT:7001 -e INDEX_PREFIX=wikidata --name qlever.wikidata qlever-docker',
    'RDF4J': f'TIMEOUT=600; PORT=7001; docker run --rm -v $QLEVER_HOME/qlever-indices/wikidata:/index  -p $PORT:7001 -e INDEX_PREFIX=wikidata --name qlever.wikidata qlever-docker',
}
#######################################################

PORT = ENGINES_PORTS[ENGINE]

server_log = open(SERVER_LOG_FILE, 'w')
server_process = None

# Check if output file already exists
if os.path.exists(RESUME_FILE):
    print(f'File {RESUME_FILE} already exists.')
    sys.exit()

# ================== Auxiliars ===============================
def lsof(pid):
    process = subprocess.Popen(['lsof', '-a', f'-p{pid}', f'-i:{PORT}', '-t'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out, _ = process.communicate()
    return out.decode('UTF-8').rstrip()

def lsofany():
    process = subprocess.Popen(['lsof', '-t', f'-i:{PORT}'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out, _ = process.communicate()
    return out.decode('UTF-8').rstrip()


# ================== Parsers =================================
def parse_to_sparql(query):
    if not LIMIT:
        return f'SELECT * WHERE {{ {query} }}'
    return f'SELECT * WHERE {{ {query} }} LIMIT {LIMIT}'


def IRI_to_mdb(iri):
    expressions = []

    # property
    expressions.append(re.compile(r"^<http://www\.wikidata\.org/prop/direct/([QqPp]\d+)>$"))

    # entity
    expressions.append(re.compile(r"^<http://www\.wikidata\.org/entity/([QqPp]\d+)>$"))

    # string
    expressions.append(re.compile(r'^("(?:[^"\\]|\\.)*")$'))

    # something with schema
    expressions.append(re.compile(r'^("(?:[^"\\]|\\.)*")\^\^<http://www\.w3\.org/2001/XMLSchema#\w+>$'))

    # string with idiom
    expressions.append(re.compile(r'^"((?:[^"\\]|\\.)*)"@(.+)$'))

    # point
    expressions.append(re.compile(r'^"((?:[^"\\]|\\.)*)"\^\^<http://www\.opengis\.net/ont/geosparql#wktLiteral>$'))

    # anon
    expressions.append(re.compile(r'^_:\w+$'))

    # math
    expressions.append(re.compile(r'^"((?:[^"\\]|\\.)*)"\^\^<http://www\.w3\.org/1998/Math/MathML>$'))

    for expression in expressions:
        match_iri = expression.match(iri)
        if match_iri is not None:
            return match_iri.groups()[0]


    # other url
    other_expression = re.compile(r"^<(.+)>$")
    match_iri = other_expression.match(iri)
    if match_iri is not None:
        return f'"{match_iri.groups()[0]}"'
    else:
        raise Exception(f'unhandled iri: {iri}')


def start_server():
    global server_process
    os.chdir(ENGINES_PATHS[ENGINE])
    print('starting server...')

    server_log.write("[start server]\n")
    server_process = subprocess.Popen(SERVER_CMD[ENGINE], stdout=server_log, stderr=server_log)
    print(f'pid: {server_process.pid}')

    # Sleep to wait server start
    while not lsof(server_process.pid):
        time.sleep(1)

    print(f'done')


def kill_server():
    global server_process
    print(f'killing server[{server_process.pid}]...')
    server_log.write("[kill server]\n")
    if ENGINE == 'VIRTUOSO':
        kill_process = subprocess.Popen([f'{ENGINES_PATHS[ENGINE]}/bin/isql', f'localhost:{PORT}', '-K'])
        kill_process.wait()
    else:
        server_process.kill()
        server_process.wait()

    while lsof(server_process.pid):
        time.sleep(1)

    if ENGINE == 'VIRTUOSO':
        kill_process = subprocess.Popen(['rm', VIRTUOSO_LOCK_FILE], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        kill_process.wait()
    print('done')


# Send query to server
def execute_queries():
    with open(QUERIES_FILE) as queries_file:
        for line in queries_file:
            query_number, query = line.split(',')
            print(f'Executing query {query_number}')
            query_sparql(query, query_number)


def execute_sparql_wrapper(query_pattern, query_number):
    query = parse_to_sparql(query_pattern)

    sparql_wrapper = SPARQLWrapper(ENDPOINTS[ENGINE])
    # sparql_wrapper.setTimeout(TIMEOUT+10) # Give 10 more seconds for a chance to graceful timeout
    sparql_wrapper.setReturnFormat(JSON)
    sparql_wrapper.setQuery(query)

    count = 0
    start_time = time.time()

    try:
        # Compute query
        results = sparql_wrapper.query()
        json_results = results.convert()
        for _ in json_results["results"]["bindings"]:
            count += 1

        elapsed_time = int((time.time() - start_time) * 1000) # Truncate to milliseconds

        with open(RESUME_FILE, 'a') as file:
            file.write(f'{query_number},{count},OK,{elapsed_time}\n')

    except Exception as e:
        elapsed_time = int((time.time() - start_time) * 1000) # Truncate to milliseconds
        with open(RESUME_FILE, 'a') as file:
            file.write(f'{query_number},,ERROR({type(e).__name__}),{elapsed_time}\n')

        with open(ERROR_FILE, 'a') as file:
            file.write(f'Exception in query {str(query_number)} [{type(e).__name__}]: {str(e)}\n')


def query_sparql(query_pattern, query_number):
    start_time = time.time()

    try:
        p = multiprocessing.Process(target=execute_sparql_wrapper, args=[query_pattern, query_number])
        p.start()
        # Give 2 more seconds for a chance to graceful timeout or enumerate the results
        p.join(TIMEOUT + 2)
        if p.is_alive():
            p.kill()
            p.join()
            raise Exception("PROCESS_TIMEOUT")

    except Exception as e:
        elapsed_time = int((time.time() - start_time) * 1000) # Truncate to milliseconds
        with open(RESUME_FILE, 'a') as file:
            file.write(f'{query_number},,TIMEOUT({type(e).__name__}),{elapsed_time}\n')

        with open(ERROR_FILE, 'a') as file:
            file.write(f'Exception in query {str(query_number)} [{type(e).__name__}]: {str(e)}\n')

        kill_server()
        start_server()


with open(RESUME_FILE, 'w') as file:
    file.write('query_number,results,status,time\n')

with open(ERROR_FILE, 'w') as file:
    file.write('') # to replaces the old error file

if lsofany():
    raise Exception("other server already running")

print('benchmark is starting. TIMEOUT', TIMEOUT, 'seconds')
start_server()
execute_queries()

if server_process is not None:
    kill_server()

server_log.close()
