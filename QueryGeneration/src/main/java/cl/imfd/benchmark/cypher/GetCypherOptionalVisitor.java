package cl.imfd.benchmark.cypher;

import java.util.ArrayList;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpPath;

import cl.imfd.benchmark.VisitorBase;

public class GetCypherOptionalVisitor extends VisitorBase {
	private ArrayList<String> optionalPattern = new ArrayList<String>();

	public String cypherPattern = "";

	@Override
	public void visit(OpPath opPath) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(OpBGP opBGP) {
		for (Triple triple : opBGP.getPattern().getList()) {
			Node s = triple.getSubject();
			Node p = triple.getPredicate();
			Node o = triple.getObject();

			StringBuilder sb = new StringBuilder();
			sb.append("(");
			if (s.isVariable()) {
				sb.append(s.getName().replace("?", ""));
			} else {
				sb.append(":Entity{id:'");
				sb.append(s.getURI()
						   .replace("http://www.wikidata.org/entity/Q", "Q")
						   .replace("http://www.wikidata.org/prop/direct/P", "P"));
				sb.append("'}");
			}
			sb.append(")-[:");

			if (p.isVariable()) {
				sb.append(p.getName().replace("?", ""));
			} else {
				sb.append(p.getURI().replace("http://www.wikidata.org/prop/direct/P", "P"));
			}
			sb.append("]->(");

			if (o.isVariable()) {
				sb.append(o.getName().replace("?", ""));
			} else {
				sb.append(":Entity{id:'");
				sb.append(o.getURI()
						   .replace("http://www.wikidata.org/entity/Q", "Q")
						   .replace("http://www.wikidata.org/prop/direct/P", "P"));
				sb.append("'}");
			}
			sb.append(")");

			optionalPattern.add(sb.toString());
		}
	}

	@Override
	public void visit(OpLeftJoin opLeftJoin) {
		optionalPattern = new ArrayList<String>();
		opLeftJoin.getLeft().visit(this);

		cypherPattern += getCypherPattern();

		cypherPattern += " OPTIONAL MATCH ";

		optionalPattern = new ArrayList<String>();
		opLeftJoin.getRight().visit(this);
		cypherPattern += getCypherPattern();

		cypherPattern += ",";
	}

	public String getCypherPattern() {
		return String.join(",", optionalPattern);
	}
}
