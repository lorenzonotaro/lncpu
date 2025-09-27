package com.lnc.cc.anaylsis;

import com.lnc.cc.ast.*;
import com.lnc.cc.common.ConstantSymbol;
import com.lnc.cc.common.ScopedASTVisitor;
import com.lnc.cc.common.BaseSymbol;
import com.lnc.cc.types.FunctionType;
import com.lnc.cc.types.TypeQualifier;

/**
 * The LocalResolver class extends the ScopedASTVisitor and is responsible for
 * resolving symbols and defining constants or structures within the Abstract Syntax Tree (AST)
 * during compilation. It primarily focuses on symbol resolution, scope management, and
 * handling various types of expressions and statements in the AST.
 *
 * This class provides specific implementations for visiting different AST node types such as
 * expressions, statements, declarations, etc., and performs tasks like resolving identifiers,
 * defining variables, constants, and navigating through nested scopes as necessary.
 */
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
        defineConstant(stringExpression.token.lexeme, ConstantSymbol.string(stringExpression.token));
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
            define(BaseSymbol.variable(functionDeclaration.name, FunctionType.of(functionDeclaration), new TypeQualifier(functionDeclaration.isForwardDeclaration(), false, true, functionDeclaration.declarator.typeQualifier().isExport())));
        }

        super.visitStatement(statement);
    }

    @Override
    public Void visit(VariableDeclaration variableDeclaration) {

        BaseSymbol symbol;

        if (variableDeclaration.isParameter) {
            symbol = BaseSymbol.parameter(variableDeclaration.name, variableDeclaration.declarator.typeSpecifier(), variableDeclaration.declarator.typeQualifier(), variableDeclaration.getParameterIndex());
        } else {
            symbol = BaseSymbol.variable(variableDeclaration.name, variableDeclaration.declarator.typeSpecifier(), variableDeclaration.declarator.typeQualifier());
        }

        define(symbol);

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
