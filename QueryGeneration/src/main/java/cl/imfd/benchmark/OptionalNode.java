package cl.imfd.benchmark;

import java.util.ArrayList;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

public class OptionalNode {
	public ArrayList<Triple> bgp = new ArrayList<Triple>();

	public ArrayList<OptionalNode> optionalChildren = new ArrayList<OptionalNode>();

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (Triple triple : bgp) {
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
			sb.append(' ');
			if (p.isURI()) {
				sb.append('<');
				sb.append(p);
				sb.append('>');
			} else {
				sb.append(p);
			}
			sb.append(' ');

			if (o.isURI()) {
				sb.append('<');
				sb.append(o);
				sb.append('>');
			} else {
				sb.append(o);
			}

			sb.append(" . ");
		}

		for (OptionalNode child : optionalChildren) {
			sb.append("OPTIONAL { ");
			sb.append(child.toString());
			sb.append("} ");
		}

		return sb.toString();
	}

}
