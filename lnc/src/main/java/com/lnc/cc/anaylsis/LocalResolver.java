package com.lnc.cc.anaylsis;

import com.lnc.cc.ast.*;
import com.lnc.common.Logger;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;

public class LocalResolver implements IASTVisitor<Void, Void> {
    private final Scope global;
    
    private Scope currentScope;
    private boolean success;

    public LocalResolver(){
        this.global = new Scope(null);
        this.currentScope = global;
        this.success = true;
    }

    public boolean resolveLocals(Declaration[] declarations){

        for(Declaration declaration : declarations){
            declaration.accept(this);
        }

        return success;
    }

    public Symbol resolveSymbol(Token token){
        Symbol symbol = currentScope.resolve(token.lexeme);

        if(symbol == null){
            throw new CompileException("undefined symbol", token);
        }

        return symbol;
    }

    public void enterFunction(FunctionDeclaration function){
        currentScope = new Scope(function, currentScope);
    }

    public void pushLocalScope(){
        currentScope = new Scope(currentScope.getContext(), currentScope);
    }

    public void popLocalScope(){
        currentScope = currentScope.getParent();
    }


    @Override
    public Void accept(AssignmentExpression assignmentExpression) {

        assignmentExpression.left.accept(this);
        assignmentExpression.right.accept(this);

        return null;
    }

    @Override
    public Void accept(BinaryExpression binaryExpression) {

        binaryExpression.left.accept(this);
        binaryExpression.right.accept(this);

        return null;
    }

    @Override
    public Void accept(CallExpression callExpression) {

        callExpression.callee.accept(this);
        for (Expression argument : callExpression.arguments) {
            argument.accept(this);
        }

        return null;
    }

    @Override
    public Void accept(IdentifierExpression identifierExpression) {

        try {
            resolveSymbol(identifierExpression.ident);
        } catch(CompileException e){
            e.log();
            success = false;
        }
        return null;
    }

    @Override
    public Void accept(MemberAccessExpression memberAccessExpression) {
        throw new CompileException("member access not implemented", memberAccessExpression.right);
    }

    @Override
    public Void accept(NumericalExpression numericalExpression) {
        return null;
    }

    @Override
    public Void accept(StringExpression stringExpression) {
        return null;
    }

    @Override
    public Void accept(SubscriptExpression subscriptExpression) {

        subscriptExpression.left.accept(this);
        subscriptExpression.index.accept(this);


        return null;
    }

    @Override
    public Void accept(UnaryExpression unaryExpression) {

        unaryExpression.operand.accept(this);

        return null;
    }

    @Override
    public Void accept(BlockStatement blockStatement) {

        pushLocalScope();

        for(Statement statement : blockStatement.statements){
            statement.accept(this);
        }

        popLocalScope();

        return null;
    }

    @Override
    public Void accept(ExpressionStatement expressionStatement) {

        expressionStatement.expression.accept(this);

        return null;
    }

    @Override
    public Void accept(ForStatement forStatement) {

        pushLocalScope();

        forStatement.initializer.accept(this);

        forStatement.condition.accept(this);

        forStatement.increment.accept(this);

        forStatement.body.accept(this);

        popLocalScope();


        return null;
    }

    @Override
    public Void accept(FunctionDeclaration functionDeclaration) {


        currentScope.define(new Symbol(functionDeclaration.name, functionDeclaration.declarator.typeSpecifier(), true, functionDeclaration.body == null));


        if (functionDeclaration.body != null) {
            enterFunction(functionDeclaration);

            for(VariableDeclaration parameter : functionDeclaration.parameters){
                parameter.accept(this);
            }

            functionDeclaration.body.accept(this);
        }

        popLocalScope();

        return null;
    }

    @Override
    public Void accept(IfStatement ifStatement) {

        ifStatement.condition.accept(this);

        ifStatement.thenStatement.accept(this);

        if(ifStatement.elseStatement != null){
            ifStatement.elseStatement.accept(this);
        }

        return null;
    }

    @Override
    public Void accept(ReturnStatement returnStatement) {

        if(returnStatement.value != null){
            returnStatement.value.accept(this);
        }

        return null;
    }

    @Override
    public Void accept(VariableDeclaration variableDeclaration) {

        Symbol symbol = new Symbol(variableDeclaration.name, variableDeclaration.declarator.typeSpecifier(), false, variableDeclaration.declarator.typeQualifier().isExtern());

        currentScope.define(symbol);

        return null;
    }

    @Override
    public Void accept(WhileStatement whileStatement) {

        whileStatement.condition.accept(this);

        whileStatement.statement.accept(this);

        return null;
    }
}
