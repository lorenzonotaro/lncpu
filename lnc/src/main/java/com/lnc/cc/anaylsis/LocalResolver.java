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

        resolveSymbol(identifierExpression.token);

        return null;
    }

    @Override
    public Void accept(MemberAccessExpression memberAccessExpression) {
        memberAccessExpression.left.accept(this);

        return null;
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
    public void visitStatement(Statement statement) {

        if(statement instanceof FunctionDeclaration functionDeclaration){
            define(new BaseSymbol(functionDeclaration.name, FunctionType.of(functionDeclaration), functionDeclaration.isForwardDeclaration()), false);
        }

        super.visitStatement(statement);
    }

    @Override
    public Void accept(VariableDeclaration variableDeclaration) {

        BaseSymbol symbol = new BaseSymbol(variableDeclaration.name, variableDeclaration.declarator.typeSpecifier(), variableDeclaration.declarator.typeQualifier().isExtern());

        define(symbol, variableDeclaration.isParameter);

        super.accept(variableDeclaration);

        return null;
    }

    @Override
    public Void accept(ContinueStatement continueStatement) {
        return null;
    }

    @Override
    public Void accept(BreakStatement breakStatement) {
        return null;
    }

    @Override
    public Void visit(StructDeclaration structDeclaration) {
        return null;
    }
}
