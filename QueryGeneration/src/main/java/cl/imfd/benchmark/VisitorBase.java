package cl.imfd.benchmark;

import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitor;
import org.apache.jena.sparql.algebra.op.OpAssign;
import org.apache.jena.sparql.algebra.op.OpConditional;
import org.apache.jena.sparql.algebra.op.OpDatasetNames;
import org.apache.jena.sparql.algebra.op.OpDiff;
import org.apache.jena.sparql.algebra.op.OpDisjunction;
import org.apache.jena.sparql.algebra.op.OpDistinct;
import org.apache.jena.sparql.algebra.op.OpExtend;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpGraph;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpLabel;
import org.apache.jena.sparql.algebra.op.OpList;
import org.apache.jena.sparql.algebra.op.OpMinus;
import org.apache.jena.sparql.algebra.op.OpNull;
import org.apache.jena.sparql.algebra.op.OpOrder;
import org.apache.jena.sparql.algebra.op.OpProcedure;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.algebra.op.OpPropFunc;
import org.apache.jena.sparql.algebra.op.OpQuad;
import org.apache.jena.sparql.algebra.op.OpQuadBlock;
import org.apache.jena.sparql.algebra.op.OpQuadPattern;
import org.apache.jena.sparql.algebra.op.OpReduced;
import org.apache.jena.sparql.algebra.op.OpSequence;
import org.apache.jena.sparql.algebra.op.OpService;
import org.apache.jena.sparql.algebra.op.OpSlice;
import org.apache.jena.sparql.algebra.op.OpTable;
import org.apache.jena.sparql.algebra.op.OpTopN;
import org.apache.jena.sparql.algebra.op.OpTriple;
import org.apache.jena.sparql.algebra.op.OpUnion;

public abstract class VisitorBase implements OpVisitor  {

	public boolean hasUnsuportedOp = false;

	@Override
	public void visit(OpService opService) {
		// Everything inside a service will be ignored
	}

	@Override
	public void visit(OpJoin opJoin) {
		opJoin.getLeft().visit(this);
		opJoin.getRight().visit(this);
	}

	@Override
	public void visit(OpSequence opSequence) {
		for (Op op : opSequence.getElements()) {
			op.visit(this);
		}
	}

	@Override
	public void visit(OpOrder opOrder) {
		opOrder.getSubOp().visit(this);
	}

	@Override
	public void visit(OpProject opProject) {
		opProject.getSubOp().visit(this);
	}

	@Override
	public void visit(OpReduced opReduced) {
		opReduced.getSubOp().visit(this);
	}

	@Override
	public void visit(OpDistinct opDistinct) {
		opDistinct.getSubOp().visit(this);
	}

	@Override
	public void visit(OpSlice opSlice) {
		opSlice.getSubOp().visit(this);
	}

	@Override
	public void visit(OpGroup opGroup) {
		opGroup.getSubOp().visit(this);
	}

	@Override
	public void visit(OpTopN opTop) {
		opTop.getSubOp().visit(this);
	}

	@Override
	public void visit(OpQuadPattern quadPattern) {
		hasUnsuportedOp = true;
	}

	@Override
	public void visit(OpQuadBlock quadBlock) {
		hasUnsuportedOp = true;
	}

	@Override
	public void visit(OpTriple opTriple) {
		hasUnsuportedOp = true;
	}

	@Override
	public void visit(OpQuad opQuad) {
		hasUnsuportedOp = true;
	}

	@Override
	public void visit(OpTable opTable) {
		hasUnsuportedOp = true;
	}

	@Override
	public void visit(OpNull opNull) {
		hasUnsuportedOp = true;
	}

	@Override
	public void visit(OpProcedure opProc) {
		hasUnsuportedOp = true;
	}

	@Override
	public void visit(OpPropFunc opPropFunc) {
		hasUnsuportedOp = true;
	}

	@Override
	public void visit(OpFilter opFilter) {
		hasUnsuportedOp = true;
	}

	@Override
	public void visit(OpGraph opGraph) {
		hasUnsuportedOp = true;
	}

	@Override
	public void visit(OpDatasetNames dsNames) {
		hasUnsuportedOp = true;
	}

	@Override
	public void visit(OpLabel opLabel) {
		hasUnsuportedOp = true;
	}

	@Override
	public void visit(OpAssign opAssign) {
		hasUnsuportedOp = true;
	}

	@Override
	public void visit(OpExtend opExtend) {
		hasUnsuportedOp = true;
	}

	@Override
	public void visit(OpUnion opUnion) {
		hasUnsuportedOp = true;
	}

	@Override
	public void visit(OpDiff opDiff) {
		hasUnsuportedOp = true;
	}

	@Override
	public void visit(OpMinus opMinus) {
		hasUnsuportedOp = true;
	}

	@Override
	public void visit(OpConditional opCondition) {
		hasUnsuportedOp = true;
	}

	@Override
	public void visit(OpDisjunction opDisjunction) {
		hasUnsuportedOp = true;
	}

	@Override
	public void visit(OpList opList) {
		hasUnsuportedOp = true;
	}

}
