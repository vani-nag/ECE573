package ast.visitor;

import ast.*;

public interface ASTVisitor<R> {
	
	R run(ASTNode node);
	
	R visit(VarNode node);
	R visit(IntLitNode node);
	R visit(FloatLitNode node);
	R visit(BinaryOpNode node);
	R visit(CastExprNode node);
	R visit(UnaryOpNode node);
	R visit(AssignNode node);
	R visit(StatementListNode node);
	R visit(ReadNode node);
	R visit(WriteNode node);
	R visit(IfStatementNode node);
	R visit(WhileNode node);
	R visit(ReturnNode node);
	R visit(CondNode node);
	R visit(FunctionNode node);
	R visit(FunctionListNode node);
	R visit(CallNode node);
	R visit(PtrDerefNode node);
	R visit(AddrOfNode node);
	R visit(MallocNode node);
	R visit(FreeNode node);

}
