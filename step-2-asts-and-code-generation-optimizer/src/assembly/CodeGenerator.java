package assembly;

import java.util.List;

import compiler.Scope.SymbolTableEntry;
import ast.visitor.AbstractASTVisitor;

import ast.*;
import assembly.instructions.*;
import compiler.Scope;

public class CodeGenerator extends AbstractASTVisitor<CodeObject> {

	int intRegCount;
	int floatRegCount;
	static final public char intTempPrefix = 't';
	static final public char floatTempPrefix = 'f';
	
	
	
	public CodeGenerator() {
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
		Instruction i = null;
		/* FILL IN FOR STEP 2 */

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
		}

		//Update fields of CodeObject
		co.code.add(i);
		co.lval = false;
		co.temp = i.getDest();	

		return co;
	}

	/**
	 * Generate code for unary operations.
	 * 
	 * Step 0: create new code object
	 * Step 1: add code from child expression
	 * Step 1a: if child is an lval, add a load to get the data
	 * Step 2: generate instruction to perform unary operation (don't forget to generate right type of op)
	 * 
	 * Don't forget to update the temp and lval fields of the code object!
	 * 	   Hint: where is the result stored? Is this data or an address?
	 * 
	 */
	@Override
	protected CodeObject postprocess(UnaryOpNode node, CodeObject expr) {
		
		CodeObject co = new CodeObject();
		Instruction neg = null;

		/* FILL IN FOR STEP 2 */
		//Step 1a
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
		
		
		return co;
	}

	/**
	 * Generate code for assignment statements
	 * 
	 * Step 0: create new code object
	 * Step 1a: if LHS is a variable, generate a load instruction to get the address into a register
	 *          (see generateAddressFromVariable)
	 * Step 1b: add code from LHS of assignment (make sure it results in an lval!)
	 * Step 2: add code from RHS of assignment
	 * Step 2a: if right child is an lval, add a load to get the data
	 * Step 3: generate store (don't forget to generate the right type of store)
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
		Instruction load = null;

		assert(left.lval == true); //left hand side had better hold an address

		//Step 1a
		if (left.isVar()) {
			left = generateAddrFromVariable(left);
		}

		/* FILL IN FOR STEP 2 */
		co.code.addAll(left.code);

		//Step 2a
		if(right.lval)
			right = rvalify(right);
		//Step 2
		co.code.addAll(right.code);
		switch(node.getType()) 
		{
			case INT:
				load = new Sw(right.temp, left.temp, "0");break;
			case FLOAT:
				load = new Fsw(right.temp, left.temp, "0");break;
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
		CodeObject tmp = generateAddrFromVariable(var);

		co.code.addAll(tmp.code);

		InstructionList il = new InstructionList();
		switch(node.getType()) {
			case INT: 
				//Code to generate if INT:
				//geti tmp
				//sw tmp 0(var.tmp)
				Instruction geti = new GetI(generateTemp(Scope.Type.INT));
				il.add(geti);
				Instruction sw = new Sw(geti.getDest(), tmp.temp, "0");
				il.add(sw);
				break;
			case FLOAT:
				//Code to generate if FLOAT:
				//getf tmp
				//fsw tmp 0(var.tmp)
				Instruction getf = new GetF(generateTemp(Scope.Type.FLOAT));
				il.add(getf);
				Instruction fsw = new Fsw(getf.getDest(), tmp.temp, "0");
				il.add(fsw);
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
			
			//Get the address of the variable
			CodeObject addrCo = generateAddrFromVariable(expr);
			co.code.addAll(addrCo.code);

			//Step 2:
			Instruction write = new PutS(addrCo.temp);
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
	 * Generating code for returns
	 * 
	 * For now, we don't do anything with return values, so just generate HALT
	 */
	@Override
	protected CodeObject postprocess(ReturnNode node, CodeObject retExpr) {
		CodeObject co = new CodeObject();

		//if retexpr is an lval, load from it
		if (retExpr.lval == true) {
			retExpr = rvalify(retExpr);
		}

		co.code.addAll(retExpr.code);

		//We don't support functions yet, so a return is just a halt
		Instruction h = new Halt();
		co.code.add(h);
		co.type = null; //set to null to trigger errors

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
	
	/**
	 * Take a code object that results in an lval, and create a new code
	 * object that adds a load to generate the rval.
	 * 
	 * Step 0: Create new code object
	 * 
	 * Step 1: Add all the lco code to the new code object
	 * 		   (If lco is just a variable, create a new code object that
	 *          stores the address of variable in a code object; see
	 *          generateAddrFromVariable)
	 * 
	 * Step 2: Generate a load to load from lco's temp into a new temporary
	 * 		   Hint: it'll be easiest to generate a load with no offset:
	 * 				lw newtemp 0(oldtemp)
	 *         Don't forget to generate the right kind of load based on the type
	 *         stored in the address
	 * 
	 * Don't forget to update the temp and lval fields of the code object!
	 * 		   Hint: where is the result stored? Is this data or an address?
	 * 
	 * @param lco The code object resulting in an address
	 * @return A code object with all the code of <code>lco</code> followed by a load
	 *         to generate an rval
	 */
	protected CodeObject rvalify(CodeObject lco) {

		//Step 0
		CodeObject co = new CodeObject();
		Instruction load = null;

		assert (lco.lval == true);

		/* FILL IN FOR STEP 2 */

		//Step 1 - Add all code from lco and check if lco is just a var(lco or co?) *CHECK*
		
		if(lco.isVar())
			lco = generateAddrFromVariable(lco);
		co.code.addAll(lco.code);
		
		//Step 2 - Generate load instruction based on type and add instruction to co's code
		switch(lco.getType()) 
		{
			case INT:
				load = new Lw(generateTemp(Scope.Type.INT), lco.temp, "0");break;
			case FLOAT:
				load = new Flw(generateTemp(Scope.Type.FLOAT), lco.temp, "0");break;
		}
		
		//Step 3 - Update temp and lval fields of co
		co.code.add(load);
		co.temp = load.getDest();
		co.lval = false;
		


		return co;
	}

	/**
	 * Take a code object that holds just a variable and turn it into a code object
	 * that places the address of that variable into a registers
	 * 
	 * @param lco The code object holding a variable
	 * @return A code object with the variable's address in a register
	 */
	private CodeObject generateAddrFromVariable(CodeObject lco) {

		CodeObject co = new CodeObject();

		//Step 1:
		SymbolTableEntry symbol = lco.getSTE();
		String address = String.valueOf(symbol.getAddress());

		//Step 2:
		//la tmp' addr //Register type needs to be an int
		Instruction loadAddr = new La(generateTemp(Scope.Type.INT), address);

		co.code.add(loadAddr); //add instruction to code object
		co.lval = true; //co holds an lval, because it's an address
		co.temp = loadAddr.getDest(); //temp is in destination of la
		co.ste = null; //not a variable
		co.type = symbol.getType(); //even though register type is an int, address points to Type

		return co;
	}

}
