package com.lnc.cc.anaylsis;

import com.lnc.cc.ast.*;
import com.lnc.cc.types.FunctionType;
import com.lnc.common.frontend.CompileException;

public class LocalResolver extends ScopedASTVisitor<Void> {

    public LocalResolver(AST ast){
        super(ast);
        setCurrentScope(ast.getGlobalScope());
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
    public Void accept(VariableDeclaration variableDeclaration) {

        super.accept(variableDeclaration);

        Symbol symbol = new Symbol(variableDeclaration.name, variableDeclaration.declarator.typeSpecifier(), variableDeclaration.declarator.typeQualifier().isExtern());

        define(symbol);


        return null;
    }

    @Override
    public Void accept(FunctionDeclaration functionDeclaration) {
        Symbol symbol = new Symbol(functionDeclaration.name, FunctionType.of(functionDeclaration), functionDeclaration.body == null);

        define(symbol);

        super.accept(functionDeclaration);

        return null;
    }

}
