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

	public boolean hasUnsupportedOp = false;

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
		hasUnsupportedOp = true;
	}

	@Override
	public void visit(OpQuadBlock quadBlock) {
		hasUnsupportedOp = true;
	}

	@Override
	public void visit(OpTriple opTriple) {
		hasUnsupportedOp = true;
	}

	@Override
	public void visit(OpQuad opQuad) {
		hasUnsupportedOp = true;
	}

	@Override
	public void visit(OpTable opTable) {
		hasUnsupportedOp = true;
	}

	@Override
	public void visit(OpNull opNull) {
		hasUnsupportedOp = true;
	}

	@Override
	public void visit(OpProcedure opProc) {
		hasUnsupportedOp = true;
	}

	@Override
	public void visit(OpPropFunc opPropFunc) {
		hasUnsupportedOp = true;
	}

	@Override
	public void visit(OpFilter opFilter) {
		hasUnsupportedOp = true;
	}

	@Override
	public void visit(OpGraph opGraph) {
		hasUnsupportedOp = true;
	}

	@Override
	public void visit(OpDatasetNames dsNames) {
		hasUnsupportedOp = true;
	}

	@Override
	public void visit(OpLabel opLabel) {
		hasUnsupportedOp = true;
	}

	@Override
	public void visit(OpAssign opAssign) {
		hasUnsupportedOp = true;
	}

	@Override
	public void visit(OpExtend opExtend) {
		hasUnsupportedOp = true;
	}

	@Override
	public void visit(OpUnion opUnion) {
		hasUnsupportedOp = true;
	}

	@Override
	public void visit(OpDiff opDiff) {
		hasUnsupportedOp = true;
	}

	@Override
	public void visit(OpMinus opMinus) {
		hasUnsupportedOp = true;
	}

	@Override
	public void visit(OpConditional opCondition) {
		hasUnsupportedOp = true;
	}

	@Override
	public void visit(OpDisjunction opDisjunction) {
		hasUnsupportedOp = true;
	}

	@Override
	public void visit(OpList opList) {
		hasUnsupportedOp = true;
	}

}
