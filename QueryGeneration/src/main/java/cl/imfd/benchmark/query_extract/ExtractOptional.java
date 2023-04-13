package cl.imfd.benchmark.query_extract;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.TreeSet;

import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.algebra.AlgebraGenerator;
import org.apache.jena.sparql.algebra.Op;

import cl.imfd.benchmark.QueryIterator;
import cl.imfd.benchmark.QueryFolderIterator;
import cl.imfd.benchmark.visitors.ExtractOptionalVisitor;

public class ExtractOptional {

	public static void main(String[] args) throws UnsupportedEncodingException, IOException {
		TreeSet<String> optQueries = new TreeSet<String>();

		QueryIterator queryIter = new QueryIterator();
		if (args.length > 0) {
			queryIter = new QueryFolderIterator(args[0]);
		}

		for (String query : queryIter) {
			Op op = null;
			ExtractOptionalVisitor visitor = new ExtractOptionalVisitor();

			try {
				op = (new AlgebraGenerator()).compile(QueryFactory.create(query));
				op.visit(visitor);
			} catch (QueryParseException e) {
				continue;
			} catch (QueryException e) {
				continue;
			}
			if (!visitor.hasUnsupportedOp && visitor.optionalNode.bgp.size() > 0
					&& visitor.optionalNode.optionalChildren.size() > 0
					&& !visitor.hasCrossProduct()) {
				String str = visitor.optionalNode.toString();

				visitor.optionalNode.toString(); // sets properly visitor.optionalNode.crossProduct
				if (!visitor.optionalNode.crossProduct) {
					optQueries.add(str);
				}
			}
		}

		// Write queries into file
		try {
			FileWriter optsFile = new FileWriter("opts.txt");

			int count = 0;
			for (String query : optQueries) {
				count++;
				optsFile.write(Integer.toString(count));
				optsFile.write(',');
				optsFile.write(query);
				optsFile.write('\n');
			}

			optsFile.close();
		} catch (IOException e) {
			System.out.println("An error occurred writing the output.");
			e.printStackTrace();
		}
	}
}
