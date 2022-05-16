from neo4j import GraphDatabase
import time
import sys
import os
import re

# Usage:
# python cypher_benchmark.py <QUERIES_FILEPATH> <LIMIT> <PREFIX_NAME>
# LIMIT = 0 will not add a limit

QUERIES_FILE = sys.argv[1]
LIMIT        = sys.argv[2]
PREFIX_NAME  = sys.argv[3]

########### EDIT THIS PARAMETERS ###########
BENCHMARK_ROOT = '/data2/benchmark'
RESUME_FILE = f'{BENCHMARK_ROOT}/results/{PREFIX_NAME}_NEO4J_limit_{LIMIT}.csv'
NEO4J_DATABSE = 'wikidata'
############################################

# Check if output file already exists
if os.path.exists(RESUME_FILE):
    print(f'File {RESUME_FILE} already exists.')
    sys.exit()
else:
    with open(RESUME_FILE, 'w', encoding='UTF-8') as file:
        file.write('query_number,results,status,time\n')


def execute_query(session, cypher_query, query_number):
    if LIMIT:
        cypher_query += f' LIMIT {LIMIT}'

    result_count = 0
    start_time = time.time()
    try:
        print("executing query", query_number)
        results = session.read_transaction(lambda tx: tx.run(cypher_query).data())
        for _ in results:
            result_count += 1
        elapsed_time = int((time.time() - start_time) * 1000) # Truncate to miliseconds
        with open(RESUME_FILE, 'a', encoding='UTF-8') as output_file:
            output_file.write(f'{query_number},{result_count},OK,{elapsed_time}\n')

    except Exception as e:
        elapsed_time = int((time.time() - start_time) * 1000) # Truncate to miliseconds
        with open(RESUME_FILE, 'a', encoding='UTF-8') as output_file:
            output_file.write(f'{query_number},{result_count},ERROR({type(e).__name__}),{elapsed_time}\n')
        print(e)



with open(QUERIES_FILE, 'r', encoding='UTF-8') as queries_file:
    driver = GraphDatabase.driver('bolt://127.0.0.1:7687')
    with driver.session(database=NEO4J_DATABSE) as session:
        for line in queries_file:
            splited_by_comma = line.split(',')
            query_number = splited_by_comma[0]
            query = ','.join(splited_by_comma[1:])
            execute_query(session, query, query_number)
    driver.close()