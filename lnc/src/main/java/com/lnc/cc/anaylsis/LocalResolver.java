package com.lnc.cc.anaylsis;

import com.lnc.cc.ast.*;
import com.lnc.cc.common.ScopedASTVisitor;
import com.lnc.cc.common.BaseSymbol;
import com.lnc.cc.types.FunctionType;

public class LocalResolver extends ScopedASTVisitor<Void> {

    public LocalResolver(AST ast){
        super(ast);
    }


    @Override
    public Void visit(AssignmentExpression assignmentExpression) {

        assignmentExpression.left.accept(this);
        assignmentExpression.right.accept(this);

        return null;
    }

    @Override
    public Void visit(BinaryExpression binaryExpression) {

        binaryExpression.left.accept(this);
        binaryExpression.right.accept(this);

        return null;
    }

    @Override
    public Void visit(CallExpression callExpression) {

        callExpression.callee.accept(this);

        for (Expression argument : callExpression.arguments) {
            argument.accept(this);
        }

        return null;
    }

    @Override
    public Void visit(IdentifierExpression identifierExpression) {

        resolveSymbol(identifierExpression.token);

        return null;
    }

    @Override
    public Void visit(MemberAccessExpression memberAccessExpression) {
        memberAccessExpression.left.accept(this);

        return null;
    }

    @Override
    public Void visit(NumericalExpression numericalExpression) {
        return null;
    }

    @Override
    public Void visit(StringExpression stringExpression) {
        return null;
    }

    @Override
    public Void visit(SubscriptExpression subscriptExpression) {

        subscriptExpression.left.accept(this);
        subscriptExpression.index.accept(this);

        return null;
    }

    @Override
    public Void visit(UnaryExpression unaryExpression) {

        unaryExpression.operand.accept(this);

        return null;
    }

    @Override
    public void visitStatement(Statement statement) {

        if(statement instanceof FunctionDeclaration functionDeclaration){
            define(new BaseSymbol(functionDeclaration.name, FunctionType.of(functionDeclaration), functionDeclaration.isForwardDeclaration()), false);
        }

        super.visitStatement(statement);
    }

    @Override
    public Void visit(VariableDeclaration variableDeclaration) {

        BaseSymbol symbol = new BaseSymbol(variableDeclaration.name, variableDeclaration.declarator.typeSpecifier(), variableDeclaration.declarator.typeQualifier().isExtern());

        define(symbol, variableDeclaration.isParameter);

        super.visit(variableDeclaration);

        return null;
    }

    @Override
    public Void visit(ContinueStatement continueStatement) {
        return null;
    }

    @Override
    public Void visit(BreakStatement breakStatement) {
        return null;
    }

    @Override
    public Void visit(StructDeclaration structDeclaration) {
        return null;
    }
}
