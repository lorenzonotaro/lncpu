package com.lnc.cc.anaylsis;

import com.lnc.cc.ast.*;
import com.lnc.cc.types.FunctionType;
import com.lnc.cc.types.I8Type;
import com.lnc.cc.types.PointerType;
import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.Logger;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;

public class TypeChecker extends ScopedASTVisitor<TypeSpecifier> {

    public TypeChecker(AST ast) {
        super(ast);
        setCurrentScope(ast.getGlobalScope());
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

        TypeSpecifier functionType = callExpression.callee.accept(this);


        if (!(functionType instanceof FunctionType function)) {
            throw new CompileException("called object is not a function or function pointer", callExpression.callee.token);
        }

        if (callExpression.arguments.length != function.parameterTypes.length) {
            throw new CompileException("function expects " + function.parameterTypes.length + " arguments, but " + callExpression.arguments.length + " were given", callExpression.token);
        }

        for (int i = 0; i < callExpression.arguments.length; i++) {
            TypeSpecifier argumentType = callExpression.arguments[i].accept(this);
            check(argumentType, function.parameterTypes[i], callExpression.arguments[i].token);
        }

        return function.returnType;
    }

    @Override
    public TypeSpecifier accept(IdentifierExpression identifierExpression) {
        return resolveSymbol(identifierExpression.token).getType();
    }

    @Override
    public TypeSpecifier accept(MemberAccessExpression memberAccessExpression) {
        throw new Error("Member access not implemented");
    }

    @Override
    public TypeSpecifier accept(NumericalExpression numericalExpression) {
        return new I8Type();
    }

    @Override
    public TypeSpecifier accept(StringExpression stringExpression) {
        throw new Error("String expression not implemented");
    }

    @Override
    public TypeSpecifier accept(SubscriptExpression subscriptExpression) {
        throw new Error("Subscript expression not implemented");
    }

    @Override
    public TypeSpecifier accept(UnaryExpression unaryExpression) {
        if(unaryExpression.operator == UnaryExpression.Operator.DEREFERENCE){

            TypeSpecifier operandType = unaryExpression.operand.accept(this);
            if(operandType.type == TypeSpecifier.Type.POINTER){
                return ((PointerType)operandType).getBaseType();
            }else{
                throw new CompileException("dereferencing non-pointer type", unaryExpression.token);
            }
        }

        return unaryExpression.operand.accept(this);
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

        throw new CompileException("incompatible types: " + type + " and " + expectedType, location);
    }
}

