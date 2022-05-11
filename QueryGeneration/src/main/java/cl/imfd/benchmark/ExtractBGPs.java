package cl.imfd.benchmark;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.algebra.AlgebraGenerator;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.core.Var;

public class ExtractBGPs {
	public static void main(String[] args) throws UnsupportedEncodingException, IOException {
		TreeSet<String> multipleBGP = new TreeSet<String>();
		TreeSet<String> singleBGP = new TreeSet<String>();

		QueryIterator queryIter = new QueryIterator();

		for (String query : queryIter) {
			Op op = null;
			try {
				op = (new AlgebraGenerator()).compile(QueryFactory.create(query));
			} catch (QueryParseException e) {
				continue;
			} catch (QueryException e) {
				continue;
			}

			ExtractBGPsVisitor visitor = new ExtractBGPsVisitor();
			op.visit(visitor);

			if (visitor.mainBGP.size() > 0 && !visitor.hasUnsuportedOp) {
				// order triples ignoring variable names
				// eg: ?x1 P1 ?x2 is the same as ?x2 P1 ?x1

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
				if (transformedBgp.size() == 1) {
					singleBGP.add(sb.toString());
				} else {
					multipleBGP.add(sb.toString());
				}
			}
		}

		// Write BGPs into files
		try {
		      FileWriter singleBGPFile = new FileWriter("single_bgps.txt");
		      FileWriter multipleBGPFile = new FileWriter("multiple_bgps.txt");

		      int singleCount = 0;
		      for (String query : singleBGP) {
		    	  singleCount++;
		    	  singleBGPFile.write(Integer.toString(singleCount));
		    	  singleBGPFile.write(',');
		    	  singleBGPFile.write(query);
		    	  singleBGPFile.write('\n');
		      }

		      int multipleCount = 0;
		      for (String query : multipleBGP) {
		    	  multipleCount++;
		    	  multipleBGPFile.write(Integer.toString(multipleCount));
		    	  multipleBGPFile.write(',');
		    	  multipleBGPFile.write(query);
		    	  multipleBGPFile.write('\n');
		      }

		      singleBGPFile.close();
		      multipleBGPFile.close();
	    } catch (IOException e) {
	    	System.out.println("An error occurred.");
	    	e.printStackTrace();
	    }
	}
}
