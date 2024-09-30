package com.lnc.cc.anaylsis;

import com.lnc.cc.ast.*;
import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.Logger;
import com.lnc.common.frontend.Token;

public class TypeResolver implements IASTVisitor<Void, TypeSpecifier> {

    private final AST ast;

    private boolean success = true;

    public TypeResolver(AST ast) {
        this.ast = ast;
    }

    public boolean resolveTypes() {

        for (Declaration declaration : ast.getDeclarations()) {
            declaration.accept(this);
        }

        return success;
    }

    @Override
    public TypeSpecifier accept(AssignmentExpression assignmentExpression) {

        TypeSpecifier leftType = assignmentExpression.left.accept(this);

        TypeSpecifier rightType = assignmentExpression.right.accept(this);

        check(rightType, leftType, assignmentExpression.operator);

        return leftType;
    }

    @Override
    public TypeSpecifier accept(BinaryExpression binaryExpression) {

        TypeSpecifier leftType = binaryExpression.left.accept(this);

        TypeSpecifier rightType = binaryExpression.right.accept(this);

        check(leftType, rightType, binaryExpression.token);

        return leftType;
    }

    @Override
    public TypeSpecifier accept(CallExpression callExpression) {
        return callExpression.callee.accept(this);
    }

    @Override
    public TypeSpecifier accept(IdentifierExpression identifierExpression) {
        return null;
    }

    @Override
    public TypeSpecifier accept(MemberAccessExpression memberAccessExpression) {
        return null;
    }

    @Override
    public TypeSpecifier accept(NumericalExpression numericalExpression) {
        return null;
    }

    @Override
    public TypeSpecifier accept(StringExpression stringExpression) {
        return null;
    }

    @Override
    public TypeSpecifier accept(SubscriptExpression subscriptExpression) {
        return null;
    }

    @Override
    public TypeSpecifier accept(UnaryExpression unaryExpression) {
        return null;
    }

    @Override
    public Void accept(BlockStatement blockStatement) {
        return null;
    }

    @Override
    public Void accept(ExpressionStatement expressionStatement) {
        return null;
    }

    @Override
    public Void accept(ForStatement forStatement) {
        return null;
    }

    @Override
    public Void accept(FunctionDeclaration functionDeclaration) {
        return null;
    }

    @Override
    public Void accept(IfStatement ifStatement) {
        return null;
    }

    @Override
    public Void accept(ReturnStatement returnStatement) {
        return null;
    }

    @Override
    public Void accept(VariableDeclaration variableDeclaration) {
        return null;
    }

    @Override
    public Void accept(WhileStatement whileStatement) {
        return null;
    }

    private void check(TypeSpecifier type, TypeSpecifier expectedType, Token location) {
        if (type == null || expectedType == null) {
            throw new IllegalStateException("Type or expected type is null");
        }

        if(type.compatible(expectedType)) {
            return;
        }

        if(type.size() == expectedType.size()) {
            Logger.compileWarning("implicit conversion from " + type + " to " + expectedType, location);
            return;
        }

        Logger.compileWarning("incompatible types: " + type + " and " + expectedType, location);
        success = false;
    }
}

