package com.lnc.cc.ir;

import com.lnc.cc.common.*;
import com.lnc.cc.ast.*;
import com.lnc.common.IntUtils;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;

import java.util.ArrayList;
import java.util.List;

public class IRGenerator extends ScopedASTVisitor<IROperand> {


    private final List<IRBlock> blocks = new ArrayList<>();

    private FlatSymbolTable globalSymbolTable;

    private IRBlock currentBlock;

    public IRGenerator(AST ast) {
        super(ast);
        globalSymbolTable = FlatSymbolTable.flatten(ast.getGlobalScope());
    }



    @Override
    public Void accept(WhileStatement whileStatement) {
        return super.accept(whileStatement);
    }

    @Override
    public Void accept(VariableDeclaration variableDeclaration) {
        return super.accept(variableDeclaration);
    }

    @Override
    public Void accept(ForStatement forStatement) {
        return super.accept(forStatement);
    }

    @Override
    public Void accept(ReturnStatement returnStatement) {
        return super.accept(returnStatement);
    }

    @Override
    public Void accept(IfStatement ifStatement) {
        return super.accept(ifStatement);
    }

    @Override
    public Void accept(FunctionDeclaration functionDeclaration) {
        return super.accept(functionDeclaration);
    }

    @Override
    public Symbol resolveSymbol(Token token) {
        Scope scope = getCurrentScope();

        Symbol symbol;

        if(currentBlock != null){
            symbol = currentBlock.resolveSymbol(scope, token.lexeme);
            if(symbol != null){
                return symbol;
            }

            symbol = globalSymbolTable.resolveSymbol(scope, token.lexeme);


            if(symbol != null){
                return symbol;
            }

            throw new CompileException("symbol not found: " + token.lexeme, token);
        }

        symbol = globalSymbolTable.resolveSymbol(scope, token.lexeme);

        if(symbol != null){
            return symbol;
        }

        throw new CompileException("symbol not found: " + token.lexeme, token);
    }

    @Override
    public void visitStatement(Statement statement) {

        if(statement instanceof FunctionDeclaration functionDeclaration){
            currentBlock = new IRBlock(functionDeclaration);
            blocks.add(currentBlock);
        }

        super.visitStatement(statement);
    }

    @Override
    public IROperand accept(AssignmentExpression assignmentExpression) {
        return null;
    }

    @Override
    public IROperand accept(BinaryExpression binaryExpression) {
        return null;
    }

    @Override
    public IROperand accept(CallExpression callExpression) {
        return null;
    }

    @Override
    public IROperand accept(IdentifierExpression identifierExpression) {
        return null;
    }

    @Override
    public IROperand accept(MemberAccessExpression memberAccessExpression) {
        return null;
    }

    @Override
    public IROperand accept(NumericalExpression numericalExpression) {

        if(IntUtils.inByteRange(numericalExpression.value)){
            return new ImmediateOperand((byte) (numericalExpression.value & 0xFF));
        }

        throw new CompileException("value out of range: " + numericalExpression.value, numericalExpression.token);
    }

    @Override
    public IROperand accept(StringExpression stringExpression) {
        throw new CompileException("string expressions not supported", stringExpression.token);
    }

    @Override
    public IROperand accept(SubscriptExpression subscriptExpression) {
        throw new CompileException("subscript expressions not supported", subscriptExpression.token);
    }

    @Override
    public IROperand accept(UnaryExpression unaryExpression) {
        IROperand operand = unaryExpression.operand.accept(this);

        if(operand.type == IROperand.Type.LOCATION){
            var vr = allocVR();
            emit(new Load(vr, (Location) operand));
            operand = vr;
        }

        switch (unaryExpression.operator){
            case NOT -> {
                emit(new Not(operand));
            }

        }

        //TODO: implement other unary operators
        return null;
    }

    private VirtualRegister allocVR() {
        return currentBlock.createVirtualRegister();
    }

    public void emit(IRInstruction instruction){
        currentBlock.emit(instruction);
    }
}
