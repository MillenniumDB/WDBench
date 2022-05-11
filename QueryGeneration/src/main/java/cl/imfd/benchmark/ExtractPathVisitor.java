package cl.imfd.benchmark;

import java.util.ArrayList;
import java.util.TreeSet;

import org.apache.jena.graph.Node;
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

public class ExtractPathVisitor extends VisitorBase implements PathVisitor {
	public ArrayList<String> pathsStr = new ArrayList<String>();

	private String currentString;

	public boolean invalidPath = true;

	public TreeSet<String> pathsSet = new TreeSet<String>();

	@Override
	public void visit(OpPath opPath) {
		currentString = "";
		invalidPath = false;
		opPath.getTriplePath().getPath().visit(this);
		Node s = opPath.getTriplePath().getSubject();
		Node o = opPath.getTriplePath().getObject();

		if (!invalidPath) {
			StringBuilder sb = new StringBuilder();
			int variableCount = 1;
			if (s.isVariable()) {
				String newVarName = "?x" + variableCount;
				sb.append(newVarName);

				if (!o.toString().equals(s.toString())) {
					variableCount++;
				}
			} else if (s.isURI()) {
				if (!s.getURI().contains("http://www.wikidata.org/prop/direct/P")
					&& !s.getURI().contains("http://www.wikidata.org/entity/Q"))
				{
					return;
				}
				sb.append('<');
				sb.append(s);
				sb.append('>');
			} else {
				return;
			}
			sb.append(' ');
			sb.append(currentString);
			sb.append(' ');

			if (o.isVariable()) {
				String newVarName = "?x" + variableCount;
				sb.append(newVarName);
			} else if (o.isURI()) {
				if (!o.getURI().contains("http://www.wikidata.org/prop/direct/P")
					&& !o.getURI().contains("http://www.wikidata.org/entity/Q"))
				{
					return;
				}
				sb.append('<');
				sb.append(o);
				sb.append('>');
			} else {
				return;
			}

			String str = sb.toString();
			pathsSet.add(str);
		}
	}

	@Override
	public void visit(OpBGP opBGP) {
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
			invalidPath = true;

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
