package cl.imfd.benchmark.query_extract;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.TreeSet;

import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;

import cl.imfd.benchmark.QueryIterator;
import cl.imfd.benchmark.QueryFolderIterator;
import cl.imfd.benchmark.visitors.ExtractBGPsVisitor;

public class ExtractBGPs {
	public static void main(String[] args) throws UnsupportedEncodingException, IOException {
		TreeSet<String> multipleBGP = new TreeSet<String>();
		TreeSet<String> singleBGP = new TreeSet<String>();

		QueryIterator queryIter = new QueryIterator();
		if (args.length > 0) {
			queryIter = new QueryFolderIterator(args[0]);
		}

		int QueryParseExceptionCount = 0;
		int QueryExceptionCount = 0;
		for (String query : queryIter) {
			Op op = null;
			try {
				op = Algebra.compile(QueryFactory.create(query));
			} catch (QueryParseException e) {
				QueryParseExceptionCount++;
				continue;
			} catch (QueryException e) {
				QueryExceptionCount++;
				continue;
			}

			ExtractBGPsVisitor visitor = new ExtractBGPsVisitor();
			op.visit(visitor);

			if (visitor.mainBGP.size() > 0
					&& !visitor.hasUnsupportedOp
					&& !visitor.hasCrossProduct()) {
				if (visitor.mainBGP.size() == 1) {
					singleBGP.add(visitor.toQueryPattern());
				} else {
					multipleBGP.add(visitor.toQueryPattern());
				}
			}
		}

		System.out.println("QueryParseException count: " + QueryParseExceptionCount);
		System.out.println("QueryException count: " + QueryExceptionCount);

		// Write BGPs into files
		try {
			FileWriter singleBGPFile = new FileWriter("single_bgps.txt");
			FileWriter multipleBGPFile = new FileWriter("multiple_bgps.txt");

			int singleCount = 0;
			for (String query : singleBGP) {
				singleCount++;
				singleBGPFile.write(Integer.toString(singleCount));
				singleBGPFile.write(',');
				singleBGPFile.write(query);
				singleBGPFile.write('\n');
			}

			int multipleCount = 0;
			for (String query : multipleBGP) {
				multipleCount++;
				multipleBGPFile.write(Integer.toString(multipleCount));
				multipleBGPFile.write(',');
				multipleBGPFile.write(query);
				multipleBGPFile.write('\n');
			}

			singleBGPFile.close();
			multipleBGPFile.close();
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}
}
