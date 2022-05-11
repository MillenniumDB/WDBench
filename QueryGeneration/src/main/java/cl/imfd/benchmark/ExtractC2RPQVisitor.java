package cl.imfd.benchmark;

import java.util.ArrayList;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpPath;
import org.apache.jena.sparql.path.P_Alt;
import org.apache.jena.sparql.path.P_Distinct;
import org.apache.jena.sparql.path.P_FixedLength;
import org.apache.jena.sparql.path.P_Inverse;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_Mod;
import org.apache.jena.sparql.path.P_Multi;
import org.apache.jena.sparql.path.P_NegPropSet;
import org.apache.jena.sparql.path.P_OneOrMore1;
import org.apache.jena.sparql.path.P_OneOrMoreN;
import org.apache.jena.sparql.path.P_ReverseLink;
import org.apache.jena.sparql.path.P_Seq;
import org.apache.jena.sparql.path.P_Shortest;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.sparql.path.P_ZeroOrMoreN;
import org.apache.jena.sparql.path.P_ZeroOrOne;
import org.apache.jena.sparql.path.PathVisitor;

public class ExtractC2RPQVisitor extends VisitorBase implements PathVisitor {

	public ArrayList<Triple> mainBGP = new ArrayList<Triple>();

	public ArrayList<Node> pathsS = new ArrayList<Node>();
	public ArrayList<String> paths = new ArrayList<String>();
	public ArrayList<Node> pathsO = new ArrayList<Node>();

//	public boolean invalidPath = true;

	private String currentString;

	@Override
	public void visit(OpPath opPath) {
		currentString = "";
//		invalidPath = false;
		opPath.getTriplePath().getPath().visit(this);
		Node s = opPath.getTriplePath().getSubject();
		Node o = opPath.getTriplePath().getObject();

//		if (invalidPath) return;

		if (s.isURI()) {
			if (!s.getURI().contains("http://www.wikidata.org/prop/direct/P")
				&& !s.getURI().contains("http://www.wikidata.org/entity/Q"))
			{
				hasUnsuportedOp = true;
				return;
			}
		} else if (!s.isVariable()) {
			hasUnsuportedOp = true;
			return;
		}

		if (o.isURI()) {
			if (!o.getURI().contains("http://www.wikidata.org/prop/direct/P")
				&& !o.getURI().contains("http://www.wikidata.org/entity/Q"))
			{
				hasUnsuportedOp = true;
				return;
			}
		} else if (!o.isVariable()) {
			hasUnsuportedOp = true;
			return;
		}

		pathsS.add(s);
		pathsO.add(o);
		paths.add(currentString);
	}

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
	public void visit(OpLeftJoin opLeftJoin) {
		hasUnsuportedOp = true;
	}

	/***************** Path Visitor methods ********************/
	@Override
	public void visit(P_Link pathNode) {
		Node n = pathNode.getNode();
		if (!n.isURI()) throw new UnsupportedOperationException();

		if (!n.getURI().contains("http://www.wikidata.org/prop/direct/P"))
			hasUnsuportedOp = true;

		currentString = '<' + n.getURI() + '>';
	}

	@Override
	public void visit(P_Alt pathAlt) {
		pathAlt.getLeft().visit(this);
		String l = currentString;
		pathAlt.getRight().visit(this);
		String r = currentString;
		currentString = "(" + l + "|" + r + ")";
	}

	@Override
	public void visit(P_Seq pathSeq) {
		pathSeq.getLeft().visit(this);
		String l = currentString;
		pathSeq.getRight().visit(this);
		String r = currentString;
		currentString = "(" + l + "/" + r + ")";
	}

	@Override
	public void visit(P_NegPropSet pathNotOneOf) {
		if (pathNotOneOf.getNodes().size() != 1) {
			throw new UnsupportedOperationException();
		}
		pathNotOneOf.getNodes().get(0).visit(this);
		currentString = "!" + currentString;
	}

	@Override
	public void visit(P_Inverse inversePath) {
		inversePath.getSubPath().visit(this);
		currentString = "^" + currentString;
	}


	@Override
	public void visit(P_ZeroOrOne path) {
		path.getSubPath().visit(this);
		currentString = "(" + currentString + ")?";
	}

	@Override
	public void visit(P_ZeroOrMore1 path) {
		path.getSubPath().visit(this);
		currentString = "(" + currentString + ")*";
	}

	@Override
	public void visit(P_OneOrMore1 path) {
		path.getSubPath().visit(this);
		currentString = "(" + currentString + ")+";
	}


	@Override
	public void visit(P_ReverseLink pathNode) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(P_Mod pathMod) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(P_FixedLength pFixedLength) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(P_Distinct pathDistinct) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(P_Multi pathMulti) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(P_Shortest pathShortest) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(P_ZeroOrMoreN path) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visit(P_OneOrMoreN path) {
		throw new UnsupportedOperationException();
	}
}
