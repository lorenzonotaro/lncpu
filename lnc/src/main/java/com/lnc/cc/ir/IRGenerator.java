package com.lnc.cc.ir;

import com.lnc.cc.common.VoidStatementVisitor;
import com.lnc.cc.ast.*;

import java.util.ArrayList;
import java.util.List;

public class IRGenerator implements VoidStatementVisitor<Void> {

    private final AST ast;

    private final List<IRBlock> blocks = new ArrayList<>();

    private IRBlock currentBlock;

    public IRGenerator(AST ast) {
        this.ast = ast;
    }

    @Override
    public Void accept(FunctionDeclaration functionDeclaration) {

        return VoidStatementVisitor.super.accept(functionDeclaration);
    }

    @Override
    public void visitStatement(Statement statement) {

        if (statement instanceof ScopedStatement scoped){
            currentBlock = new IRBlock(currentBlock, scoped);
            blocks.add(currentBlock);
        }

        statement.accept(this);
    }

    @Override
    public Void accept(AssignmentExpression assignmentExpression) {
        return null;
    }

    @Override
    public Void accept(BinaryExpression binaryExpression) {
        return null;
    }

    @Override
    public Void accept(CallExpression callExpression) {
        return null;
    }

    @Override
    public Void accept(IdentifierExpression identifierExpression) {
        return null;
    }

    @Override
    public Void accept(MemberAccessExpression memberAccessExpression) {
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
        return null;
    }

    @Override
    public Void accept(UnaryExpression unaryExpression) {
        return null;
    }
}
