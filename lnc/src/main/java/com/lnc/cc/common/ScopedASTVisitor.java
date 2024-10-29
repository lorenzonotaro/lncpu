package com.lnc.cc.common;

import com.lnc.cc.ast.*;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;

public abstract class ScopedASTVisitor<T> extends ASTVisitor<T> {
    protected Scope currentScope;
    private FunctionDeclaration currentFunction;

    public ScopedASTVisitor(AST ast) {
        super(ast);
        currentScope = ast.getGlobalScope();
    }

    public Symbol resolveSymbol(Token token) {

        if(currentScope == null){
            throw new CompileException("current scope is null", token);
        }

        Symbol symbol = currentScope.resolve(token.lexeme);

        if (symbol == null) {

            Symbol globalSymbol = getAST().getGlobalScope().resolve(token.lexeme);

            if(globalSymbol != null){
                return globalSymbol;
            }

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

        for(Statement statement : blockStatement.statements){
            try{
                visitStatement(statement);
            } catch (CompileException e){
                e.log();
                fail();
            }
        }

        return null;
    }

    public Void accept(ExpressionStatement expressionStatement) {

        try{
            expressionStatement.expression.accept(this);
        }catch(CompileException e){
            e.log();
            fail();
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
                    fail();
                }
            }
        }


        if(functionDeclaration.body != null){
            try{
                this.currentFunction = functionDeclaration;
                visitStatement(functionDeclaration.body);
            } catch (CompileException e){
                e.log();
                fail();
            } finally {
                this.currentFunction = null;
            }
        }

        return null;
    }

    public Void accept(IfStatement ifStatement) {

        try{
            ifStatement.condition.accept(this);
        }catch (CompileException e){
            e.log();
            fail();
        }

        try{
            visitStatement(ifStatement.thenStatement);
        } catch (CompileException e){
            e.log();
            fail();
        }

        try{
            if(ifStatement.elseStatement != null){
                visitStatement(ifStatement.elseStatement);
            }
        }catch (CompileException e){
            e.log();
            fail();
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
            fail();
        }

        return null;
    }

    @Override
    public Void accept(ForStatement forStatement) {

        try{
            if(forStatement.initializer != null){
                visitStatement(forStatement.initializer);
            }
        }catch (CompileException e){
            e.log();
            fail();
        }

        try{
            if(forStatement.condition != null){
                forStatement.condition.accept(this);
            }
        }catch (CompileException e){
            e.log();
            fail();
        }

        try{
            if(forStatement.increment != null){
                forStatement.increment.accept(this);
            }
        }catch (CompileException e){
            e.log();
            fail();
        }

        try{
            if(forStatement.body != null){
                visitStatement(forStatement.body);
            }
        }catch (CompileException e){
            e.log();
            fail();
        }

        return null;
    }

    public Void accept(VariableDeclaration variableDeclaration) {
        try{
            if (variableDeclaration.initializer != null) {
                variableDeclaration.initializer.accept(this);
            }
        }catch (CompileException e){
            e.log();
            fail();
        }
        return null;

    }

    public Void accept(WhileStatement whileStatement) {

        try{
            whileStatement.condition.accept(this);
        }catch (CompileException e){
            e.log();
            fail();
        }

        try{
            visitStatement(whileStatement.body);
        }catch (CompileException e){
            e.log();
            fail();
        }

        return null;
    }

    protected void define(Symbol symbol) {
        currentScope.define(symbol);
    }

    public Scope pushLocalScope() {
        return currentScope = currentScope.createChild();
    }

    public void popLocalScope() {
        currentScope = currentScope.getParent();

        if(currentScope == null){
            currentScope = getAST().getGlobalScope();
        }
    }

    @Override
    public void visitStatement(Statement statement) {

        if(statement instanceof IScopedStatement ss){
            if(ss.getScope() != null){
                setCurrentScope(ss.getScope());
            } else if (ss instanceof FunctionDeclaration fd) {
                ss.setScope(currentScope = Scope.createRoot(fd.name.lexeme));
            }else{
                ss.setScope(pushLocalScope());
            }
        }

        super.visitStatement(statement);

        if(statement instanceof IScopedStatement ss){
            popLocalScope();
        }
    }

    public FunctionDeclaration getCurrentFunction() {
        return currentFunction;
    }
}
