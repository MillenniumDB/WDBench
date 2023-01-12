import re
import sys

# this scripts converts filtered wikidata truthy into 3 .csv files
# that can be imported into Neo4J
# Usage: python nt_to_neo4j.py [path_to_nt_file]

if len(sys.argv) < 2:
    print("pass the file path as argument")
    exit(1)

input_fname = sys.argv[1]

s_exp = re.compile(r"^<http://www\.wikidata\.org/entity/(\w\d+)>$")

p_exp = re.compile(r"^<http://www\.wikidata\.org/prop/direct/(\w\d+)>$")

# entity
o1_exp = re.compile(r"^<http://www\.wikidata\.org/entity/(\w\d+)>$")

# string
o2_exp = re.compile(r'^"((?:[^"\\]|\\.)*)"$')

# something with schema
o3_exp = re.compile(r'^"((?:[^"\\]|\\.)*)"\^\^<http://www\.w3\.org/2001/XMLSchema#\w+>$')

# string with idiom
o4_exp = re.compile(r'^"((?:[^"\\]|\\.)*)"@(.+)$')

# other url
o5_exp = re.compile(r"^<(.+)>$")

# point
o6_exp = re.compile(r'^"((?:[^"\\]|\\.)*)"\^\^<http://www\.opengis\.net/ont/geosparql#wktLiteral>$')

# anon
o7_exp = re.compile(r'^_:\w+$')

# math
o8_exp = re.compile(r'^"((?:[^"\\]|\\.)*)"\^\^<http://www\.w3\.org/1998/Math/MathML>$')


with open(input_fname, 'r', encoding='utf-8') as input_file, \
     open("entities.csv", 'w', encoding='utf-8') as entities_file, \
     open("literals.csv", 'w', encoding='utf-8') as literals_file, \
     open("edges.csv", 'w', encoding='utf-8') as edges_file:
    entities_file.write('id:ID\n')
    literals_file.write('id:ID,value\n')
    edges_file.write(':START_ID,:END_ID,:TYPE\n')
    literals_id_counter = 0
    entities = set()
    for line in input_file:
        l = line.split(' ')
        s = l[0]
        p = l[1]
        o = ' '.join(l[2:-1])

        m_s = s_exp.match(s)
        subject_entity = m_s.groups()[0]
        if subject_entity not in entities: # to avoid adding duplicates
            entities.add(subject_entity)
            entities_file.write(f'{subject_entity}\n')

        m_p = p_exp.match(p)
        property = m_p.groups()[0]

        m_o = o1_exp.match(o) # entity
        if m_o is not None:
            object_entity = m_o.groups()[0]
            if object_entity not in entities: # to avoid adding duplicates
                entities.add(object_entity)
                entities_file.write(f'{object_entity}\n')

            edges_file.write(f'{subject_entity},{object_entity},{property}\n')
            continue

        m_o = o2_exp.match(o) # string
        if m_o is not None:
            literals_id_counter += 1
            literal = m_o.groups()[0].replace('"', '""')
            literals_file.write(f'{literals_id_counter},"{literal}"\n')
            edges_file.write(f'{subject_entity},{literals_id_counter},{property}\n')
            continue

        m_o = o3_exp.match(o)
        if m_o is not None:
            literals_id_counter += 1
            literal = m_o.groups()[0].replace('"', '""')
            literals_file.write(f'{literals_id_counter},"{literal}"\n')
            edges_file.write(f'{subject_entity},{literals_id_counter},{property}\n')
            continue

        m_o = o4_exp.match(o)
        if m_o is not None:
            # TODO: idiom is ignored
            literals_id_counter += 1
            literal = m_o.groups()[0].replace('"', '""')
            literals_file.write(f'{literals_id_counter},"{literal}"\n')
            edges_file.write(f'{subject_entity},{literals_id_counter},{property}\n')
            continue

        m_o = o5_exp.match(o)
        if m_o is not None:
            literals_id_counter += 1
            literal = m_o.groups()[0].replace('"', '""')
            literals_file.write(f'{literals_id_counter},"{literal}"\n')
            edges_file.write(f'{subject_entity},{literals_id_counter},{property}\n')
            continue

        m_o = o6_exp.match(o)
        if m_o is not None:
            literals_id_counter += 1
            literal = m_o.groups()[0].replace('"', '""')
            literals_file.write(f'{literals_id_counter},"{literal}"\n')
            edges_file.write(f'{subject_entity},{literals_id_counter},{property}\n')
            continue

        m_o = o7_exp.match(o)
        if m_o is not None:
            literals_id_counter += 1
            literals_file.write(f'{literals_id_counter},""\n')
            edges_file.write(f'{subject_entity},{literals_id_counter},{property}\n')
            continue

        m_o = o8_exp.match(o)
        if m_o is not None:
            literals_id_counter += 1
            literal = m_o.groups()[0].replace('"', '""')
            literals_file.write(f'{literals_id_counter},"{literal}"\n')
            edges_file.write(f'{subject_entity},{literals_id_counter},{property}\n')
            continue
