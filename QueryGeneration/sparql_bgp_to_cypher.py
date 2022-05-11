import sys
import os
import re

def IRI_to_neo(iri):
    entity_expressions = []
    expressions = []

    # property
    entity_expressions.append(re.compile(r"^<http://www\.wikidata\.org/prop/direct/([QqPp]\d+)>$"))

    # entity
    entity_expressions.append(re.compile(r"^<http://www\.wikidata\.org/entity/([QqPp]\d+)>$"))

    for expression in entity_expressions:
        match_iri = expression.match(iri)
        if match_iri is not None:
            return match_iri.groups()[0], True

    # string
    expressions.append(re.compile(r'^"((?:[^"\\]|\\.)*)"$'))

    # something with schema
    expressions.append(re.compile(r'^"((?:[^"\\]|\\.)*)"\^\^<http://www\.w3\.org/2001/XMLSchema#\w+>$'))

    # string with idiom
    expressions.append(re.compile(r'^"((?:[^"\\]|\\.)*)"@(.+)$'))

    # other url
    expressions.append(re.compile(r"^<(.+)>$"))

    for expression in expressions:
        match_iri = expression.match(iri)
        if match_iri is not None:
            return match_iri.groups()[0], False

    raise Exception(f'unhandled iri: {iri}')


def execute_query(query, query_number):
    # print(f'Executing query {query_number}')
    # Remove the ' .' at the end of the string
    query = query.strip()[:-2]

    # Split into triples:
    triples = query.split(' . ')

    basic_patterns = []
    variables = set()
    for triple in triples:
        s, p, o = triple.split(' ')

        if s[0] == '?':
            s = s[1:]
            variables.add(s + '.id')
        else:
            id, is_entity = IRI_to_neo(s)
            if is_entity:
                s = f':Entity {{id:"{id}"}}'
            else:
                s = f'{{id:"{id}"}}'

        if p[0] == '?':
            p = p[1:]
            variables.add(p + '.id')
        else:
            id, is_entity = IRI_to_neo(p)
            if not is_entity:
                raise Exception(f'Bad predicate: {p}')
            p = f':{id}'

        if o[0] == '?':
            o = o[1:]
            variables.add(o + '.id')
        else:
            id, is_entity = IRI_to_neo(o)
            if is_entity:
                o = f':Entity {{id:"{id}"}}'
            else:
                o = f'{{id:"{id}"}}'

        basic_patterns.append(f'({s})-[{p}]->({o})')

    match_pattern = ','.join(basic_patterns)
    select_variables = ','.join(variables)
    cypher_query = f'MATCH {match_pattern} RETURN { select_variables }'

    print(f'{query_number},{cypher_query}')


QUERIES_FILE = sys.argv[1]

with open(QUERIES_FILE, 'r', encoding='UTF-8') as queries_file:
    for line in queries_file:
        query_number, query = line.split(',')
        execute_query(query, query_number)