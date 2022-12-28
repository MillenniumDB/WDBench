package cl.imfd.benchmark;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.algebra.AlgebraGenerator;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.core.Var;

public class ExtractC2RPQ {

	public static void main(String[] args) throws UnsupportedEncodingException, IOException {
		TreeSet<String> pathQueries = new TreeSet<String>();

		QueryIterator queryIter = new QueryIterator();
		if (args.length > 0){
			queryIter = new QueryFolderIterator(args[0]);
		}
		for (String query : queryIter) {
			Op op = null;
			try {
				op = (new AlgebraGenerator()).compile(QueryFactory.create(query));
			} catch (QueryParseException e) {
				continue;
			} catch (QueryException e) {
				continue;
			}

			ExtractC2RPQVisitor visitor = new ExtractC2RPQVisitor();
			op.visit(visitor);

			if (visitor.paths.size() == 0 || visitor.mainBGP.size() == 0 || visitor.hasUnsuportedOp) {
				continue;
			}

			// Used to order the triples
			HashMap<Integer, String> orderMap = new HashMap<Integer, String>();

			// Here the index of the order will be placed
			ArrayList<Integer> orderList = new ArrayList<Integer>();

			for (int i = 0; i < visitor.mainBGP.size(); i++) {
				Node s = visitor.mainBGP.get(i).getSubject();
				Node p = visitor.mainBGP.get(i).getPredicate();
				Node o = visitor.mainBGP.get(i).getObject();
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
			for (int i = 0; i < visitor.mainBGP.size(); i++) {
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

			// rename variables
			// OldName -> NewName
			HashMap<String, String> variableMap = new HashMap<String, String>();
			ArrayList<Triple> transformedBgp = new ArrayList<Triple>();

			int currentNewVar = 1;
			for (Integer i : orderList) {
				Triple triple = visitor.mainBGP.get(i);
				Node s = triple.getSubject();
				Node p = triple.getPredicate();
				Node o = triple.getObject();

				Node newS = s;
				Node newP = p;
				Node newO = o;

				if (s.isVariable()) {
					String varName = s.getName();
					if (variableMap.containsKey(varName)) {
						newS = Var.alloc(variableMap.get(varName));
					} else {
						String newVarName = "x" + currentNewVar;
						variableMap.put(varName, newVarName);
						newS = Var.alloc(newVarName);
						currentNewVar++;
					}
				}

				if (p.isVariable()) {
					String varName = p.getName();
					if (variableMap.containsKey(varName)) {
						newP = Var.alloc(variableMap.get(varName));
					} else {
						String newVarName = "x" + currentNewVar;
						variableMap.put(varName, newVarName);
						newP = Var.alloc(newVarName);
						currentNewVar++;
					}
				}

				if (o.isVariable()) {
					String varName = o.getName();
					if (variableMap.containsKey(varName)) {
						newO = Var.alloc(variableMap.get(varName));
					} else {
						String newVarName = "x" + currentNewVar;
						variableMap.put(varName, newVarName);
						newO = Var.alloc(newVarName);
						currentNewVar++;
					}
				}

				transformedBgp.add(new Triple(newS, newP, newO));
			}

			StringBuilder sb = new StringBuilder();
			for (Triple triple : transformedBgp) {
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

			for (int i = 0; i < visitor.paths.size(); i++) {
				Node s = visitor.pathsS.get(i);
				if (s.isVariable()) {
					String varName = s.getName();
					if (variableMap.containsKey(varName)) {
						s = Var.alloc(variableMap.get(varName));
					} else {
						String newVarName = "x" + currentNewVar;
						variableMap.put(varName, newVarName);
						s = Var.alloc(newVarName);
						currentNewVar++;
					}
					sb.append(s);
				} else {
					sb.append('<');
					sb.append(s);
					sb.append('>');
				}
				sb.append(' ');
				sb.append(visitor.paths.get(i));
				sb.append(' ');

				Node o = visitor.pathsO.get(i);
				if (o.isVariable()) {
					String varName = o.getName();
					if (variableMap.containsKey(varName)) {
						o = Var.alloc(variableMap.get(varName));
					} else {
						String newVarName = "x" + currentNewVar;
						variableMap.put(varName, newVarName);
						o = Var.alloc(newVarName);
						currentNewVar++;
					}
					sb.append(o);
				} else {
					sb.append('<');
					sb.append(o);
					sb.append('>');
				}
				sb.append(" . ");
			}

			pathQueries.add(sb.toString());
		}

		// Write Paths into file
		try {
		      FileWriter pathsFile = new FileWriter("c2rpqs.txt");

		      int count = 0;
		      for (String query : pathQueries) {
		    	  count++;
		    	  pathsFile.write(Integer.toString(count));
		    	  pathsFile.write(',');
		    	  pathsFile.write(query);
		    	  pathsFile.write('\n');
		      }

		      pathsFile.close();
	    } catch (IOException e) {
	    	System.out.println("An error occurred.");
	    	e.printStackTrace();
	    }
	}
}
