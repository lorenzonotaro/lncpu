package com.lnc.cc.common;

import com.lnc.cc.ast.*;

public abstract class ASTVisitor<E> implements IASTVisitor<Void, E> {

    private final AST ast;
    private boolean success = true;

    public ASTVisitor(AST ast) {
        this.ast = ast;
    }

    @Override
    public Void accept(BlockStatement blockStatement){
        for (Statement statement : blockStatement.statements) {
            visitStatement(statement);
        }
        return null;
    }

    @Override
    public Void accept(ExpressionStatement expressionStatement){
        expressionStatement.expression.accept(this);
        return null;
    }

    @Override
    public Void accept(ForStatement forStatement){
        visitStatement(forStatement.initializer);
        forStatement.condition.accept(this);
        forStatement.body.accept(this);
        forStatement.increment.accept(this);
        return null;
    }

    @Override
    public Void accept(FunctionDeclaration functionDeclaration){
        visitStatement(functionDeclaration.body);
        return null;
    }

    @Override
    public Void accept(IfStatement ifStatement){
        ifStatement.condition.accept(this);
        visitStatement(ifStatement.thenStatement);
        if (ifStatement.elseStatement != null) {
            visitStatement(ifStatement.elseStatement);
        }
        return null;
    }

    @Override
    public Void accept(ReturnStatement returnStatement){
        returnStatement.value.accept(this);
        return null;
    }

    @Override
    public Void accept(VariableDeclaration variableDeclaration){
        if (variableDeclaration.initializer != null) {
            variableDeclaration.initializer.accept(this);
        }
        return null;
    }

    @Override
    public Void accept(WhileStatement whileStatement){
        whileStatement.condition.accept(this);
        visitStatement(whileStatement.body);
        return null;
    }

    @Override
    public Void accept(DoWhileStatement doWhileStatement){
        doWhileStatement.condition.accept(this);
        visitStatement(doWhileStatement.body);
        return null;
    }

    public void visitStatement(Statement statement){
        statement.accept(this);
    }

    protected AST getAST() {
        return ast;
    }


    protected boolean success() {
        return success;
    }

    protected void fail(){
        success = false;
    }

    public boolean visit() {

        for (Declaration declaration : ast.getDeclarations()) {
            visitStatement(declaration);
        }

        return success();
    }
}
