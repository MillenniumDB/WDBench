package cl.imfd.benchmark.query_extract;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.TreeSet;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.algebra.AlgebraGenerator;
import org.apache.jena.sparql.algebra.Op;

import cl.imfd.benchmark.QueryIterator;
import cl.imfd.benchmark.QueryFolderIterator;
import cl.imfd.benchmark.visitors.ExtractC2RPQVisitor;

public class ExtractC2RPQ {

	public static void main(String[] args) throws UnsupportedEncodingException, IOException {
		TreeSet<String> pathQueries = new TreeSet<String>();

		QueryIterator queryIter = new QueryIterator();
		if (args.length > 0) {
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
			op = (new AlgebraGenerator()).compile(QueryFactory.create(query));
			ExtractC2RPQVisitor visitor = new ExtractC2RPQVisitor();
			op.visit(visitor);

			if (visitor.paths.size() == 0 || visitor.mainBGP.size() == 0 || visitor.hasUnsupportedOp) {
				continue;
			}

			StringBuilder sb = new StringBuilder();
			for (Triple triple : visitor.mainBGP) {
				Node s = triple.getSubject();
				Node p = triple.getPredicate();
				Node o = triple.getObject();

				if (s.isURI()) {
					sb.append('<');
					sb.append(s);
					sb.append('>');
				} else {
					sb.append(s);
				}

				sb.append(" ");

				if (p.isURI()) {
					sb.append('<');
					sb.append(p);
					sb.append('>');
				} else {
					sb.append(p);
				}

				sb.append(" ");

				if (o.isURI()) {
					sb.append('<');
					sb.append(o);
					sb.append('>');
				} else {
					sb.append(o);
				}

				sb.append(" . ");
			}

			for (int i = 0; i < visitor.paths.size(); i++) {
				Node s = visitor.pathsS.get(i);
				if (s.isVariable()) {
					sb.append(s.toString());
				} else {
					sb.append('<');
					sb.append(s);
					sb.append('>');
				}
				sb.append(' ');
				sb.append(visitor.paths.get(i));
				sb.append(' ');

				Node o = visitor.pathsO.get(i);
				if (o.isVariable()) {
					sb.append(o.toString());
				} else {
					sb.append('<');
					sb.append(o);
					sb.append('>');
				}
				sb.append(" . ");
			}
			if (!visitor.hasCrossProduct()) {
				pathQueries.add(sb.toString());
			}
		}

		// Write Paths into file
		try {
			FileWriter pathsFile = new FileWriter("c2rpqs.txt");

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
