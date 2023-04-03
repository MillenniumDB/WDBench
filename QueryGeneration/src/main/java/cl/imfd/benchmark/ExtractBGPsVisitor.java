package cl.imfd.benchmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpPath;


public class ExtractBGPsVisitor extends VisitorBase {
	public ArrayList<Triple> mainBGP = new ArrayList<Triple>();
	
	public boolean hasCrossProduct() {
		if (mainBGP.size() <= 1) {
			return false;
		}
		HashMap<String, TreeSet<String>> graph = new HashMap<String, TreeSet<String>>();
		for (Triple triple : mainBGP) {
			String s = triple.getSubject().toString();
			String p = triple.getPredicate().toString();
			String o = triple.getObject().toString();
			
			graph.put(s, new TreeSet<String>());
			if (p.charAt(0) == '?') {
				graph.put(p, new TreeSet<String>());
			}
			graph.put(o, new TreeSet<String>());
		}
		
		for (Triple triple : mainBGP) {
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
		queue.add(mainBGP.get(0).getSubject().toString());
		
		while (!queue.isEmpty()) {
			String current = queue.poll();
			visited.add(current);
			
			for (String neighbour : graph.get(current)) {
				System.out.println(current + "->" + neighbour);
				if (!visited.contains(neighbour)) {
					visited.add(neighbour);
					queue.add(neighbour);
				}
			}
		}
		return visited.size() != graph.size();
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
	public void visit(OpPath opPath) {
		hasUnsuportedOp = true;
	}

	@Override
	public void visit(OpLeftJoin opLeftJoin) {
		hasUnsuportedOp = true;
	}
}
