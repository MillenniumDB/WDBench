package cl.imfd.benchmark;

import java.util.ArrayList;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpPath;


public class ExtractBGPsVisitor extends VisitorBase {
	public ArrayList<Triple> mainBGP = new ArrayList<Triple>();

	@Override
	public void visit(OpBGP opBGP) {
		for (Triple triple : opBGP.getPattern().getList()) {
			Node s = triple.getSubject();
			Node p = triple.getPredicate();
			Node o = triple.getObject();

			// Ignore queries having variables introduced by [] or path transformations
			if (s.isVariable() && s.toString().charAt(0) == '?' && s.toString().charAt(1) == '?') hasUnsuportedOp = true;
			if (p.isVariable() && p.toString().charAt(0) == '?' && p.toString().charAt(1) == '?') hasUnsuportedOp = true;
			if (o.isVariable() && o.toString().charAt(0) == '?' && o.toString().charAt(1) == '?') hasUnsuportedOp = true;

			// Check s, p and o are not a label variable:
			if (s.isVariable() && s.getName().contains("Label")) hasUnsuportedOp = true;
			if (p.isVariable() && p.getName().contains("Label")) hasUnsuportedOp = true;
			if (o.isVariable() && o.getName().contains("Label")) hasUnsuportedOp = true;

			// check triple is not 3 variables
			if (s.isVariable() && p.isVariable() && o.isVariable()) hasUnsuportedOp = true;

			// Check p is a direct property
			if (p.isURI() && !p.getURI().contains("http://www.wikidata.org/prop/direct/P")) hasUnsuportedOp = true;

			// check s and o are wikidata entity or wikidata property
			if (s.isURI()) {
				if (!s.getURI().contains("http://www.wikidata.org/prop/direct/P")
					&& !s.getURI().contains("http://www.wikidata.org/entity/Q"))
				{
					hasUnsuportedOp = true;
				}
			} else if (s.isLiteral()) hasUnsuportedOp = true;

			if (o.isURI()) {
				if (!o.getURI().contains("http://www.wikidata.org/prop/direct/P")
					&& !o.getURI().contains("http://www.wikidata.org/entity/Q"))
				{
					hasUnsuportedOp = true;
				}
			} else if (o.isLiteral()) hasUnsuportedOp = true;

			mainBGP.add(triple);
		}
	}

	@Override
	public void visit(OpPath opPath) {
		hasUnsuportedOp = true;
	}

	@Override
	public void visit(OpLeftJoin opLeftJoin) {
		hasUnsuportedOp = true;
	}
}
