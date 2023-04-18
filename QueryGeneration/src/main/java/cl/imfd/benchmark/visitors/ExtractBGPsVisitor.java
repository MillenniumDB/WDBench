package cl.imfd.benchmark.visitors;

import java.util.ArrayList;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpPath;

import cl.imfd.benchmark.CrossProductCheck;

public class ExtractBGPsVisitor extends VisitorBase {
	public ArrayList<Triple> mainBGP = new ArrayList<Triple>();

	public String toQueryPattern() {
		StringBuilder sb = new StringBuilder();
		for (Triple triple : mainBGP) {
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
		return sb.toString();
	}
	
	public boolean hasCrossProduct() {
		return CrossProductCheck.hasCrossProduct(mainBGP);
	}

	@Override
	public void visit(OpBGP opBGP) {
		for (Triple triple : opBGP.getPattern().getList()) {
			Node s = triple.getSubject();
			Node p = triple.getPredicate();
			Node o = triple.getObject();

			// Ignore queries having variables introduced by [] or path transformations
			if (s.isVariable() && s.toString().charAt(0) == '?' && s.toString().charAt(1) == '?')
				hasUnsupportedOp = true;
			if (p.isVariable() && p.toString().charAt(0) == '?' && p.toString().charAt(1) == '?')
				hasUnsupportedOp = true;
			if (o.isVariable() && o.toString().charAt(0) == '?' && o.toString().charAt(1) == '?')
				hasUnsupportedOp = true;

			// Check s, p and o are not a label variable:
			if (s.isVariable() && s.getName().contains("Label"))
				hasUnsupportedOp = true;
			if (p.isVariable() && p.getName().contains("Label"))
				hasUnsupportedOp = true;
			if (o.isVariable() && o.getName().contains("Label"))
				hasUnsupportedOp = true;

			// check triple is not 3 variables
			if (s.isVariable() && p.isVariable() && o.isVariable())
				hasUnsupportedOp = true;

			// Check p is a direct property
			if (p.isURI() && !p.getURI().contains("http://www.wikidata.org/prop/direct/P"))
				hasUnsupportedOp = true;

			// check s and o are wikidata entity or wikidata property
			if (s.isURI()) {
				if (!s.getURI().contains("http://www.wikidata.org/prop/direct/P")
						&& !s.getURI().contains("http://www.wikidata.org/entity/Q")) {
					hasUnsupportedOp = true;
				}
			} else if (s.isLiteral())
				hasUnsupportedOp = true;

			if (o.isURI()) {
				if (!o.getURI().contains("http://www.wikidata.org/prop/direct/P")
						&& !o.getURI().contains("http://www.wikidata.org/entity/Q")) {
					hasUnsupportedOp = true;
				}
			} else if (o.isLiteral())
				hasUnsupportedOp = true;

			mainBGP.add(triple);
		}
	}

	@Override
	public void visit(OpPath opPath) {
		hasUnsupportedOp = true;
	}

	@Override
	public void visit(OpLeftJoin opLeftJoin) {
		hasUnsupportedOp = true;
	}
}
