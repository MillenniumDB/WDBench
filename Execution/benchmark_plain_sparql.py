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
# python benchmark_plain_sparql.py <ENGINE> <QUERIES_FILE_ABSOLUTE_PATH> <PREFIX_NAME> <DISK>

# Db engine that will execute queries
ENGINE       = sys.argv[1]
QUERIES_FILE = sys.argv[2]
PREFIX_NAME  = sys.argv[3]
DISK         = sys.argv[4]

###################### EDIT THIS PARAMETERS ######################
TIMEOUT = 60 # Max time per query in seconds
BENCHMARK_ROOT = '/home/cbuil/benchmark'

# Path to needed output and input files
RESUME_FILE = f'{BENCHMARK_ROOT}/results/{PREFIX_NAME}_{ENGINE}_{DISK}.csv'
ERROR_FILE  = f'{BENCHMARK_ROOT}/results/errors/{PREFIX_NAME}_{ENGINE}_{DISK}.log'

SERVER_LOG_FILE  = f'{BENCHMARK_ROOT}/scripts/log/{PREFIX_NAME}_{ENGINE}_{DISK}.log'

VIRTUOSO_LOCK_FILE = f'{BENCHMARK_ROOT}/virtuoso/wikidata/virtuoso.lck'

# use absolute paths to avoid problems with current directory
ENGINES_PATHS = {
    'BLAZEGRAPH': f'{BENCHMARK_ROOT}/blazegraph/service',
    'JENA':       f'{BENCHMARK_ROOT}/jena/apache-jena-fuseki-4.7.0',
    'JENA-HDT':   f'{BENCHMARK_ROOT}/jena-hdt/apache-jena-fuseki-4.7.0',
    'VIRTUOSO':   f'{BENCHMARK_ROOT}/virtuoso',
    'QLEVER':     f'{BENCHMARK_ROOT}/qlever',
    'RDF4J':      f'{BENCHMARK_ROOT}/rdf4j/apache-tomcat-9.0.70',
    'QENDPOINT':  f'{BENCHMARK_ROOT}/qEndpoint/qendpoint-backend',
}

ENGINES_PORTS = {
    'BLAZEGRAPH': 9999,
    'JENA':       3030,
    'JENA-HDT':   3030,
    'VIRTUOSO':   1111,
    'QLEVER':     7001,
    'RDF4J':      8080,
    'QENDPOINT':  1235,
}

ENDPOINTS = {
    'BLAZEGRAPH': 'http://localhost:9999/bigdata/namespace/wdq/sparql',
    'JENA':       'http://localhost:3030/jena/sparql',
    'JENA-HDT':   'http://localhost:3030/wikidata-hdt-service/query',
    'VIRTUOSO':   'http://localhost:8890/sparql',
    'QLEVER':     'http://localhost:7001/sparql',
    'RDF4J':      'http://localhost:8080/rdf4j-server/repositories/wikidata',
    'QENDPOINT':  'http://localhost:1235/api/endpoint/sparql',
}

SERVER_CMD = {
    'BLAZEGRAPH': ['./runBlazegraph.sh'],
    'JENA': f'java -Xmx64g -jar fuseki-server.jar --loc=wikidata --timeout={TIMEOUT*1000} /jena'.split(' '),
    'JENA-HDT': ['./fuseki-server'],
    'VIRTUOSO': ['bin/virtuoso-t', '-c', 'wikidata.ini', '+foreground'],
    'QLEVER': f'build/ServerMain -i qlever-indices/wikidata/wikidata -c 64 -m 70 -e 5 -j 8 -p 7001 /qlever'.split(' '),
    'RDF4J': ['./bin/catalina.sh', 'start'],
    'QENDPOINT': f'java -Xmx64g -jar target/qendpoint-backend-1.12.0-exec.jar /qendpoint'.split(' '),
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
    if ENGINE == 'RDF4J':
        process = subprocess.Popen(['lsof', '-i', f'-i:{PORT}'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out, _ = process.communicate()
    return out.decode('UTF-8').rstrip()


def lsofany():
    process = subprocess.Popen(['lsof', '-t', f'-i:{PORT}'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out, _ = process.communicate()
    return out.decode('UTF-8').rstrip()


def start_server():
    global server_process
    os.chdir(ENGINES_PATHS[ENGINE])
    print('starting server...')

    server_log.write("[start server]\n")
    server_process = subprocess.Popen(SERVER_CMD[ENGINE], stdout=server_log, stderr=server_log)

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
    elif ENGINE == 'RDF4J':
        kill_process = subprocess.Popen([f'{ENGINES_PATHS[ENGINE]}/bin/shutdown.sh'])
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


def execute_sparql_wrapper(query, query_number):
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

        print(count)
        elapsed_time = int((time.time() - start_time) * 1000) # Truncate to milliseconds

        with open(RESUME_FILE, 'a') as file:
            file.write(f'{query_number},{count},OK,{elapsed_time}\n')

    except Exception as e:
        elapsed_time = int((time.time() - start_time) * 1000) # Truncate to milliseconds
        with open(RESUME_FILE, 'a') as file:
            file.write(f'{query_number},,ERROR({type(e).__name__}),{elapsed_time}\n')

        with open(ERROR_FILE, 'a') as file:
            file.write(f'Exception in query {str(query_number)} [{type(e).__name__}]: {str(e)}\n')


def query_sparql(query, query_number):
    start_time = time.time()

    try:
        p = multiprocessing.Process(target=execute_sparql_wrapper, args=[query, query_number])
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