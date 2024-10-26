package com.lnc.cc.common;

import com.lnc.cc.ast.*;

public interface VoidStatementVisitor<E> extends IASTVisitor<Void, E> {
    @Override
    default Void accept(BlockStatement blockStatement){
        for (Statement statement : blockStatement.statements) {
            visitStatement(statement);
        }
        return null;
    }

    @Override
    default Void accept(ExpressionStatement expressionStatement){
        expressionStatement.expression.accept(this);
        return null;
    }

    @Override
    default Void accept(ForStatement forStatement){
        visitStatement(forStatement.initializer);
        forStatement.condition.accept(this);
        forStatement.increment.accept(this);
        forStatement.body.accept(this);
        return null;
    }

    @Override
    default Void accept(FunctionDeclaration functionDeclaration){
        visitStatement(functionDeclaration.body);
        return null;
    }

    @Override
    default Void accept(IfStatement ifStatement){
        ifStatement.condition.accept(this);
        visitStatement(ifStatement.thenStatement);
        if (ifStatement.elseStatement != null) {
            visitStatement(ifStatement.elseStatement);
        }
        return null;
    }

    @Override
    default Void accept(ReturnStatement returnStatement){
        returnStatement.value.accept(this);
        return null;
    }

    @Override
    default Void accept(VariableDeclaration variableDeclaration){
        if (variableDeclaration.initializer != null) {
            variableDeclaration.initializer.accept(this);
        }
        return null;
    }

    @Override
    default Void accept(WhileStatement whileStatement){
        whileStatement.condition.accept(this);
        visitStatement(whileStatement.statement);
        return null;
    }

    default void visitStatement(Statement statement){
        statement.accept(this);
    }
}
