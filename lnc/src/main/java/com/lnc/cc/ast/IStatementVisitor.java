package com.lnc.cc.ast;

public interface IStatementVisitor<S> {


    S accept(BlockStatement blockStatement);

    S accept(ExpressionStatement expressionStatement);

    S accept(ForStatement forStatement);

    S accept(FunctionDeclaration functionDeclaration);

    S accept(IfStatement ifStatement);

    S accept(ReturnStatement returnStatement);

    S accept(VariableDeclaration variableDeclaration);

    S accept(WhileStatement whileStatement);

    S accept(DoWhileStatement doWhileStatement);

    S accept(ContinueStatement continueStatement);

    S accept(BreakStatement breakStatement);
}
