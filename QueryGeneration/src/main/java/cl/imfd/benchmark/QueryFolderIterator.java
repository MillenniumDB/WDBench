package cl.imfd.benchmark;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QueryFolderIterator extends QueryIterator {

	private static String PREFIXES = "# list of prefixes for import\n"
			+ "PREFIX bd: <http://www.bigdata.com/rdf#>\n"
			+ "PREFIX cc: <http://creativecommons.org/ns#>\n"
			+ "PREFIX dct: <http://purl.org/dc/terms/>\n"
			+ "PREFIX geo: <http://www.opengis.net/ont/geosparql#>\n"
			+ "PREFIX ontolex: <http://www.w3.org/ns/lemon/ontolex#>\n"
			+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
			+ "PREFIX p: <http://www.wikidata.org/prop/>\n"
			+ "PREFIX pq: <http://www.wikidata.org/prop/qualifier/>\n"
			+ "PREFIX pqn: <http://www.wikidata.org/prop/qualifier/value-normalized/>\n"
			+ "PREFIX pqv: <http://www.wikidata.org/prop/qualifier/value/>\n"
			+ "PREFIX pr: <http://www.wikidata.org/prop/reference/>\n"
			+ "PREFIX prn: <http://www.wikidata.org/prop/reference/value-normalized/>\n"
			+ "PREFIX prov: <http://www.w3.org/ns/prov#>\n"
			+ "PREFIX prv: <http://www.wikidata.org/prop/reference/value/>\n"
			+ "PREFIX ps: <http://www.wikidata.org/prop/statement/>\n"
			+ "PREFIX psn: <http://www.wikidata.org/prop/statement/value-normalized/>\n"
			+ "PREFIX psv: <http://www.wikidata.org/prop/statement/value/>\n"
			+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX schema: <http://schema.org/>\n"
			+ "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n"
			+ "PREFIX wd: <http://www.wikidata.org/entity/>\n"
			+ "PREFIX wdata: <http://www.wikidata.org/wiki/Special:EntityData/>\n"
			+ "PREFIX wdno: <http://www.wikidata.org/prop/novalue/>\n"
			+ "PREFIX wdref: <http://www.wikidata.org/reference/>\n"
			+ "PREFIX wds: <http://www.wikidata.org/entity/statement/>\n"
			+ "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n"
			+ "PREFIX wdtn: <http://www.wikidata.org/prop/direct-normalized/>\n"
			+ "PREFIX wdv: <http://www.wikidata.org/value/>\n"
			+ "PREFIX wikibase: <http://wikiba.se/ontology#>\n"
			+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>";

    private String mQueryFolder;

    public QueryFolderIterator(String queryFolder) {
        mQueryFolder = queryFolder;
    }

    private Set<String> getQueryFolderIterator() throws IOException{
        try (Stream<Path> stream = Files.list(Paths.get(mQueryFolder))) {
            return stream
                .filter(file -> !Files.isDirectory(file))
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.toSet());
        }
    }

	@Override
	public Iterator<String> iterator() {
		Iterator<String> it = null;
        try {
            it = new Iterator<String>() {
                private Set<String> queryFiles = getQueryFolderIterator();

                private String currentQuery;

                private Iterator<String> queries = queryFiles.stream().iterator();

                @Override
                public boolean hasNext() {
                	try {
                        while( queries.hasNext() ){
                            String nextQueryFile = mQueryFolder + "/" + (String) queries.next();
                            currentQuery = new String(Files.readAllBytes(Paths.get(nextQueryFile)), StandardCharsets.UTF_8);
                            return true;
                        }
                        return false;
            		} catch (IOException e) {
            			e.printStackTrace();
            			return false;
            		}
                }

                @Override
                public String next() {
                    return PREFIXES + currentQuery;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
            return it;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return it;
	}
}
