package cl.imfd.benchmark;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.AlgebraGenerator;
import org.apache.jena.sparql.algebra.Op;
import org.junit.Test;

import cl.imfd.benchmark.visitors.ExtractC2RPQVisitor;

public class CrossProductTest {

	@Test
	public void C2RPQhasExtractCrossProductTest() {
		ExtractC2RPQVisitor visitor = new ExtractC2RPQVisitor();
		String query = "SELECT * WHERE {"
				+ "<http://www.wikidata.org/entity/Q10313> <http://www.wikidata.org/prop/direct/P625> ?x1 ."
				+ " ?x2 (<http://www.wikidata.org/prop/direct/P31>/(<http://www.wikidata.org/prop/direct/P279>)*) <http://www.wikidata.org/entity/Q1248784> . }";
		
		System.out.println(query);
		Op op = (new AlgebraGenerator()).compile(QueryFactory.create(query));
		op.visit(visitor);
		boolean hasCrossProduct = visitor.hasCrossProduct();
		assertTrue(hasCrossProduct);
	}
	
	@Test
	public void C2RPQhasNotExtractCrossProductTest() {
		ExtractC2RPQVisitor visitor = new ExtractC2RPQVisitor();
		String query = "SELECT * WHERE {"
				+ "?x1 <http://www.wikidata.org/prop/direct/P170> ?x2 . \n"
				+ "?x1 <http://www.wikidata.org/prop/direct/P276> ?x3 . \n"
				+ "?x2 <http://www.wikidata.org/prop/direct/P463> <http://www.wikidata.org/entity/Q270920> . \n"
				+ "?x1 (<http://www.wikidata.org/prop/direct/P31>/(<http://www.wikidata.org/prop/direct/P279>)*) <http://www.wikidata.org/entity/Q3305213> . \n"
				+ "?x3 (<http://www.wikidata.org/prop/direct/P131>)* <http://www.wikidata.org/entity/Q145>  .}";
		
		query = "SELECT * WHERE { ?x1 <http://www.wikidata.org/prop/direct/P17> ?x2 . \n"
				+ "?x1 <http://www.wikidata.org/prop/direct/P18> ?x3 . \n"
				+ "?x2 <http://www.wikidata.org/prop/direct/P297> ?x4 . \n"
				+ "?x1 <http://www.wikidata.org/prop/direct/P31> ?x5 . \n"
				+ "?x1 (<http://www.wikidata.org/prop/direct/P31>/(<http://www.wikidata.org/prop/direct/P279>)*) <http://www.wikidata.org/entity/Q2095> . \n"
				+ "?x1 (<http://www.wikidata.org/prop/direct/P31>/(<http://www.wikidata.org/prop/direct/P279>)*) ?x6 .}";
		
		query = "SELECT * WHERE {?x1 <http://www.wikidata.org/prop/direct/P17> ?x2 . \n"
				+ "?x1 <http://www.wikidata.org/prop/direct/P31> ?x3 . \n"
				+ "?x3 ((<http://www.wikidata.org/prop/direct/P279>|<http://www.wikidata.org/prop/direct/P131>))* <http://www.wikidata.org/entity/Q476028> . \n"
				+ "?x2 ((<http://www.wikidata.org/prop/direct/P279>|<http://www.wikidata.org/prop/direct/P131>))* <http://www.wikidata.org/entity/Q36> . }";
		
		System.out.println(query);
		Op op = (new AlgebraGenerator()).compile(QueryFactory.create(query));
		op.visit(visitor);
		boolean hasCrossProduct = visitor.hasCrossProduct();
		assertFalse(hasCrossProduct);
	}

}
