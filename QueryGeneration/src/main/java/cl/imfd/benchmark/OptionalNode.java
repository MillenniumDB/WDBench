package cl.imfd.benchmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;

public class OptionalNode {
	public ArrayList<Triple> bgp = new ArrayList<Triple>();

	public ArrayList<OptionalNode> optionalChildren = new ArrayList<OptionalNode>();

	public boolean crossProduct = true;

	@Override
	public String toString() {
		HashMap<String, String> variableMap = new HashMap<String, String>();

		// We use array because Java doesn't provide a simpler way of passing an int as reference
		int[] currentNewVar = new int[1];
		currentNewVar[0] = 1;

		crossProduct = false;
		return toString(variableMap, currentNewVar);
	}

	private String toString(HashMap<String, String> variableMap, int[] currentNewVar) {
		reorderAndRename(variableMap, currentNewVar);

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
			sb.append(child.toString(variableMap, currentNewVar));
			sb.append("} ");
			if (child.crossProduct) {
				crossProduct = true;
			}
		}

		return sb.toString();
	}

	public void reorderAndRename(HashMap<String, String> variableMap, int[] currentNewVar) {
		// Used to order the triples
		HashMap<Integer, String> orderMap = new HashMap<Integer, String>();

		// Here the index of the order will be placed
		ArrayList<Integer> orderList = new ArrayList<Integer>();

		for (int i = 0; i < bgp.size(); i++) {
			Node s = bgp.get(i).getSubject();
			Node p = bgp.get(i).getPredicate();
			Node o = bgp.get(i).getObject();
			StringBuilder orderSb = new StringBuilder();
			if (s.isVariable()) {
				orderSb.append("?");
			} else {
				orderSb.append(s.toString());
			}
			if (p.isVariable()) {
				orderSb.append("?");
			} else {
				orderSb.append(p.toString());
			}
			if (o.isVariable()) {
				orderSb.append("?");
			} else {
				orderSb.append(o.toString());
			}
			orderMap.put(i, orderSb.toString());
		}

		// sort indices
		for (int i = 0; i < bgp.size(); i++) {
			int min = -1;
			for (Entry<Integer, String> entry : orderMap.entrySet()) {
				if (min == -1) {
					min = entry.getKey();
				} else {
					String minStr = orderMap.get(min);

					if (entry.getValue().compareTo(minStr) < 0) {
						min = entry.getKey();
					}
				}
			}
			orderList.add(min);
			orderMap.remove(min);
		}

		ArrayList<Triple> transformedBgp = new ArrayList<Triple>();

		for (Integer i : orderList) {
			Triple triple = bgp.get(i);
			Node s = triple.getSubject();
			Node p = triple.getPredicate();
			Node o = triple.getObject();

			Node newS = s;
			Node newP = p;
			Node newO = o;

			if (s.isVariable()) {
				String varName = s.getName();
				if (variableMap.containsKey(varName)) {
					crossProduct = false;
					newS = Var.alloc(variableMap.get(varName));
				} else {
					String newVarName = "x" + currentNewVar[0];
					variableMap.put(varName, newVarName);
					newS = Var.alloc(newVarName);
					currentNewVar[0] = currentNewVar[0] + 1;
				}
			}

			if (p.isVariable()) {
				String varName = p.getName();
				if (variableMap.containsKey(varName)) {
					crossProduct = false;
					newP = Var.alloc(variableMap.get(varName));
				} else {
					String newVarName = "x" + currentNewVar[0];
					variableMap.put(varName, newVarName);
					newP = Var.alloc(newVarName);
					currentNewVar[0] = currentNewVar[0] + 1;
				}
			}

			if (o.isVariable()) {
				String varName = o.getName();
				if (variableMap.containsKey(varName)) {
					crossProduct = false;
					newO = Var.alloc(variableMap.get(varName));
				} else {
					String newVarName = "x" + currentNewVar[0];
					variableMap.put(varName, newVarName);
					newO = Var.alloc(newVarName);
					currentNewVar[0] = currentNewVar[0] + 1;
				}
			}

			transformedBgp.add(new Triple(newS, newP, newO));
		}
		bgp = transformedBgp;
	}
}
