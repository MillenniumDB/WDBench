package cl.imfd.benchmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;

import org.apache.jena.graph.Triple;

public class CrossProductCheck {
	public static boolean hasCrossProduct(ArrayList<Triple> triples) {
		if (triples.size() <= 1) {
			return false;
		}
		HashMap<String, TreeSet<String>> graph = new HashMap<String, TreeSet<String>>();
		for (Triple triple : triples) {
			String s = triple.getSubject().toString();
			String p = triple.getPredicate().toString();
			String o = triple.getObject().toString();

			graph.put(s, new TreeSet<String>());
			if (p.charAt(0) == '?') {
				graph.put(p, new TreeSet<String>());
			}
			graph.put(o, new TreeSet<String>());
		}

		for (Triple triple : triples) {
			String s = triple.getSubject().toString();
			String p = triple.getPredicate().toString();
			String o = triple.getObject().toString();
			if (p.charAt(0) == '?') {
				graph.get(s).add(p);
				graph.get(p).add(s);
				graph.get(p).add(o);
				graph.get(o).add(p);
			}
			graph.get(s).add(o);
			graph.get(o).add(s);
		}

		TreeSet<String> visited = new TreeSet<String>();
		LinkedList<String> queue = new LinkedList<String>();
		queue.add(triples.get(0).getSubject().toString());

		while (!queue.isEmpty()) {
			String current = queue.poll();
			visited.add(current);

			for (String neighbor : graph.get(current)) {
				if (!visited.contains(neighbor)) {
					visited.add(neighbor);
					queue.add(neighbor);
				}
			}
		}
		return visited.size() != graph.size();
	}
}
