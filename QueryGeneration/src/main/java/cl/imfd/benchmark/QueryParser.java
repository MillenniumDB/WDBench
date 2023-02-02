package cl.imfd.benchmark;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;

public class QueryParser {

	public static void main(String[] args) throws UnsupportedEncodingException, IOException {
		QueryIterator queryIter = new QueryIterator();
		if (args.length > 0) {
			queryIter = new QueryFolderIterator(args[0]);
		}
		int parseErrors = 0;
		int i = 0;
		FileWriter pathsFile = new FileWriter("wikidata_advanced_examples.txt");
		for (String query : queryIter) {
			try {
				Query parsedQuery = QueryFactory.create(query);
				pathsFile.write(i + "," + parsedQuery.serialize().replace('\n', ' ') + '\n');
				i += 1;
			} catch (QueryParseException e) {
//				System.out.println(e.getMessage());
				parseErrors += 1;
				continue;
			} catch (QueryException e) {
				continue;
			}
		}
		System.out.println("Parse errors: " + parseErrors);
		pathsFile.close();
	}
}
