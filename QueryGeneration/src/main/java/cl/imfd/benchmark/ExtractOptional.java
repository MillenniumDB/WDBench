package cl.imfd.benchmark;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.algebra.AlgebraGenerator;
import org.apache.jena.sparql.algebra.Op;

public class ExtractOptional {

	// yo lo que haria es una lista de conjuntos de variables, donde cada triple te da un conjunto de variables
	private static boolean hasCrossProduct(String query) {
		List<Set<String>> variableSetList = new ArrayList<Set<String>>();
		// Lista de conjuntos de variables
		List<String> tripleList = new ArrayList<String>(Arrays.asList(query.replaceAll("\\{", "").replaceAll("\\}", "")
				.replaceAll("OPTIONAL", "").strip().split("\\s\\.\\s")));
		for (String triplePattern : tripleList) {
			Set<String> elementSet = new HashSet<String>();
			for(String tripleElement : triplePattern.split("\\s")){
				elementSet.add(tripleElement.strip());
			}
			variableSetList.add(elementSet);
		}
		// luego con un doble for me aseguro de que cada triple tenga interseccion en algun otro triple, sino devuelvo false
		Iterator<Set<String>> variableSetListIterator = variableSetList.iterator(); 
		while(variableSetListIterator.hasNext()) {
			boolean empty = false;
			Set<String> variableList = variableSetListIterator.next();
			Iterator<Set<String>> variableSetListIteratorInner = variableSetList.iterator();
			while(variableSetListIteratorInner.hasNext()) {
				variableList.retainAll(variableSetListIteratorInner.next());
				if(variableList.isEmpty()) {
					empty = true;
				}
			}
			if(empty) {
				System.out.println(query);
				return empty;
			}
		}
		return false;
	}

	public static void main(String[] args) throws UnsupportedEncodingException, IOException {
		TreeSet<String> optQueries = new TreeSet<String>();

		QueryIterator queryIter = new QueryIterator();
		if (args.length > 0) {
			queryIter = new QueryFolderIterator(args[0]);
		}

		for (String query : queryIter) {
			Op op = null;
			ExtractOptionalVisitor visitor = new ExtractOptionalVisitor();

			try {
				op = (new AlgebraGenerator()).compile(QueryFactory.create(query));
				op.visit(visitor);
			} catch (QueryParseException e) {
				continue;
			} catch (QueryException e) {
				continue;
			}
			if (!visitor.hasUnsuportedOp && visitor.optionalNode.bgp.size() > 0
					&& visitor.optionalNode.optionalChildren.size() > 0) {
				String str = visitor.optionalNode.toString();

				visitor.optionalNode.toString(); // sets propertly visitor.optionalNode.crossProduct
				if (!visitor.optionalNode.crossProduct) {
					optQueries.add(str);
				}
			}
		}

		// Write queries into file
		try {
			FileWriter optsFile = new FileWriter("opts.txt");

			int count = 0;
			for (String query : optQueries) {
				if (!hasCrossProduct(query)) {
					count++;
					optsFile.write(Integer.toString(count));
					optsFile.write(',');
					optsFile.write(query);
					optsFile.write('\n');
				}
			}

			optsFile.close();
		} catch (IOException e) {
			System.out.println("An error occurred writing the output.");
			e.printStackTrace();
		}
	}
}
