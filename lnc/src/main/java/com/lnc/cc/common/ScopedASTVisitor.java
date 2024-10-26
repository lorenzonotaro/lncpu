package com.lnc.cc.common;

import com.lnc.cc.ast.*;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;

public abstract class ScopedASTVisitor<T> implements VoidStatementVisitor<T> {
    protected final AST ast;
    protected Scope currentScope;

    private boolean success = true;

    public ScopedASTVisitor(AST ast) {
        this.ast = ast;
    }

    public Symbol resolveSymbol(Token token) {

        if(currentScope == null){
            throw new CompileException("current scope is null", token);
        }

        Symbol symbol = currentScope.resolve(token.lexeme);

        if (symbol == null) {
            throw new CompileException("undefined symbol", token);
        }

        return symbol;
    }

    public void setCurrentScope(Scope scope){
        this.currentScope = scope;
    }

    public Scope getCurrentScope(){
        return currentScope;
    }

    public Void accept(BlockStatement blockStatement) {

        if (blockStatement.getScope() != null) {
            setCurrentScope(blockStatement.getScope());
        } else {
            blockStatement.setScope(pushLocalScope());
        }

        for(Statement statement : blockStatement.statements){
            try{
                visitStatement(statement);
            } catch (CompileException e){
                e.log();
                success = false;
            }
        }

        popLocalScope();

        return null;
    }

    public Void accept(ExpressionStatement expressionStatement) {

        try{
            expressionStatement.expression.accept(this);
        }catch(CompileException e){
            e.log();
            success = false;
        }
        return null;
    }

    public Void accept(FunctionDeclaration functionDeclaration) {

        if(functionDeclaration.parameters != null){
            for(VariableDeclaration parameter : functionDeclaration.parameters){
                try{
                    parameter.accept(this);
                } catch (CompileException e){
                    e.log();
                    success = false;
                }
            }
        }

        if(functionDeclaration.body != null){
            try{
                visitStatement(functionDeclaration.body);
            } catch (CompileException e){
                e.log();
                success = false;
            }
        }

        return null;
    }

    public Void accept(IfStatement ifStatement) {

        try{
            ifStatement.condition.accept(this);
        }catch (CompileException e){
            e.log();
            success = false;
        }

        try{
            visitStatement(ifStatement.thenStatement);
        } catch (CompileException e){
            e.log();
            success = false;
        }

        try{
            if(ifStatement.elseStatement != null){
                visitStatement(ifStatement.elseStatement);
            }
        }catch (CompileException e){
            e.log();
            success = false;
        }

        return null;
    }

    public Void accept(ReturnStatement returnStatement) {

        try{
            if(returnStatement.value != null){
                returnStatement.value.accept(this);
            }
        }catch (CompileException e){
            e.log();
            success = false;
        }

        return null;
    }

    @Override
    public Void accept(ForStatement forStatement) {
        if(forStatement.getScope() != null)
            setCurrentScope(forStatement.getScope());
        else
            forStatement.setScope(pushLocalScope());

        try{
            if(forStatement.initializer != null){
                visitStatement(forStatement.initializer);
            }
        }catch (CompileException e){
            e.log();
            success = false;
        }

        try{
            if(forStatement.condition != null){
                forStatement.condition.accept(this);
            }
        }catch (CompileException e){
            e.log();
            success = false;
        }

        try{
            if(forStatement.increment != null){
                forStatement.increment.accept(this);
            }
        }catch (CompileException e){
            e.log();
            success = false;
        }

        try{
            if(forStatement.body != null){
                visitStatement(forStatement.body);
            }
        }catch (CompileException e){
            e.log();
            success = false;
        }

        popLocalScope();

        return null;
    }

    public Void accept(VariableDeclaration variableDeclaration) {
        try{
            if (variableDeclaration.initializer != null) {
                variableDeclaration.initializer.accept(this);
            }
        }catch (CompileException e){
            e.log();
            success = false;
        }
        return null;

    }

    public Void accept(WhileStatement whileStatement) {

        try{
            whileStatement.condition.accept(this);
        }catch (CompileException e){
            e.log();
            success = false;
        }

        try{
            visitStatement(whileStatement.statement);
        }catch (CompileException e){
            e.log();
            success = false;
        }

        return null;
    }

    protected void define(Symbol symbol) {
        currentScope.define(symbol);
    }

    public Scope pushLocalScope() {
        return currentScope = new Scope(currentScope);
    }

    public void popLocalScope() {
        currentScope = currentScope.getParent();
    }

    protected boolean success() {
        return success;
    }

    public boolean visit() {

        for (Declaration declaration : ast.getDeclarations()) {
            visitStatement(declaration);
        }

        return success();
    }
}
