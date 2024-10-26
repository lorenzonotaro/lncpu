package com.lnc.cc.anaylsis;

import com.lnc.cc.ast.*;

public interface VoidStatementVisitor<E> extends IASTVisitor<Void, E> {
    @Override
    default Void accept(BlockStatement blockStatement){
        for (Statement statement : blockStatement.statements) {
            statement.accept(this);
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
        forStatement.initializer.accept(this);
        forStatement.condition.accept(this);
        forStatement.increment.accept(this);
        forStatement.body.accept(this);
        return null;
    }

    @Override
    default Void accept(FunctionDeclaration functionDeclaration){
        functionDeclaration.body.accept(this);
        return null;
    }

    @Override
    default Void accept(IfStatement ifStatement){
        ifStatement.condition.accept(this);
        ifStatement.thenStatement.accept(this);
        if (ifStatement.elseStatement != null) {
            ifStatement.elseStatement.accept(this);
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
        whileStatement.statement.accept(this);
        return null;
    }
}
