package com.lnc.cc.anaylsis;

import com.lnc.cc.ast.*;
import com.lnc.cc.common.ScopedASTVisitor;
import com.lnc.cc.types.*;
import com.lnc.common.IntUtils;
import com.lnc.common.Logger;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;

import java.util.HashMap;
import java.util.Map;

public class TypeChecker extends ScopedASTVisitor<TypeSpecifier> {

    public TypeChecker(AST ast) {
        super(ast);
    }

    @Override
    public Void accept(VariableDeclaration variableDeclaration) {

        if (variableDeclaration.declarator.typeSpecifier().type == TypeSpecifier.Type.STRUCT) {
            StructType structType = (StructType) variableDeclaration.declarator.typeSpecifier();
            if (structType.hasDefinition()) {
                if(structType.providesDefinition()){
                    defineStruct(structType.getName(), structType.getDefinition());
                }
            } else {
                StructDefinitionType type = resolveStruct(structType.getName());
                structType.bindDefinition(type);
            }
        }

        return super.accept(variableDeclaration);
    }


    @Override
    public Void visit(StructDeclaration structDeclaration) {
        StructDefinitionType structDefinition = structDeclaration.getStructDefinition();

        if(structDefinition == null){
            return null;
        }

        defineStruct(structDeclaration.name, structDefinition);
        return null;
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
            throw new CompileException("function expects " + function.parameterTypes.length + " arguments, but " + callExpression.arguments.length + (callExpression.arguments.length == 1 ? " was" : " were") + " given", callExpression.token);
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
        TypeSpecifier arrayType = subscriptExpression.left.accept(this);

        if (!(arrayType instanceof AbstractSubscriptableType pointer)) {
            throw new CompileException("subscripted value is not an array or pointer", subscriptExpression.token);
        }

        TypeSpecifier indexType = subscriptExpression.index.accept(this);


        if(indexType.type != TypeSpecifier.Type.UI8 && indexType.type != TypeSpecifier.Type.I8){
            throw new CompileException("array subscript is not an integer", subscriptExpression.token);
        }

        return pointer.getBaseType();
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
        }else if(unaryExpression.operator == UnaryExpression.Operator.ADDRESS_OF){
            TypeSpecifier operandType = unaryExpression.operand.accept(this);
            return new PointerType(operandType);
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

        if(type.typeSize() == expectedType.typeSize()) {
            Logger.compileWarning("implicit conversion from '" + type + "' to '" + expectedType + "'.", location);
            return;
        }

        throw new CompileException("incompatible types: " + type + " and " + expectedType, location);
    }

    @Override
    public Void accept(ReturnStatement returnStatement) {
        TypeSpecifier returnType = returnStatement.value == null ? null : returnStatement.value.accept(this);

        var currentFunction = getCurrentFunction();

        if(currentFunction == null){
            throw new CompileException("return statement outside of function", returnStatement.token);
        }

        TypeSpecifier expectedReturnType = currentFunction.declarator.typeSpecifier();

        if(returnType == null && expectedReturnType.type == TypeSpecifier.Type.VOID){
            return null;
        }else if(returnType == null){
            throw new CompileException("return with no value when function expects '" + expectedReturnType + "'", returnStatement.token);
        }else if(expectedReturnType.type == TypeSpecifier.Type.VOID){
            throw new CompileException("return with a value in function returning void", returnStatement.token);
        }

        check(returnType, expectedReturnType, returnStatement.token);

        return null;
    }

    private void checkStructCompleteness(StructDefinitionType definition, Token name) {
        if(definition.isComplete())
            return;

        Map<String, StructFieldEntry> fieldMap = new HashMap<>();

        int offset = 0;
        for(VariableDeclaration field : definition.getFields()){
            if(field.declarator.typeSpecifier().isPrimitive()){
                fieldMap.put(field.name.lexeme, new StructFieldEntry(offset, field));
                offset += field.declarator.typeSpecifier().typeSize();
            }else if(field.declarator.typeSpecifier().type == TypeSpecifier.Type.STRUCT){
                StructType structType = (StructType) field.declarator.typeSpecifier();

                if (structType.hasDefinition()) {
                    if(structType.providesDefinition()){
                        defineStruct(structType.getName(), structType.getDefinition());
                    }
                } else {
                    StructDefinitionType type = resolveStruct(structType.getName());
                    structType.bindDefinition(type);
                }

                fieldMap.put(field.name.lexeme, new StructFieldEntry(offset, field));
                offset += field.declarator.typeSpecifier().typeSize();

            }else{
                throw new CompileException("unexpected struct field type (should not happen)", field.name);
            }
            offset += field.declarator.typeSpecifier().typeSize();
        }

        definition.setFieldMap(fieldMap);
    }

    @Override
    public void defineStruct(Token name, StructDefinitionType definition) {
        checkStructCompleteness(definition, name);
        super.defineStruct(name, definition);
    }

    @Override
    public StructDefinitionType resolveStruct(Token token) {
        StructDefinitionType struct = super.resolveStruct(token);
        checkStructCompleteness(struct, token);
        return struct;
    }

    @Override
    public Void accept(ContinueStatement continueStatement) {
        return null;
    }

    @Override
    public Void accept(BreakStatement breakStatement) {
        return null;
    }

}

