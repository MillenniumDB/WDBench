package cl.imfd.benchmark.cypher;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.algebra.AlgebraGenerator;
import org.apache.jena.sparql.algebra.Op;

public class GetCypherOptional {

	static String INPUT = "opts.txt";

	public static void main(String[] args) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(INPUT));
		FileWriter outputFile = new FileWriter("cypher_opts.txt");

		for (String line = br.readLine(); line != null; line = br.readLine()) {
			String[] cols = line.trim().split(",");

			String query = "SELECT * WHERE { " + cols[1] + " }";

			Op op = null;
			GetCypherOptionalVisitor visitor = new GetCypherOptionalVisitor();

			try {
				op = (new AlgebraGenerator()).compile(QueryFactory.create(query));
				op.visit(visitor);
			} catch (QueryParseException e) {
				continue;
			} catch (QueryException e) {
				continue;
			}

			if (!visitor.hasUnsuportedOp) {
				outputFile.write(cols[0]);
				outputFile.write(",MATCH ");
				outputFile.write(visitor.cypherPattern.substring(0, visitor.cypherPattern.length() - 1));
				outputFile.write(" RETURN *\n");
			}
		}
		outputFile.close();
		br.close();
	}
}
