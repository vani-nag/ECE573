package assembly;

import java.util.List;

import compiler.Scope.SymbolTableEntry;
import ast.visitor.AbstractASTVisitor;

import ast.*;
import assembly.instructions.*;
import compiler.Scope;
import compiler.LocalScope;

public class CodeGenerator extends AbstractASTVisitor<CodeObject> {

	int intRegCount;
	int floatRegCount;
	static final public char intTempPrefix = 't';
	static final public char floatTempPrefix = 'f';
	
	int loopLabel;
	int elseLabel;
	int outLabel;

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
	 * Important: add a pointer from the code object to the symbol table entry
	 *            so we know how to generate code for it later (we'll need to find
	 *            the address)
	 * 
	 * Mark the code object as holding a variable, and also as an lval
	 */
	@Override
	protected CodeObject postprocess(VarNode node) {
		
		Scope.SymbolTableEntry sym = node.getSymbol();
		
		CodeObject co = new CodeObject(sym);
		co.lval = true;
		co.type = node.getType();

		return co;
	}

	/** Generate code for IntLiterals
	 * 
	 * Use load immediate instruction to do this.
	 */
	@Override
	protected CodeObject postprocess(IntLitNode node) {
		CodeObject co = new CodeObject();
		
		//Load an immediate into a register
		//The li and la instructions are the same, but it's helpful to distinguish
		//for readability purposes.
		//li tmp' value
		Instruction i = new Li(generateTemp(Scope.Type.INT), node.getVal());

		co.code.add(i); //add this instruction to the code object
		co.lval = false; //co holds an rval -- data
		co.temp = i.getDest(); //temp is in destination of li
		co.type = node.getType();

		return co;
	}

	/** Generate code for FloatLiteras
	 * 
	 * Use load immediate instruction to do this.
	 */
	@Override
	protected CodeObject postprocess(FloatLitNode node) {
		CodeObject co = new CodeObject();
		
		//Load an immediate into a regisster
		//The li and la instructions are the same, but it's helpful to distinguish
		//for readability purposes.
		//li tmp' value
		Instruction i = new FImm(generateTemp(Scope.Type.FLOAT), node.getVal());

		co.code.add(i); //add this instruction to the code object
		co.lval = false; //co holds an rval -- data
		co.temp = i.getDest(); //temp is in destination of li
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
		Instruction i = null;
		//Step 1a
		if(left.lval)
			left = rvalify(left);
		//Step 1
		co.code.addAll(left.code);

		//Step 2a
		if(right.lval)
			right = rvalify(right);
		//Step 2
		co.code.addAll(right.code);


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
	 * Step 1a: if child is an lval, add a load to get the data
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
		Instruction neg = null;
		if(expr.lval == true)
			expr = rvalify(expr);
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
		Instruction load = null;
		InstructionList il = null;
		SymbolTableEntry symbol = left.getSTE();
		String address = symbol.addressToString();

		assert(left.lval == true); //left hand side had better hold an address

		//Step 1a
		if (left.isVar() && !symbol.isLocal()) {
			il = generateAddrFromVariable(left);
			co.code.addAll(il);
		}

		/* FILL IN FOR STEP 2 */
		

		//Step 2a
		if(right.lval)
			right = rvalify(right);
		//Step 2
		co.code.addAll(right.code);
		//System.out.println("Assign: left temp: "+left.temp+" Right temp: "+right.temp);
		if(!symbol.isLocal())
		{
			switch(node.getType()) 
			{
				case INT:
					load = new Sw(right.temp, il.getLast().getDest(), "0");co.type = Scope.Type.INT;break;
				case FLOAT:
					load = new Fsw(right.temp, il.getLast().getDest(), "0");co.type = Scope.Type.FLOAT;break;
			}
			
		}
		else
		{
			switch(node.getType()) 
			{
				case INT:
					load = new Sw(right.temp, "fp", address);co.type = Scope.Type.INT;break;
				case FLOAT:
					load = new Fsw(right.temp, "fp", address);co.type = Scope.Type.FLOAT;break;
			}
		}
		co.code.add(load);
		
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
				//geti tmp
				//if var is global: la tmp', <var>; sw tmp 0(tmp')
				//if var is local: sw tmp offset(fp)
				Instruction geti = new GetI(generateTemp(Scope.Type.INT));
				il.add(geti);
				InstructionList store = new InstructionList();
				if (var.getSTE().isLocal()) {
					store.add(new Sw(geti.getDest(), "fp", String.valueOf(var.getSTE().addressToString())));
				} else {
					store.addAll(generateAddrFromVariable(var));
					store.add(new Sw(geti.getDest(), store.getLast().getDest(), "0"));
				}
				il.addAll(store);
				break;
			case FLOAT:
				//Code to generate if FLOAT:
				//getf tmp
				//if var is global: la tmp', <var>; fsw tmp 0(tmp')
				//if var is local: fsw tmp offset(fp)
				Instruction getf = new GetF(generateTemp(Scope.Type.FLOAT));
				il.add(getf);
				InstructionList fstore = new InstructionList();
				if (var.getSTE().isLocal()) {
					fstore.add(new Fsw(getf.getDest(), "fp", String.valueOf(var.getSTE().addressToString())));
				} else {
					fstore.addAll(generateAddrFromVariable(var));
					fstore.add(new Fsw(getf.getDest(), fstore.getLast().getDest(), "0"));
				}
				il.addAll(fstore);
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
	 */
	@Override
	protected CodeObject postprocess(WriteNode node, CodeObject expr) {
		CodeObject co = new CodeObject();

		//generating code for write(expr)

		//for strings, we expect a variable
		if (node.getWriteExpr().getType() == Scope.Type.STRING) {
			//Step 1:
			assert(expr.getSTE() != null);
			
			System.out.println("; generating code to print " + expr.getSTE());

			//Get the address of the variable
			InstructionList addrCo = generateAddrFromVariable(expr);
			co.code.addAll(addrCo);

			//Step 2:
			Instruction write = new PutS(addrCo.getLast().getDest());
			co.code.add(write);
		} else {
			//Step 1a:
			//if expr is an lval, load from it
			if (expr.lval == true) {
				expr = rvalify(expr);
			}
			
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
		ExpressionNode en = null;
		//If left or right is an lvalue, make it rvalue
		if(left.lval)
			left = rvalify(left);
		if(right.lval)
			right = rvalify(right);
		
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
	 */
	@Override
	protected CodeObject postprocess(IfStatementNode node, CodeObject cond, CodeObject tlist, CodeObject elist) {
		//Step 0:
		CodeObject co = new CodeObject();

		/* FILL IN FROM STEP 3*/
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
					case LE: floatCmp = new Fle(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));branchCond = new Bne(floatCmp.getDest(), "x0", outLabel); break;

					case LT: floatCmp = new Flt(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));branchCond = new Bne(floatCmp.getDest(), "x0", outLabel); break;

					case GE: floatCmp = new Flt(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));branchCond = new Beq(floatCmp.getDest(), "x0", outLabel); break;

					case GT: floatCmp = new Fle(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));branchCond = new Beq(floatCmp.getDest(), "x0", outLabel); break;

					case EQ: floatCmp = new Feq(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));branchCond = new Bne(floatCmp.getDest(), "x0", outLabel); break;

					case NE: floatCmp = new Feq(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));branchCond = new Beq(floatCmp.getDest(), "x0", outLabel); break;
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
					case LE: floatCmp = new Fle(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));branchCond = new Bne(floatCmp.getDest(), "x0", elseLabel); break;

					case LT: floatCmp = new Flt(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));branchCond = new Bne(floatCmp.getDest(), "x0", elseLabel); break;

					case GE: floatCmp = new Flt(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));branchCond = new Beq(floatCmp.getDest(), "x0", elseLabel); break;

					case GT: floatCmp = new Fle(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));branchCond = new Beq(floatCmp.getDest(), "x0", elseLabel); break;

					case EQ: floatCmp = new Feq(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));branchCond = new Bne(floatCmp.getDest(), "x0", elseLabel); break;

					case NE: floatCmp = new Feq(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));branchCond = new Beq(floatCmp.getDest(), "x0", elseLabel); break;
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
				case LE: floatCmp = new Fle(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));branchCond = new Bne(floatCmp.getDest(), "x0", outLabel); break;

				case LT: floatCmp = new Flt(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));branchCond = new Bne(floatCmp.getDest(), "x0", outLabel); break;

				case GE: floatCmp = new Flt(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));branchCond = new Beq(floatCmp.getDest(), "x0", outLabel); break;

				case GT: floatCmp = new Fle(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));branchCond = new Beq(floatCmp.getDest(), "x0", outLabel); break;

				case EQ: floatCmp = new Feq(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));branchCond = new Bne(floatCmp.getDest(), "x0", outLabel); break;

				case NE: floatCmp = new Feq(cond.leftTemp, cond.rightTemp, generateTemp(Scope.Type.INT));branchCond = new Beq(floatCmp.getDest(), "x0", outLabel); break;
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

		/* FILL IN */
		Instruction store = null;
		String fnOutLabel = generateFunctionOutLabel();
		Instruction jumpStmt = null;
		
		//Step 1
		if (retExpr.lval)
			retExpr = rvalify(retExpr);
		co.code.addAll(retExpr.code);

		//Step 2
		//System.out.println("ReturnNode: retExpr's type: "+retExpr.getType());
		switch(retExpr.getType())
		{
			case INT: store = new Sw(retExpr.temp, "fp", "8");break;
			case FLOAT: store = new Fsw(retExpr.temp, "fp", "8");break;
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

		/* FILL IN */
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

		/* FILL IN */
		
		//Step 1 - push args on stack
		int i = 0;
		Instruction store = null;
		//System.out.println("CallNode");

		//Step 1a
		for(CodeObject arg : args)
		{
			if(arg.lval)
				arg = rvalify(arg);
			//System.out.println("CallNode: "+arg.temp);
			co.code.addAll(arg.code);

			//Step 1b 
			switch(arg.getType())
			{
				case INT: store = new Sw(arg.temp, "sp", "0");co.code.add(store);
					  store = new Addi("sp", "-4", "sp");co.code.add(store);break;
				case FLOAT: store = new Fsw(arg.temp, "sp", "0");co.code.add(store);
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
		store = new Sw("ra", "sp", "0");
		co.code.add(store);
		store = new Addi("sp", "-4", "sp");
		co.code.add(store);
	
		//Step 4 - jr to fn name
		store = new Jr(generateFunctionLabel(node.getFuncName()));
		co.code.add(store);

		//Step 5 - get ret addr from stack
		store = new Addi("sp", "4", "sp");
		co.code.add(store);
		store = new Lw("ra", "sp", "0");
		co.code.add(store);

		//Step 6 - get return value from stack
		store = new Addi("sp", "4", "sp");
		co.code.add(store);
		switch(node.getType())
		{
			case INT: store = new Lw(generateTemp(Scope.Type.INT), "sp", "0");break;
			case FLOAT: store = new Flw(generateTemp(Scope.Type.FLOAT), "sp", "0");break;
		}
		
		co.code.add(store);
		co.temp = store.getDest();
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

		/* FILL IN FROM STEP 2 */
		Instruction load = null;
		Instruction ilast = null;

		//Local variables-WEIRD
		SymbolTableEntry symbol;
		String address;
		String temp;
		InstructionList il = new InstructionList();
		symbol = lco.getSTE();
		address = symbol.addressToString();

		//Step 1 - Add all code from lco and check if lco is just a var
		/*if(lco.isVar())
		{

			il = generateAddrFromVariable(lco);
		}
		co.code.addAll(il);
		ilast = il.getLast();
		temp = ilast.getDest();
		*/
		//Offset the fp by address?	
		if(symbol.isLocal()) 
		{
			//System.out.println("rvalify isLocal");
			switch(lco.getType()) 
			{
				case INT:
					load = new Lw(generateTemp(Scope.Type.INT), "fp", address);break;
				case FLOAT:
					load = new Flw(generateTemp(Scope.Type.FLOAT), "fp", address);break;
			}
		}	
			
		//Step 2 - Generate load instruction based on type and add instruction to co's code
		else
		{
			//System.out.println("rvalify global");
			il = generateAddrFromVariable(lco);
			//System.out.println("rvalify: load= "+il.isEmpty());
			co.code.addAll(il);
			ilast = il.getLast();
			temp = ilast.getDest();
			switch(lco.getType()) 
			{
				case INT:
					load = new Lw(generateTemp(Scope.Type.INT), temp, "0");break;
				case FLOAT:
					load = new Flw(generateTemp(Scope.Type.FLOAT), temp, "0");break;
			}
		}
		
		//Step 3 - Update temp and lval fields of co
		//System.out.println("rvalify: load= "+load.toString());
		co.code.add(load);
		co.temp = load.getDest();
		//System.out.println("rvalify: co.temp = "+co.temp);
		co.lval = false;
		co.type = lco.getType();

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
			//System.out.println("ADDING WEIRD ADD! generateAddrFromVariable");
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
