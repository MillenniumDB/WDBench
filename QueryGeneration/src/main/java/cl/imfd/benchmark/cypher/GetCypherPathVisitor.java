package cl.imfd.benchmark.cypher;

import java.util.ArrayList;

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

import cl.imfd.benchmark.VisitorBase;

public class GetCypherPathVisitor extends VisitorBase implements PathVisitor {
	public ArrayList<String> sequence = new ArrayList<String>();

	public ArrayList<Boolean> inverse = new ArrayList<Boolean>();

	public ArrayList<String> sufixes = new ArrayList<String>();

	public ArrayList<String> alternatives = new ArrayList<String>();

	public boolean insideRecursion = false;  // *,+,?

	public boolean insideAlt = false;

	private boolean currentInverse;

	private String currentSuffix = null; // null, "", "+", "?", "*"

	private Node subject;

	private Node object;

	@Override
	public void visit(OpPath opPath) {
		opPath.getTriplePath().getPath().visit(this);
		if (sequence.size() == 0) {
			sequence.add(String.join("|", alternatives));
			inverse.add(currentInverse);
			sufixes.add(currentSuffix);
		}
		subject = opPath.getTriplePath().getSubject();
		object = opPath.getTriplePath().getObject();
	}

	@Override
	public void visit(OpBGP opBGP) {	}

	@Override
	public void visit(OpLeftJoin opLeftJoin) { }

	public String getCypherPath() {
		StringBuilder sb = new StringBuilder();

		sb.append("(");
		if (subject.isVariable()) {
			sb.append(subject.getName().replace("?", ""));
		} else {
			sb.append(":Entity{id:'");
			sb.append(subject.getURI()
					.replace("http://www.wikidata.org/entity/Q", "Q")
					.replace("http://www.wikidata.org/prop/direct/P", "P"));
			sb.append("'}");
		}

		for (int i = 0; i < sequence.size(); i++) {
			if (inverse.get(i)) {
				// )<-[]-(
				sb.append(")<-[:");
				sb.append(sequence.get(i));
				sb.append("]-(");
			}
			else {
				// )-[]->(
				sb.append(")-[:");
				sb.append(sequence.get(i));
				sb.append(sufixes.get(i));
				sb.append("]->(");
			}
		}

		if (object.isVariable()) {
			sb.append(object.getName().replace("?", ""));
		} else {
			// :Entity{id:'Q37'}
			sb.append(":Entity{id:'");
			sb.append(object.getURI()
					.replace("http://www.wikidata.org/entity/Q", "Q")
					.replace("http://www.wikidata.org/prop/direct/P", "P"));
			sb.append("'}");
		}
		sb.append(")");

		return sb.toString();
	}


	/***************** Path Visitor methods ********************/
	@Override
	public void visit(P_Link pathNode) {
		Node n = pathNode.getNode();
		if (!n.isURI()) throw new UnsupportedOperationException();

		if (!n.getURI().contains("http://www.wikidata.org/prop/direct/P")) {
			hasUnsuportedOp = true;
		} else {
			String currentEdge = n.getURI().replace("http://www.wikidata.org/prop/direct/P", "P");
			alternatives.add(currentEdge);
		}

		if (currentSuffix == null) {
			currentSuffix = "";
		}
	}

	@Override
	public void visit(P_Alt pathAlt) {
		insideAlt = true;

		pathAlt.getLeft().visit(this);
		pathAlt.getRight().visit(this);
	}

	@Override
	public void visit(P_Seq pathSeq) {
		if (insideRecursion || insideAlt) {
			hasUnsuportedOp = true;
			return;
		}
		boolean originalInverse = currentInverse;

		currentSuffix = null;
		alternatives = new ArrayList<String>();
		pathSeq.getLeft().visit(this);

		if (!(pathSeq.getLeft() instanceof P_Seq)) {
			sequence.add(String.join("|", alternatives));
			inverse.add(currentInverse);
			sufixes.add(currentSuffix);
		}

		currentInverse = originalInverse;
		currentSuffix = null;
		alternatives = new ArrayList<String>();
		pathSeq.getRight().visit(this);

		if (!(pathSeq.getRight() instanceof P_Seq)) {
			sequence.add(String.join("|", alternatives));
			inverse.add(currentInverse);
			sufixes.add(currentSuffix);
		}
		currentInverse = originalInverse;

		// 139,?x1 ((<P131>)*/^<P527>) <Q47588>
		// 148,?x1 ((<P19>/(<P131>)*)/^<P527>) <Q47588>
		// 214,?x1 ((^(<P279>)*/^(<P31>)?)/<P17>) <Q183>
		// 654,?x1 (^<P171>/<P225>) ?x2
		// 655,?x1 (^<P171>/<P31>) ?x2
	}

	@Override
	public void visit(P_Inverse inversePath) {
		if (insideAlt) { // TODO: en alt se puede si todos son inverse
			hasUnsuportedOp = true;
			return;
		}
		currentInverse = !currentInverse;
		inversePath.getSubPath().visit(this);
	}

	@Override
	public void visit(P_ZeroOrOne path) {
		insideRecursion = true;
		if (currentSuffix == null) {
			currentSuffix = "*0..1";
		} else if (currentSuffix != "*0..1") {
			hasUnsuportedOp = true;
		}
		path.getSubPath().visit(this);
	}

	@Override
	public void visit(P_ZeroOrMore1 path) {
		insideRecursion = true;
		if (currentSuffix == null) {
			currentSuffix = "*0..";
		} else if (currentSuffix != "*0..") {
			hasUnsuportedOp = true;
		}
		path.getSubPath().visit(this);
	}

	@Override
	public void visit(P_OneOrMore1 path) {
		insideRecursion = true;
		if (currentSuffix == null) {
			currentSuffix = "*1..";
		} else if (currentSuffix != "*1..") {
			hasUnsuportedOp = true;
		}
		path.getSubPath().visit(this);
	}

	@Override
	public void visit(P_NegPropSet pathNotOneOf) {
		hasUnsuportedOp = true;
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
