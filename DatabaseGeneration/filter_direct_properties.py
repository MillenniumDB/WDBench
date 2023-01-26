import sys

# usage: python [original_truthy] [output_filename]

input_fname = sys.argv[1]
output_fname = sys.argv[2]

with open(input_fname, 'r', encoding='utf-8') as input_file, \
     open(output_fname, 'w', encoding='utf-8') as output_file:
    for line in input_file:
        l = line.split(' ')
        # s = l[0]
        p = l[1]
        # o = ' '.join(l[2:-1])
        if "http://www.wikidata.org/prop/direct/" in p:
            output_file.write(line)
