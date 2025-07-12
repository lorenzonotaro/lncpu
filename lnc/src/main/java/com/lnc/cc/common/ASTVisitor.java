package com.lnc.cc.common;

import com.lnc.cc.ast.*;

public abstract class ASTVisitor<E> implements IASTVisitor<Void, E> {

    private final AST ast;
    private boolean success = true;

    public ASTVisitor(AST ast) {
        this.ast = ast;
    }

    @Override
    public Void visit(BlockStatement blockStatement){
        for (Statement statement : blockStatement.statements) {
            visitStatement(statement);
        }
        return null;
    }

    @Override
    public Void visit(ExpressionStatement expressionStatement){
        expressionStatement.expression.accept(this);
        return null;
    }

    @Override
    public Void visit(ForStatement forStatement){
        visitStatement(forStatement.initializer);
        forStatement.condition.accept(this);

        visitStatement(forStatement.body);

        forStatement.increment.accept(this);
        return null;
    }

    @Override
    public Void visit(FunctionDeclaration functionDeclaration){
        visitStatement(functionDeclaration.body);
        return null;
    }

    @Override
    public Void visit(IfStatement ifStatement){
        ifStatement.condition.accept(this);
        visitStatement(ifStatement.thenStatement);
        if (ifStatement.elseStatement != null) {
            visitStatement(ifStatement.elseStatement);
        }
        return null;
    }

    @Override
    public Void visit(ReturnStatement returnStatement){
        returnStatement.value.accept(this);
        return null;
    }

    @Override
    public Void visit(VariableDeclaration variableDeclaration){
        if (variableDeclaration.initializer != null) {
            variableDeclaration.initializer.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(WhileStatement whileStatement){
        whileStatement.condition.accept(this);
        visitStatement(whileStatement.body);
        return null;
    }

    @Override
    public Void visit(DoWhileStatement doWhileStatement){
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
