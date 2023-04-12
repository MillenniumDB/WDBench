package cl.imfd.benchmark;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.TreeSet;

import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.algebra.AlgebraGenerator;
import org.apache.jena.sparql.algebra.Op;

public class ExtractPath {

	public static void main(String[] args) throws UnsupportedEncodingException, IOException {
		TreeSet<String> pathQueries = new TreeSet<String>();

		QueryIterator queryIter = new QueryIterator();
		if (args.length > 0){
			queryIter = new QueryFolderIterator(args[0]);
		}

		for (String query : queryIter) {
			Op op = null;

			try {
				op = (new AlgebraGenerator()).compile(QueryFactory.create(query));
			} catch (QueryParseException e) {
				continue;
			} catch (QueryException e) {
				continue;
			}
			ExtractPathVisitor visitor = new ExtractPathVisitor();
			op.visit(visitor);

			if (!visitor.hasUnsupportedOp) {
				for (String path_str : visitor.pathsSet) {
					pathQueries.add(path_str);
				}
			}
		}

		// Write Paths into file
		try {
			FileWriter pathsFile = new FileWriter("paths.txt");

			int count = 0;
			for (String query : pathQueries) {
				count++;
				pathsFile.write(Integer.toString(count));
				pathsFile.write(',');
				pathsFile.write(query);
				pathsFile.write('\n');
			}

			pathsFile.close();
	    } catch (IOException e) {
	    	System.out.println("An error occurred.");
	    	e.printStackTrace();
	    }
	}
}
