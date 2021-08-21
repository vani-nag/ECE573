package assembly;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;

import ast.visitor.AbstractASTVisitor;

import java.util.*;
import ast.*;
import assembly.instructions.*;
import compiler.Scope;
import compiler.Scope.SymbolTableEntry;

import compiler.LocalScope;

public class CodeGenerator extends AbstractASTVisitor<CodeObject> {

	int intRegCount;
	int floatRegCount;
	static final public String intTempPrefix = "$t";
	static final public String floatTempPrefix = "$f";
	
	int loopLabel;
	int elseLabel;
	int outLabel;

	static final public int numIntRegisters = 32;
	static final public int numFloatRegisters = 32;

	String currFunc;
	
	public CodeGenerator() {
		loopLabel = 0;
		elseLabel = 0;
		outLabel = 0;
		intRegCount = 0;		
		floatRegCount = 0;
	}

	public int getIntRegCount() {
		return intRegCount;
	}

	public int getFloatRegCount() {
		return floatRegCount;
	}
	
	/**
	 * Generate code for Variables
	 * 
	 * Create a code object that just holds a variable
	 * 
	 * NOTE THAT THIS HAS CHANGED TO GENERATE 3AC INSTEAD
	 */
	@Override
	protected CodeObject postprocess(VarNode node) {
		
		Scope.SymbolTableEntry sym = node.getSymbol();
		
		CodeObject co = new CodeObject(sym);
		co.lval = true;
		co.type = node.getType();
		if (sym.isLocal()) {
			co.temp = "$l" + String.valueOf(sym.getAddress());
		} else {
			co.temp = "$g" + sym.getName();
		}


		return co;
	}

	/** Generate code for IntLiterals
	 * 
	 * NOTE THAT THIS HAS CHANGED TO GENERATE 3AC INSTEAD
	 */
	@Override
	protected CodeObject postprocess(IntLitNode node) {
		CodeObject co = new CodeObject();
		
		//Load an immediate into a register
		//The li and la instructions are the same, but it's helpful to distinguish
		//for readability purposes.
		//li tmp' value
		//Instruction i = new Li(generateTemp(Scope.Type.INT), node.getVal());

		//co.code.add(i); //add this instruction to the code object
		co.lval = false; //co holds an rval -- data
		co.temp = String.valueOf(node.getVal()); //temp is in destination of li
		co.type = node.getType();

		return co;
	}

	/** Generate code for FloatLiteras
	 * 
	 * NOTE THAT THIS HAS CHANGED TO GENERATE 3AC INSTEAD
	 */
	@Override
	protected CodeObject postprocess(FloatLitNode node) {
		CodeObject co = new CodeObject();
		
		//Load an immediate into a regisster
		//The li and la instructions are the same, but it's helpful to distinguish
		//for readability purposes.
		//li tmp' value
		//Instruction i = new FImm(generateTemp(Scope.Type.FLOAT), node.getVal());

		//co.code.add(i); //add this instruction to the code object
		co.lval = false; //co holds an rval -- data
		co.temp = String.valueOf(node.getVal());  //temp is in destination of li
		co.type = node.getType();

		return co;
	}

	/**
	 * Generate code for binary operations.
	 * 
	 * Step 0: create new code object
	 * Step 1: add code from left child
	 * Step 1a: if left child is an lval, add a load to get the data
	 * Step 2: add code from right child
	 * Step 2a: if right child is an lval, add a load to get the data
	 * Step 3: generate binary operation using temps from left and right
	 * 
	 * Don't forget to update the temp and lval fields of the code object!
	 * 	   Hint: where is the result stored? Is this data or an address?
	 * 
	 */
	@Override
	protected CodeObject postprocess(BinaryOpNode node, CodeObject left, CodeObject right) {

		CodeObject co = new CodeObject();
		
		/* FILL IN FROM STEP 2 */

		/* MODIFY THIS TO GENERATE 3AC INSTEAD */
		Instruction i = null;
		
		//Step 1
		co.code.addAll(left.code);
		
		//Step 2
		co.code.addAll(right.code);

		System.out.println("In BinaryOpNode type = "+node.getOp());
		//Step 3
		if(node.getType() == Scope.Type.INT)
		{
			switch(node.getOp())
			{
				case ADD: i = new Add(left.temp, right.temp, generateTemp(Scope.Type.INT));break;  	
				case SUB: i = new Sub(left.temp, right.temp, generateTemp(Scope.Type.INT));break;
				case MUL: i = new Mul(left.temp, right.temp, generateTemp(Scope.Type.INT));break;
				case DIV: i = new Div(left.temp, right.temp, generateTemp(Scope.Type.INT));break;
			}
			co.type = Scope.Type.INT;
		}

		if(node.getType() == Scope.Type.FLOAT)
		{
			switch(node.getOp())
			{
				case ADD: i = new FAdd(left.temp, right.temp, generateTemp(Scope.Type.FLOAT));break;	
				case SUB: i = new FSub(left.temp, right.temp, generateTemp(Scope.Type.FLOAT));break;
				case MUL: i = new FMul(left.temp, right.temp, generateTemp(Scope.Type.FLOAT));break;
				case DIV: i = new FDiv(left.temp, right.temp, generateTemp(Scope.Type.FLOAT));break;
			}
			co.type = Scope.Type.FLOAT;
		}

		//Update fields of CodeObject
		co.code.add(i);
		System.out.println("In BinaryOpNode inst = "+i);
		co.lval = false;
		co.temp = i.getDest();
		
		co.type = node.getType();

		return co;
	}

	/**
	 * Generate code for unary operations.
	 * 
	 * Step 0: create new code object
	 * Step 1: add code from child expression
	 * Step 2: generate instruction to perform unary operation
	 * 
	 * Don't forget to update the temp and lval fields of the code object!
	 * 	   Hint: where is the result stored? Is this data or an address?
	 * 
	 */
	@Override
	protected CodeObject postprocess(UnaryOpNode node, CodeObject expr) {
		
		CodeObject co = new CodeObject();

		/* FILL IN FROM STEP 2 */

		/* MODIFY THIS TO GENERATE 3AC INSTEAD */
		Instruction neg = null;

		//Step 1 
		co.code.addAll(expr.code);
		
		//Step 2 - switch node or expr?
		switch(node.getType())
		{
			case INT: neg = new Neg(expr.temp, generateTemp(Scope.Type.INT));break;
			case FLOAT: neg = new FNeg(expr.temp, generateTemp(Scope.Type.FLOAT));break;
		}
		co.code.add(neg);
		co.temp = neg.getDest();
		co.lval = false;
		co.type = node.getType();

		return co;
	}

	/**
	 * Generate code for assignment statements
	 * 
	 * Step 0: create new code object
	 * Step 1: if LHS is a variable, generate a load instruction to get the address into a register
	 * Step 1a: add code from LHS of assignment (make sure it results in an lval!)
	 * Step 2: add code from RHS of assignment
	 * Step 2a: if right child is an lval, add a load to get the data
	 * Step 3: generate store
	 * 
	 * Hint: it is going to be easiest to just generate a store with a 0 immediate
	 * offset, and the complete store address in a register:
	 * 
	 * sw rhs 0(lhs)
	 */
	@Override
	protected CodeObject postprocess(AssignNode node, CodeObject left,
			CodeObject right) {
		
		CodeObject co = new CodeObject();

		/* FILL IN FROM STEP 2 */

		/* MODIFY THIS TO GENERATE 3AC INSTEAD */
		Instruction store = null;
		co.code.addAll(right.code);
		switch(node.getType())
		{
			case INT: store = new Mv(right.temp, left.temp);break;
			case FLOAT: store = new FMv(right.temp, left.temp);break;
		}
		co.code.add(store);
		
		
		System.out.println("In AssignNode left temp = "+left.temp);
		

		
		return co;
	}

	/**
	 * Add together all the lists of instructions generated by the children
	 */
	@Override
	protected CodeObject postprocess(StatementListNode node,
			List<CodeObject> statements) {
		CodeObject co = new CodeObject();
		//add the code from each individual statement
		for (CodeObject subcode : statements) {
			co.code.addAll(subcode.code);
		}
		co.type = null; //set to null to trigger errors
		return co;
	}
	
	/**
	 * Generate code for read
	 * 
	 * Step 0: create new code object
	 * Step 1: add code from VarNode (make sure it's an lval)
	 * Step 2: generate GetI instruction, storing into temp
	 * Step 3: generate store, to store temp in variable
	 * 
	 * NOTE THAT THIS HAS CHANGED TO GENERATE 3AC INSTEAD
	 */
	@Override
	protected CodeObject postprocess(ReadNode node, CodeObject var) {
		
		//Step 0
		CodeObject co = new CodeObject();

		//Generating code for read(id)
		assert(var.getSTE() != null); //var had better be a variable

		InstructionList il = new InstructionList();
		switch(node.getType()) {
			case INT: 
				//Code to generate if INT:
				//geti var.tmp
				Instruction geti = new GetI(var.temp);
				il.add(geti);
				break;
			case FLOAT:
				//Code to generate if FLOAT:
				//getf var.tmp
				Instruction getf = new GetF(var.temp);
				il.add(getf);
				break;
			default:
				throw new Error("Shouldn't read into other variable");
		}
		
		co.code.addAll(il);

		co.lval = false; //doesn't matter
		co.temp = null; //set to null to trigger errors
		co.type = null; //set to null to trigger errors

		return co;
	}

	/**
	 * Generate code for print
	 * 
	 * Step 0: create new code object
	 * 
	 * If printing a string:
	 * Step 1: add code from expression to be printed (make sure it's an lval)
	 * Step 2: generate a PutS instruction printing the result of the expression
	 * 
	 * If printing an integer:
	 * Step 1: add code from the expression to be printed
	 * Step 1a: if it's an lval, generate a load to get the data
	 * Step 2: Generate PutI that prints the temporary holding the expression
	 * 
	 * NOTE THAT THIS HAS CHANGED TO GENERATE 3AC INSTEAD
	 */
	@Override
	protected CodeObject postprocess(WriteNode node, CodeObject expr) {
		CodeObject co = new CodeObject();

		//generating code for write(expr)

		//for strings, we expect a variable
		if (node.getWriteExpr().getType() == Scope.Type.STRING) {
			//Step 1:
			assert(expr.getSTE() != null);

			//Step 2:
			Instruction write = new PutS(expr.temp);
			co.code.add(write);
		} else {			
			//Step 1:
			co.code.addAll(expr.code);

			//Step 2:
			//if type of writenode is int, use puti, if float, use putf
			Instruction write = null;
			switch(node.getWriteExpr().getType()) {
			case STRING: throw new Error("Shouldn't have a STRING here");
			case INT: write = new PutI(expr.temp); break;
			case FLOAT: write = new PutF(expr.temp); break;
			default: throw new Error("WriteNode has a weird type");
			}

			co.code.add(write);
		}

		co.lval = false; //doesn't matter
		co.temp = null; //set to null to trigger errors
		co.type = null; //set to null to trigger errors

		return co;
	}

	/**
	 * FILL IN FROM STEP 3
	 * 
	 * Generating an instruction sequence for a conditional expression
	 * 
	 * Implement this however you like. One suggestion:
	 *
	 * Create the code for the left and right side of the conditional, but defer
	 * generating the branch until you process IfStatementNode or WhileNode (since you
	 * do not know the labels yet). Modify CodeObject so you can save the necessary
	 * information to generate the branch instruction in IfStatementNode or WhileNode
	 * 
	 * Alternate idea 1:
	 * 
	 * Don't do anything as part of CodeGenerator. Create a new visitor class
	 * that you invoke *within* your processing of IfStatementNode or WhileNode
	 * 
	 * Alternate idea 2:
	 * 
	 * Create the branch instruction in this function, then tweak it as necessary in
	 * IfStatementNode or WhileNode
	 * 
	 * Hint: you may need to preserve extra information in the returned CodeObject to
	 * make sure you know the type of branch code to generate (int vs float)
	 */
	@Override
	protected CodeObject postprocess(CondNode node, CodeObject left, CodeObject right) {
		CodeObject co = new CodeObject();

		/* FILL IN FROM STEP 3*/

		/* MODIFY THIS TO GENERATE 3AC */
		ExpressionNode en = null;
		//Add code from left and right
		co.code.addAll(left.code);
		co.code.addAll(right.code);
		
		//Store type of left and right
		en = node.getLeft();
		co.type = en.getType();
		////System.out.println("co type");
		//System.out.println(co.type);

		//Store temps of left and right for issuing branching instruction
		co.leftTemp = left.temp;
		co.rightTemp = right.temp;


		return co;
	}

	/**
	 * FILL IN FROM STEP 3
	 * 
	 * Step 0: Create code object
	 * 
	 * Step 1: generate labels
	 * 
	 * Step 2: add code from conditional expression
	 * 
	 * Step 3: create branch statement (if not created as part of step 2)
	 * 			don't forget to generate correct branch based on type
	 * 
	 * Step 4: generate code
	 * 		<cond code>
	 *		<flipped branch> elseLabel
	 *		<then code>
	 *		j outLabel
	 *		elseLabel:
	 *		<else code>
	 *		outLabel:
	 *
	 * Step 5 insert code into code object in appropriate order.
	 * 
	 */
	@Override
	protected CodeObject postprocess(IfStatementNode node, CodeObject cond, CodeObject tlist, CodeObject elist) {
		//Step 0:
		CodeObject co = new CodeObject();

		/* FILL IN FROM STEP 3*/

		/* MODIFY THIS TO GENERATE 3AC */
		Instruction branchCond = null;
		Instruction jumpStmt = null;
		Instruction elseStmt = null;
		Instruction outStmt = null;
		CondNode cnode = null;
		cnode = node.getCondExpr();
		
		//Step 1
		String elseLabel = generateElseLabel();
		String outLabel = generateOutLabel();
		
		//Step 2
		co.code.addAll(cond.code);

		//Else list empty
		if(elist.code.isEmpty())
		{
			if(cond.type == Scope.Type.INT)
			{
				switch(cnode.getReversedOp())
				{
					case LE: branchCond = new Ble(cond.leftTemp, cond.rightTemp, outLabel); break;
					case LT: branchCond = new Blt(cond.leftTemp, cond.rightTemp, outLabel); break;
					case GE: branchCond = new Bge(cond.leftTemp, cond.rightTemp, outLabel); break;
					case GT: branchCond = new Bgt(cond.leftTemp, cond.rightTemp, outLabel); break;
					case EQ: branchCond = new Beq(cond.leftTemp, cond.rightTemp, outLabel); break;
					case NE: branchCond = new Bne(cond.leftTemp, cond.rightTemp, outLabel); break;
				}
			}
			if(cond.type == Scope.Type.FLOAT)
			{
				Instruction floatCmp = null;
				switch(cnode.getReversedOp())
				{
					case LE: floatCmp = new Fle(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));
								branchCond = new Bne(floatCmp.getDest(), "x0", outLabel); break;

					case LT: floatCmp = new Flt(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));
								branchCond = new Bne(floatCmp.getDest(), "x0", outLabel); break;

					case GE: floatCmp = new Flt(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));
								branchCond = new Beq(floatCmp.getDest(), "x0", outLabel); break;

					case GT: floatCmp = new Fle(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));
								branchCond = new Beq(floatCmp.getDest(), "x0", outLabel); break;

					case EQ: floatCmp = new Feq(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));
								branchCond = new Bne(floatCmp.getDest(), "x0", outLabel); break;

					case NE: floatCmp = new Feq(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));
								branchCond = new Beq(floatCmp.getDest(), "x0", outLabel); break;
				}
				co.code.add(floatCmp);
			}
			co.code.add(branchCond);

			//Step 4&5 Arrange code
			co.code.addAll(tlist.code);
			outStmt = new Label(outLabel);
			co.code.add(outStmt);
		}
	

		//Else not empty
		else
		{
			if(cond.type == Scope.Type.INT)
			{
				switch(cnode.getReversedOp())
				{
					case LE: branchCond = new Ble(cond.leftTemp, cond.rightTemp, elseLabel); break;
					case LT: branchCond = new Blt(cond.leftTemp, cond.rightTemp, elseLabel); break;
					case GE: branchCond = new Bge(cond.leftTemp, cond.rightTemp, elseLabel); break;
					case GT: branchCond = new Bgt(cond.leftTemp, cond.rightTemp, elseLabel); break;
					case EQ: branchCond = new Beq(cond.leftTemp, cond.rightTemp, elseLabel); break;
					case NE: branchCond = new Bne(cond.leftTemp, cond.rightTemp, elseLabel); break;
				}
			}
			if(cond.type == Scope.Type.FLOAT)
			{
				Instruction floatCmp = null;
				switch(cnode.getReversedOp())
				{
					case LE: floatCmp = new Fle(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));
								branchCond = new Bne(floatCmp.getDest(), "x0", elseLabel); break;

					case LT: floatCmp = new Flt(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));
								branchCond = new Bne(floatCmp.getDest(), "x0", elseLabel); break;

					case GE: floatCmp = new Flt(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));
								branchCond = new Beq(floatCmp.getDest(), "x0", elseLabel); break;

					case GT: floatCmp = new Fle(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));
								branchCond = new Beq(floatCmp.getDest(), "x0", elseLabel); break;

					case EQ: floatCmp = new Feq(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));
								branchCond = new Bne(floatCmp.getDest(), "x0", elseLabel); break;

					case NE: floatCmp = new Feq(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));
								branchCond = new Beq(floatCmp.getDest(), "x0", elseLabel); break;
				}
				co.code.add(floatCmp);
			}
			co.code.add(branchCond);

			//Step 4&5 Arrange code
			co.code.addAll(tlist.code);
			jumpStmt = new J(outLabel);
			co.code.add(jumpStmt);
			elseStmt = new Label(elseLabel);
			co.code.add(elseStmt);
			co.code.addAll(elist.code);
			outStmt = new Label(outLabel);
			co.code.add(outStmt);
			//System.out.println("Inside IF");
		}


		return co;
	}

		/**
	 * FILL IN FROM STEP 3
	 * 
	 * Step 0: Create code object
	 * 
	 * Step 1: generate labels
	 * 
	 * Step 2: add code from conditional expression
	 * 
	 * Step 3: create branch statement (if not created as part of step 2)
	 * 			don't forget to generate correct branch based on type
	 * 
	 * Step 4: generate code
	 * 		loopLabel:
	 *		<cond code>
	 *		<flipped branch> outLabel
	 *		<body code>
	 *		j loopLabel
	 *		outLabel:
	 *
	 * Step 5 insert code into code object in appropriate order.
	 */
	@Override
	protected CodeObject postprocess(WhileNode node, CodeObject cond, CodeObject slist) {
		//Step 0:
		CodeObject co = new CodeObject();

		/* FILL IN FROM STEP 3*/

		/* MODIFY THIS TO GENERATE 3AC */
		Instruction branchCond = null;
		Instruction jumpStmt = null;
		Instruction loopStmt = null;
		Instruction outStmt = null;
		CondNode cnode = null;
		cnode = node.getCond();

		//Step 1
		String loopLabel = generateLoopLabel();
		String outLabel = generateOutLabel();
		
		//Step 2
		loopStmt = new Label(loopLabel);
		co.code.add(loopStmt);
		co.code.addAll(cond.code);
		
		//Step 3 - node or cond for type?
		if(cond.type == Scope.Type.INT)
		{
			switch(cnode.getReversedOp())
			{
				case LE: branchCond = new Ble(cond.leftTemp, cond.rightTemp, outLabel); break;
				case LT: branchCond = new Blt(cond.leftTemp, cond.rightTemp, outLabel); break;
				case GE: branchCond = new Bge(cond.leftTemp, cond.rightTemp, outLabel); break;
				case GT: branchCond = new Bgt(cond.leftTemp, cond.rightTemp, outLabel); break;
				case EQ: branchCond = new Beq(cond.leftTemp, cond.rightTemp, outLabel); break;
				case NE: branchCond = new Bne(cond.leftTemp, cond.rightTemp, outLabel); break;
			}
		}
		if(cond.type == Scope.Type.FLOAT)
		{
			Instruction floatCmp = null;
			switch(cnode.getReversedOp())
			{
				case LE: floatCmp = new Fle(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));
							branchCond = new Bne(floatCmp.getDest(), "x0", outLabel); break;

				case LT: floatCmp = new Flt(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));
							branchCond = new Bne(floatCmp.getDest(), "x0", outLabel); break;

				case GE: floatCmp = new Flt(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));
							branchCond = new Beq(floatCmp.getDest(), "x0", outLabel); break;

				case GT: floatCmp = new Fle(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));
							branchCond = new Beq(floatCmp.getDest(), "x0", outLabel); break;

				case EQ: floatCmp = new Feq(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));
							branchCond = new Bne(floatCmp.getDest(), "x0", outLabel); break;

				case NE: floatCmp = new Feq(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));
							branchCond = new Beq(floatCmp.getDest(), "x0", outLabel); break;
			}
			co.code.add(floatCmp);
		}
		co.code.add(branchCond);
		
		//Step 4&5
		co.code.addAll(slist.code);
		jumpStmt = new J(loopLabel);
		co.code.add(jumpStmt);
		outStmt = new Label(outLabel);
		co.code.add(outStmt);

		return co;
	}

	/**
	 * FILL IN FOR STEP 4
	 * 
	 * Generating code for returns
	 * 
	 * Step 0: Generate new code object
	 * 
	 * Step 1: Add retExpr code to code object (rvalify if necessary)
	 * 
	 * Step 2: Store result of retExpr in appropriate place on stack (fp + 8)
	 * 
	 * Step 3: Jump to out label (use @link{generateFunctionOutLabel()})
	 */
	@Override
	protected CodeObject postprocess(ReturnNode node, CodeObject retExpr) {
		CodeObject co = new CodeObject();

		/* FILL IN FROM STEP 4*/

		/* MODIFY THIS TO GENERATE 3AC */
		Instruction store = null;
		String fnOutLabel = generateFunctionOutLabel();
		Instruction jumpStmt = null;
		co.code.addAll(retExpr.code);

		//Step 2
		//System.out.println("ReturnNode: retExpr's type: "+retExpr.getType());
		switch(retExpr.getType())
		{
			case INT: store = new Mv(retExpr.temp, "$l8");break;
			case FLOAT: store = new FMv(retExpr.temp, "$l8");break;
		}
		
		co.code.add(store);	

		//Step 3
		jumpStmt = new J(fnOutLabel);
		co.code.add(jumpStmt);

		return co;
	}

	@Override
	protected void preprocess(FunctionNode node) {
		// Generate function label information, used for other labels inside function
		currFunc = node.getFuncName();

		//reset register counts; each function uses new registers!
		intRegCount = 0;
		floatRegCount = 0;
	}
	
	class RegAllocator
	{
		int numReg;
		InstructionList il;
		LocalScope lc;
		//Code generated in reg alloc
		InstructionList code;

		// List of Basic blocks
		List<InstructionList> basicBlocks = new LinkedList<InstructionList>();
		
		//Liveness info for a basic block
		Map<Instruction, List<String>> liveOut = new HashMap<Instruction, List<String>>();

		//List of int registers and check if dirty(1) or not(0)
		ArrayList<String> intRegList = new ArrayList<>();
		Map<Integer,Integer> dirtyIntRegList = new HashMap<Integer,Integer>();

		//List of float registers and check if dirty(1)
		ArrayList<String> floatRegList = new ArrayList<>();
		Map<Integer,Integer> dirtyFloatRegList = new HashMap<Integer,Integer>();

		//List of registers used for codegen
		ArrayList<String> usedIntRegList = new ArrayList<>();
		ArrayList<String> usedFloatRegList = new ArrayList<>();
		
		
		
		public RegAllocator(int num, InstructionList i, LocalScope l)
		{
			this.numReg = num;
			this.il = i;
			this.lc = l;
			//HERE
			for(int j = 0; j < this.numReg; j++)
			{
				this.intRegList.add("empty");
				this.floatRegList.add("empty");
			}

			//Reserved registers
			this.intRegList.set(0,"no");
			this.intRegList.set(1,"no");	
			this.intRegList.set(2,"no");
			this.intRegList.set(8,"no");
		}

		public ArrayList<String> getUsedIntRegList()
		{
			return this.usedIntRegList;
		}
		
		public ArrayList<String> getUsedFloatRegList()
		{
			return this.usedFloatRegList;
		}


		public List<InstructionList> BBSplit()
		{
			int i,j,k;
			//Create a list of InstructionLists to hold instructions in each basic block
			//List<InstructionList> bb = new LinkedList<InstructionList>();
			//ArrayList<InstructionList> bb = new ArrayList<InstructionList>();
			

			InstructionList leader = new InstructionList();
			InstructionList temp = new InstructionList();
			InstructionList workList = new InstructionList();

			
			//System.out.println("Instruction 0");
			//System.out.println(this.il.nodes.get(0));
			//System.out.println(" ");
			//Find other leaders
			i = 0;
			while(i < this.il.size()-1)
			{
				//Add first instruction as a leader
				if(i == 0)
					leader.add(this.il.nodes.get(i));
				//System.out.println(" ");
				//System.out.println("Instruction "+String.valueOf(i));
				//System.out.println(this.il.nodes.get(i));
				

				//Add target of branch 
				if(this.il.nodes.get(i).toString().indexOf(':') != -1)
				{
					if(!(leader.contains(this.il.nodes.get(i))))
						leader.add(this.il.nodes.get(i));
					//System.out.println("Added leader:::");
					//System.out.println(this.il.nodes.get(i));
					//System.out.println(" ");
				}
				
				
				
				//Add statement after branch cond
				else if(this.il.nodes.get(i).getDest() != "pop")//to identify push/pop int/float
				{
					switch(this.il.nodes.get(i).getOC())
					{
						case BLE: leader.add(this.il.nodes.get(i+1));break;
						case BLT: leader.add(this.il.nodes.get(i+1));break;
						case BGE: leader.add(this.il.nodes.get(i+1));break;
						case BGT: leader.add(this.il.nodes.get(i+1));break;
						case BEQ: leader.add(this.il.nodes.get(i+1));break;
						case BNE: leader.add(this.il.nodes.get(i+1));break;
						case J: leader.add(this.il.nodes.get(i+1));break;
					}
					
				}	

				//System.out.println("Now, leaders are:");
				//System.out.println(leader);
				i++;
			}
			/*System.out.println("leaders");
			System.out.println(leader);
			System.out.println("Leader size");
			System.out.println(leader.size());
*/
			//Create basic blocks - no of basic blocks == no of leaders?
			i = 0;
			k = 0;
			workList = leader;
			
			while(workList.size() > 0)
			{
				temp.add(workList.nodes.get(0));
				//In il, find index where this leader occurs
				i = this.il.nodes.indexOf(workList.nodes.get(0));
				workList.nodes.remove(0);
				j = i + 1;
				while(j < this.il.size() && (!leader.contains(this.il.nodes.get(j))))
				{
					//bb.get(i).add(il.nodes.get(j));
					temp.add(this.il.nodes.get(j));
					j++;
				}
				
				
				this.basicBlocks.add(temp);
				//System.out.println("BB "+String.valueOf(k)+" size  "+String.valueOf(this.basicBlocks.size()));
				//System.out.println(this.basicBlocks.get(k).toString());
				temp = new InstructionList();
				i++;
				k++;
			}
			
			//Return list of basic blocks
			return this.basicBlocks;	
		}
		

		/*public void liveness(InstructionList isl)
		{
			//Temps for instructions and live variables list
			Instruction temp = null;
			LinkedList<String> liveSet = new LinkedList<String>();
			//i iterates through instrcutions in instrcution list
			int i,j;
			i = isl.size() - 1;
			//Last instruction
			temp = isl.nodes.get(i);
			liveSet.add(all locals and globals);
			this.liveOut.put(temp, liveSet);
			i--;
			//Get op name
			Opcode oc;
			while(i >= 0 )
			{
				//Add (old temp's liveSet - dest) U src operands
				switch(temp.getOC())
				{
					//2 operand instructions
					case ADD: liveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1);liveSet.add(src2); break;
					case ADDI: liveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1);liveSet.add(src2); break;
					case SUB: liveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1);liveSet.add(src2); break;
					case MUL: liveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1);liveSet.add(src2); break;
					case DIV: liveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1);liveSet.add(src2); break;
					
					case FADDS: liveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1);liveSet.add(src2); break;
					case FSUBS: liveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1);liveSet.add(src2); break;
					case FMULS: liveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1);liveSet.add(src2); break;
					case FDIVS: liveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1);liveSet.add(src2); break;
					case FLT:
iveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1);liveSet.add(src2); break;
					case FLE:
iveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1);liveSet.add(src2); break;
					case FEQ:
iveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1);liveSet.add(src2); break;

					//1 operand
					case NEG:liveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1); break;
					case FNEGS: liveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1); break;
					case MV: liveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1); break;
					case FMVS: liveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1); break;

					//Branch
					case BLE: liveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1);liveSet.add(src2);break;
					case BLT: liveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1);liveSet.add(src2);break;
					case BGE: liveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1);liveSet.add(src2);break;
					case BGT: liveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1);liveSet.add(src2);break;
					case BEQ: liveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1);liveSet.add(src2);break;
					case BNE: liveSet.add(this.liveOut.get(temp));liveSet.remove(dest);liveSet.add(src1);liveSet.add(src2);break;

					//Push/Pop int/float
					
					
				}
				
					

				}
				//Re-assign temp
				temp = this.il.nodes.get(i);
				this.liveOut.put(temp, liveSet);
				i--;
			}
					
		}

		//Returns reg number(index in regList) where operand is now present
		public int ensure(String src, int floatOrInt)
		{
			//integer	
			if(floatOrInt == 0)
			{
				if(this.intRegList.contains(src))
					return this.intRegList.indexOf(src);
				else
				{
					allocatedIndex = allocate(src, floatOrInt);
					//Generate load for global variable. HERE. temps?
					if(src.charAt(0) == '$' && src.charAt(1) == 'g')
					{
						//HERE: get address of global var for LA - how? - src.substring(2) is the name of global var
						code.add(new La("x3", address(src.substring(2))));
						code.add(new Lw("x"+String.valueOf(allocatedIndex),"x3", "0"));
						
					}
					else if(src.charAt(0) == '$' && src.charAt(1) == 'l')
					{
						code.add(new Lw("x"+String.valueOf(allocatedIndex), "fp", src.substring(2)));
					}
					this.intRegList.add(allocatedIndex, src);
					return allocatedIndex;
				}
			}	
			
			//Float
			else 
			{
				if(this.floatRegList.contains(src))
					return this.floatRegList.indexOf(src);
				allocatedIndex = allocate(src, intOrFloat);
				else
				{
					//Generate load for global variable. HERE. temps?
					if(src.charAt(0) == '$' && src.charAt(1) == 'g')
					{
						//HERE: get address of global var for LA - how? - src.substring(2) is the name of global var
						code.add(new La("x3", address(src.substring(2))));
						code.add(new Flw("f"+String.valueOf(allocatedIndex),"x3", "0"));
						
					}
					else if(src.charAt(0) == '$' && src.charAt(1) == 'l')
					{
						co.code.add(new Flw("f"+String.valueOf(allocatedIndex), "fp", src.substring(2)));
					}
					this.floatRegList.add(allocatedIndex, src);
					return allocatedIndex;
				}
			}	
		}
		public int allocate(String src, int intOrFloat)
		{
			int ret;
			if(intOrFloat == 0)
			{
				ret = this.intRegList.indexOf("empty");
				if(ret!= -1)
				{
					this.usedIntRegList.add(ret);
					return ret;
				else
				{
					ret = 4;
					free(ret);
					this.intRegList.add(ret, src);
					this.usedIntRegList.add(ret);
					return ret;
				}

			if(intOrFloat == 1)
			{
				ret = floatRegList.indexOf("empty");
				if(ret!= -1)
				{
					this.usedFloatRegList.add(ret);
					return ret;
				}
				else
				{
					ret = 4;
					free(ret);
					this.floatRegList.add(ret, src);
					this.usedFloatRegList.add(ret);
					return ret;
				}
			}
				
		}

		public void free(int regNo, int intOrFloat, Instruction inst)
		{
			
			List <String> liveSet = new LinkedList<String>();
			int isDirty;
			String src;
			liveSet = this.liveOut.get(inst);
			if(intOrFloat == 0)
			{
				src = this.intRegList.get(regNo);
				if(liveSet.contains(src) && dirtyIntRegList.get(regNo) == 1)
				{
					if(src.charAt(0) == '$' && src.charAt(1) == 'g')
					{
						//get address of global var - how? - src.substring(2) is the name of global var
						SymbolTableEntry ste = this.lc.getSymbolTableEntry(src.substring(2));
						this.code.add(new La("x3", ste.addressToString());
						this.code.add(new Sw(regNo,"x3", "0"));
						
					}
					else if(src.charAt(0) == '$' && src.charAt(1) == 'l')
					{
						this.code.add(new Sw(regNo, "fp", src.substring(2)));
					}
					else if(src.charAt(0) == '$' && src.charAt(1) == 't')
					{
						//HERE
						this.lc.addSymbol(Scope.Type.INT, src);
						SymbolTableEntry ste = lc.getSymbolTableEntry(src);
						this.code.add(new Sw(regNo, "fp", ste.addressToString()));
					}
					this.intRegList.set(regNo,"empty");
				}
			}

			else
			{
				src = this.floatRegList.get(regNo);
				if(liveSet.contains(src) && dirtyFloatRegList.get(regNo) == 1)
				{
					if(src.charAt(0) == '$' && src.charAt(1) == 'g')
					{
						//get address of global var - how? - src.substring(2) is the name of global var
						SymbolTableEntry ste = this.lc.getSymbolTableEntry(src.substring(2));
						this.code.add(new La("x3", ste.addressToString());
						this.code.add(new Fsw(regNo,"f3", "0"));
						
					}
					else if(src.charAt(0) == '$' && src.charAt(1) == 'l')
					{
						this.code.add(new Fsw(regNo, "fp", src.substring(2)));
					}
					else if(src.charAt(0) == '$' && src.charAt(1) == 't')
					{
						//HERE
						this.lc.addSymbol(Scope.Type.FLOAT, src);
						SymbolTableEntry ste = lc.getSymbolTableEntry(src);
						this.code.add(new Fsw(regNo, "fp", ste.addressToString()));
					}
					this.floatRegList.set(regNo,"empty");
				}							
		
			}
		}

		public void cleanup()
		{
			int i;
			
			//HERE
			for(i = 0; i < no.of Reg; i++)
			{
				if(this.intRegList.get(i).charAt(0)== '$' && this.intRegList.get(i).charAt(1) == 'l' && 				this.dirtyIntRegList.get(i) == 1)
					this.code.add(new Sw(String.valueOf(i), "fp", this.intRegList.get(i).substring(2)));

				else if (this.intRegList.get(i).charAt(0)== '$' && this.intRegList.get(i).charAt(1) == 'g' && 					this.dirtyIntRegList.get(i) == 1)
				{
					SymbolTableEntry ste = this.lc.getSymbolTableEntry(this.intRegList.get(i).substring(2));
					this.code.add(new La("x3", ste.addressToString());
					this.code.add(new Sw(String.valueOf(i),"x3", "0"));
				}

				if(this.floatRegList.get(i).charAt(0)== '$' && this.floatRegList.get(i).charAt(1) == 'l' && 					this.dirtyFloatRegList.get(i) == 1)
					this.code.add(new Fsw(String.valueOf(i), "fp", this.floatRegList.get(i).substring(2)));

				else if (this.floatRegList.get(i).charAt(0)== '$' && this.floatRegList.get(i).charAt(1) == 'g' && 					this.dirtyFloatRegList.get(i)==1)
				{
					SymbolTableEntry ste = this.lc.getSymbolTableEntry(this.floatRegList.get(i).substring(2));
					this.code.add(new La("x3", ste.addressToString());
					this.code.add(new Fsw(String.valueOf(i),"x3", "0"));
				}
			}
		}
		//HERE ADD AN INIT METHOD
		public void registerAllocation()
		{
			String rx,ry,rz;
			for(Instruction i: this.il)
			{
				switch(i.getOC())
				{//HERE
					//2 operand instructions
					case ADD:
					case ADDI:
					case SUB: 
					case MUL:
					case DIV:
					rx = "r"+String.valueOf(ensure(i.getOperand(src1),0)); 
					ry = "r"+String.valueOf(ensure(i.getOperand(src2),0)); 
					if(this.liveOut.get(i).contains(src1))
						free(this.intRegList.indexOf(src1), 0, i);
					if(live.get(i).contains(src2))
						free(this.intRegList.indexOf(src2), 0, i);
					rz = "r"+String.valueOf(allocate(i.getOperand(dest)));
					this.code.add(new Add(rx,ry,rz));
					this.dirtyIntRegList.put(rz,1);
					
					case FADDS:
					case FSUBS: 
					case FMULS:
					case FDIVS:

					//1 operand
					case NEG:
					case FNEGS:
					case MV:
					case FMVS:
					}
				}
			
			}
			
		}*/	
		
	}

	/**
	 * FILL IN FOR STEP 4
	 * 
	 * Generate code for functions
	 * 
	 * Step 1: add the label for the beginning of the function
	 * 
	 * Step 2: manage frame  pointer
	 * 			a. Save old frame pointer
	 * 			b. Move frame pointer to point to base of activation record (current sp)
	 * 			c. Update stack pointer
	 * 
	 * Step 3: allocate new stack frame (use scope infromation from FunctionNode)
	 * 
	 * Step 4: save registers on stack (Can inspect intRegCount and floatRegCount to know what to save)
	 * 
	 * Step 5: add the code from the function body
	 * 
	 * Step 6: add post-processing code:
	 * 			a. Label for `return` statements inside function body to jump to
	 * 			b. Restore registers
	 * 			c. Deallocate stack frame (set stack pointer to frame pointer)
	 * 			d. Reset fp to old location
	 * 			e. Return from function
	 */
	@Override
	protected CodeObject postprocess(FunctionNode node, CodeObject body) {
		CodeObject co = new CodeObject();

		/* FILL IN FROM STEP 4*/

		/** ADD REGISTER ALLOCATION HERE
		 * 
		 * You may find it useful to do this in the following way:
		 * 
		 * 1. Write a register allocator class that is initialized with the number of int/fp registers to use, the code from
		 * 		`body`, and the function scope from `node` (the function scope gives you access to local/global variables)
		 * 2. Within the register allocator class, do the following
		 * 		a. Split the code in body into basic blocks
		 * 		b. (573 version) Perform liveness analysis on each basic block (assume globals and locals are live)
		 * 		b. (468 version) Assume all locals/globals/temporaries are live all the time
		 * 		c. Perform register allocation on each basic block using the algorithms presented in class, converting 3AC into 					assembly code with macro expansion
		 * 			i. Add code to track the state of the registers for each basic block (what is assigned to the register, 						whether it's dirty)
		 * 			ii. As you perform register allocation within a basic block, spill registers to memory as necessary. Use 							any heuristic you want to determine which registers to allocate and which to spill
		 * 			iii. If you need to spill a temporary to memory, you'll find it easiest to add the temporary as a new 							"local" variable to the local scope (you can just use the temporary name as the variable name); 						that will automatically allocate a spot in the activation record for it.
		 * 			iv. At the end of each basic block, save all dirty/live registers that hold globals/locals back to the 							stack
		 * 3. Once register allocation is done, track:
		 * 		a. How big the local scope is after spilling temporaries -- this affects allocating the stack frame
		 * 		b. How many total registers you used -- this affects the register save/restore code
		 * 4. Now generate code for your function as before, but using the updated information for register save/restore and frame 				allocation
		 */
		LocalScope l = node.getScope();
		System.out.println("FnNode: name= "+node.getFuncName());
		System.out.println("FnNode: vars= "+l.getSymbolTableEntry("cur"));
		System.out.println("FnNode: vars= "+l.getEntries());
		RegAllocator ralloc = new RegAllocator(9, body.code, node.getScope());
		List<InstructionList> bb;
		bb = ralloc.BBSplit();






























		//COPIED IN FROM STEP 4
		Instruction i = null;
		//Step 1 - add label
		String fnLabel = generateFunctionLabel(node.getFuncName());
		i = new Label(fnLabel);
		co.code.add(i);

		//Step 2a - save fp at slot sp 
		i = new Sw("fp", "sp", "0");
		co.code.add(i);

		//Step 2b - move fp value to new sp
		i = new Mv("sp", "fp");
		co.code.add(i);

		//Step 2c - update sp
		i = new Addi("sp", "-4", "sp");
		co.code.add(i);

		//Step 3 - new stack frame - offset sp by as many # of local variables
		LocalScope lc = node.getScope();
		String spOffset = String.valueOf(-4 * lc.getNumLocals());
		//System.out.println("FnNode: "+spOffset);
		i = new Addi("sp", spOffset, "sp");
		co.code.add(i);

		//Step 4 - save registers on stack
		int counter = 1;
		String reg;
		//System.out.println("FnNode:intRegCount = "+String.valueOf(getIntRegCount()));
		while(counter <= getIntRegCount())
		{
			reg = "t" + String.valueOf(counter);
			i = new Sw(reg,"sp","0");
			co.code.add(i);
			i = new Addi("sp", "-4", "sp");
			co.code.add(i);
			counter++;
		}
		counter = 1;
		while(counter <= getFloatRegCount())
		{
			reg = "f" + String.valueOf(counter);
			i = new Fsw(reg,"sp","0");
			co.code.add(i);
			i = new Addi("sp", "-4", "sp");
			co.code.add(i);
			counter++;
		}

		//Step 5 - add code from body
		co.code.addAll(body.code);
		System.out.println("FnNode body: "+body.code);

		//Step 6a - jump to return
		fnLabel = generateFunctionOutLabel();
		i = new Label(fnLabel);
		co.code.add(i);

		//Step 6b - pop registers in rev order
		counter = getFloatRegCount();
		while(counter >= 1)
		{
			reg = "f" + String.valueOf(counter);
			i = new Addi("sp", "4", "sp");
			co.code.add(i);
			i = new Flw(reg,"sp","0");
			co.code.add(i);
			counter--;
		}

		counter = getIntRegCount();
		while(counter >= 1)
		{
			reg = "t" + String.valueOf(counter);
			i = new Addi("sp", "4", "sp");
			co.code.add(i);
			i = new Lw(reg,"sp","0");
			co.code.add(i);
			counter--;
		}
		
		
		//Step 6c - Mv sp to fp
		i = new Mv("fp","sp");
		co.code.add(i);
		
		//Step 6d - reset fp
		i = new Lw("fp","fp","0");
		co.code.add(i);
		
		//Step 6e - add RET
		i = new Ret();
		co.code.add(i);
		return co;
	}

	/**
	 * Generate code for the list of functions. This is the "top level" code generation function
	 * 
	 * Step 1: Set fp to point to sp
	 * 
	 * Step 2: Insert a JR to main
	 * 
	 * Step 3: Insert a HALT
	 * 
	 * Step 4: Include all the code of the functions
	 */
	@Override
	protected CodeObject postprocess(FunctionListNode node, List<CodeObject> funcs) {
		CodeObject co = new CodeObject();

		co.code.add(new Mv("sp", "fp"));
		co.code.add(new Jr(generateFunctionLabel("main")));
		co.code.add(new Halt());
		co.code.add(new Blank());

		//add code for each of the functions
		for (CodeObject c : funcs) {
			co.code.addAll(c.code);
			co.code.add(new Blank());
		}

		return co;
	}

	/**
	* 
	* FILL IN FOR STEP 4
	* 
	* Generate code for a call expression
	 * 
	 * Step 1: For each argument:
	 * 
	 * 	Step 1a: insert code of argument (don't forget to rvalify!)
	 * 
	 * 	Step 1b: push result of argument onto stack 
	 * 
	 * Step 2: alloate space for return value
	 * 
	 * Step 3: push current return address onto stack
	 * 
	 * Step 4: jump to function
	 * 
	 * Step 5: pop return address back from stack
	 * 
	 * Step 6: pop return value into fresh temporary (destination of call expression)
	 * 
	 * Step 7: remove arguments from stack (move sp)
	 */
	@Override
	protected CodeObject postprocess(CallNode node, List<CodeObject> args) {
		
		//STEP 0
		CodeObject co = new CodeObject();

		/* FILL IN FROM STEP 4*/

		/* MODIFY THIS TO GENERATE 3AC */
		//Step 1 - push args on stack
		int i = 0;
		Instruction store = null;
		//System.out.println("CallNode");

		//Step 1a
		for(CodeObject arg : args)
		{
			
			//System.out.println("CallNode: "+arg.temp);
			co.code.addAll(arg.code);

			//Step 1b 
			switch(arg.getType())
			{
				case INT: store = new PushInt(arg.temp);co.code.add(store);
					  store = new Addi("sp", "-4", "sp");co.code.add(store);break;
				case FLOAT: store = new PushFloat(arg.temp);co.code.add(store);
					  store = new Addi("sp", "-4", "sp");co.code.add(store);break;
			}
			i = i - 4;
		}

		/*//Subtract sp by (num of args * -2)
		i = i * 2;
		store = new Addi("sp", String.valueOf(i), "sp");
		co.code.add(store);*/


		//Step 2 - return value
		store = new Addi("sp", "-4", "sp");
		co.code.add(store);

		//Step 3 - push return address to sp and decrement sp
		store = new PushInt("ra");
		co.code.add(store);
		store = new Addi("sp", "-4", "sp");
		co.code.add(store);
	
		//Step 4 - jr to fn name
		store = new Jr(generateFunctionLabel(node.getFuncName()));
		co.code.add(store);

		//Step 5 - get ret addr from stack
		store = new Addi("sp", "4", "sp");
		co.code.add(store);
		store = new PopInt("ra");
		co.code.add(store);

		//Step 6 - get return value from stack
		store = new Addi("sp", "4", "sp");
		co.code.add(store);

		String k = generateTemp(Scope.Type.INT);
		String kf = generateTemp(Scope.Type.FLOAT);
		switch(node.getType())
		{
			case INT: store = new PopInt(k);co.temp = k;break;
			case FLOAT: store = new PopFloat(kf);co.temp = kf;break;
		}
		
		co.code.add(store);

		//Store return value to co.temp
		
		System.out.println("Ret val: "+co.temp);

		//Step 7 - move sp to remove args
		i = -1 * i;
		//System.out.println("CallNode: i = "+String.valueOf(i));
		store = new Addi("sp", String.valueOf(i), "sp");
		co.code.add(store);

		return co;
	}	
	
	/**
	 * Generate a fresh temporary
	 * 
	 * @return new temporary register name
	 */
	protected String generateTemp(Scope.Type t) {
		switch(t) {
			case INT: return intTempPrefix + String.valueOf(++intRegCount);
			case FLOAT: return floatTempPrefix + String.valueOf(++floatRegCount);
			default: throw new Error("Generating temp for bad type");
		}
	}

	protected String generateLoopLabel() {
		return "loop_" + String.valueOf(++loopLabel);
	}

	protected String generateElseLabel() {
		return  "else_" + String.valueOf(++elseLabel);
	}

	protected String generateOutLabel() {
		return "out_" +  String.valueOf(++outLabel);
	}

	protected String generateFunctionLabel() {
		return "func_" + currFunc;
	}

	protected String generateFunctionLabel(String func) {
		return "func_" + func;
	}

	protected String generateFunctionOutLabel() {
		return "func_ret_" + currFunc;
	}
	
	/**
	 * Take a code object that results in an lval, and create a new code
	 * object that adds a load to generate the rval.
	 * 
	 * @param lco The code object resulting in an address
	 * @return A code object with all the code of <code>lco</code> followed by a load
	 *         to generate an rval
	 */
	protected CodeObject rvalify(CodeObject lco) {
		
		assert (lco.lval == true);
		CodeObject co = new CodeObject();

		/* THIS WON'T BE NECESSARY IF YOU'RE GENERATING 3AC */

		/* DON'T FORGET TO ADD CODE TO GENERATE LOADS FOR LOCAL VARIABLES */

		return co;
	}

	/**
	 * Generate an instruction sequence that holds the address of the variable in a code object
	 * 
	 * If it's a global variable, just get the address from the symbol table
	 * 
	 * If it's a local variable, compute the address relative to the frame pointer (fp)
	 * 
	 * @param lco The code object holding a variable
	 * @return a list of instructions that puts the address of the variable in a register
	 */
	private InstructionList generateAddrFromVariable(CodeObject lco) {

		InstructionList il = new InstructionList();

		//Step 1:
		SymbolTableEntry symbol = lco.getSTE();
		String address = symbol.addressToString();

		//Step 2:
		Instruction compAddr = null;
		if (symbol.isLocal()) {
			//If local, address is offset
			//need to load fp + offset
			//addi tmp' fp offset
			compAddr = new Addi("fp", address, generateTemp(Scope.Type.INT));
		} else {
			//If global, address in symbol table is the right location
			//la tmp' addr //Register type needs to be an int
			compAddr = new La(generateTemp(Scope.Type.INT), address);
		}
		il.add(compAddr); //add instruction to code object

		return il;
	}

}
