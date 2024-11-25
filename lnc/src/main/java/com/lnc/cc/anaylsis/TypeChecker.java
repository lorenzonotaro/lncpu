package com.lnc.cc.anaylsis;

import com.lnc.cc.ast.*;
import com.lnc.cc.common.ScopedASTVisitor;
import com.lnc.cc.types.*;
import com.lnc.common.IntUtils;
import com.lnc.common.Logger;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

import java.util.HashMap;
import java.util.Map;

public class TypeChecker extends ScopedASTVisitor<TypeSpecifier> {

    public TypeChecker(AST ast) {
        super(ast);
    }

    @Override
    public Void accept(VariableDeclaration variableDeclaration) {

        checkTypeCompleteness(variableDeclaration.declarator.typeSpecifier());

        return super.accept(variableDeclaration);
    }

    private void checkTypeCompleteness(TypeSpecifier type) {
        if (type.type == TypeSpecifier.Type.STRUCT) {
            checkStructCompleteness((StructType) type);
        }else if(type.type == TypeSpecifier.Type.ARRAY){
            checkTypeCompleteness(((ArrayType)type).getBaseType());
        }else if(type.type == TypeSpecifier.Type.POINTER){
            checkTypeCompleteness(((PointerType)type).getBaseType());
        }
    }

    private void checkStructCompleteness(StructType structType) {
        if (structType.hasDefinition()) {
            if(structType.providesDefinition()){
                defineStruct(structType.getName(), structType.getDefinition());
            }
        } else {
            StructDefinitionType type = resolveStruct(structType.getName());
            structType.bindDefinition(type);
        }
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
        TypeSpecifier left = memberAccessExpression.left.accept(this);

        if(memberAccessExpression.accessOperator.type == TokenType.ARROW){
            if(left.type != TypeSpecifier.Type.POINTER){
                throw new CompileException("base operand of '->' operator has non pointer type '" + left + "'", memberAccessExpression.token);
            }

            var baseType = ((PointerType)left).getBaseType();

            if(baseType.type != TypeSpecifier.Type.STRUCT){
                throw new CompileException("base operand of '->' operator has non-struct type '" + baseType + "'", memberAccessExpression.token);
            }

            StructFieldEntry fieldEntry = getStructFieldEntry(memberAccessExpression, (StructType) baseType);

            return fieldEntry.getField().declarator.typeSpecifier();
        }else if(memberAccessExpression.accessOperator.type == TokenType.DOT){
            if(left.type != TypeSpecifier.Type.STRUCT){
                throw new CompileException("base operand of '.' operator has non-struct type '" + left + "'", memberAccessExpression.token);
            }

            StructFieldEntry fieldEntry = getStructFieldEntry(memberAccessExpression, (StructType) left);

            return fieldEntry.getField().declarator.typeSpecifier();
        }

        throw new CompileException("unexpected access operator", memberAccessExpression.token);
    }

    private static StructFieldEntry getStructFieldEntry(MemberAccessExpression memberAccessExpression, StructType structType) {

        StructDefinitionType struct = structType.getDefinition();

        StructFieldEntry fieldEntry = struct.getField(memberAccessExpression.right.lexeme);

        if(fieldEntry == null){
            throw new CompileException("struct '" + struct.getDefinitionToken().lexeme + "' has no member named '" + memberAccessExpression.right.lexeme + "'", memberAccessExpression.token);
        }
        return fieldEntry;
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
            }else if(field.declarator.typeSpecifier().type == TypeSpecifier.Type.STRUCT){
                checkStructCompleteness((StructType) field.declarator.typeSpecifier());
                fieldMap.put(field.name.lexeme, new StructFieldEntry(offset, field));
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

