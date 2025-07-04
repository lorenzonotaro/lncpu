package com.lnc.cc.ast;

public interface IStatementVisitor<S> {


    S visit(BlockStatement blockStatement);

    S visit(ExpressionStatement expressionStatement);

    S visit(ForStatement forStatement);

    S visit(FunctionDeclaration functionDeclaration);

    S visit(IfStatement ifStatement);

    S visit(ReturnStatement returnStatement);

    S visit(VariableDeclaration variableDeclaration);

    S visit(WhileStatement whileStatement);

    S visit(DoWhileStatement doWhileStatement);

    S visit(ContinueStatement continueStatement);

    S visit(BreakStatement breakStatement);

    S visit(StructDeclaration structDeclaration);
}
