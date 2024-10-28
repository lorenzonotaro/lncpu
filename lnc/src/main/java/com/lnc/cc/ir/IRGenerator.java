package com.lnc.cc.ir;

import com.lnc.cc.common.*;
import com.lnc.cc.ast.*;
import com.lnc.common.IntUtils;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;

import java.util.ArrayList;
import java.util.List;

public class IRGenerator extends ScopedASTVisitor<IROperand> {


    private final List<IRUnit> blocks = new ArrayList<>();

    private FlatSymbolTable globalSymbolTable;

    private IRUnit currentUnit;

    public IRGenerator(AST ast) {
        super(ast);
        globalSymbolTable = FlatSymbolTable.flatten(ast.getGlobalScope());
    }



    @Override
    public Void accept(WhileStatement whileStatement) {

        return null;
    }

    @Override
    public Void accept(ForStatement forStatement) {

        if (forStatement.initializer != null)
            visitStatement(forStatement.initializer);

        IRBlock startBlock = currentUnit.newBlock();
        IRBlock bodyBlock = currentUnit.newBlock();
        IRBlock continueBlock = currentUnit.newBlock();

        currentUnit.getCurrentBlock().setNext(startBlock);

        currentUnit.setCurrentBlock(startBlock);
        branchIfFalse(forStatement.condition, continueBlock, bodyBlock, null);

        currentUnit.setCurrentBlock(bodyBlock);
        visitStatement(forStatement.body);
        if (forStatement.increment != null)
            forStatement.increment.accept(this);

        emit(new Goto(startBlock));

        currentUnit.setCurrentBlock(continueBlock);


        return null;

    }

    @Override
    public Void accept(ReturnStatement returnStatement) {
        IROperand value = returnStatement.value.accept(this);
        emit(new Ret(value));
        return null;
    }

    @Override
    public Void accept(IfStatement ifStatement) {

        IRBlock thenBlock = currentUnit.newBlock();
        IRBlock elseBlock = ifStatement.elseStatement == null ? null : currentUnit.newBlock();
        IRBlock continueBlock = currentUnit.newBlock();

        if(elseBlock == null){
            branchIfFalse(ifStatement.condition, continueBlock, thenBlock, null);

            currentUnit.setCurrentBlock(thenBlock);
            visitStatement(ifStatement.thenStatement);

        }else{
            branchIfFalse(ifStatement.condition, elseBlock, thenBlock, continueBlock);

            currentUnit.setCurrentBlock(thenBlock);
            visitStatement(ifStatement.thenStatement);

            emit(new Goto(continueBlock));

            currentUnit.setCurrentBlock(elseBlock);
            visitStatement(ifStatement.elseStatement);
        }

        currentUnit.setCurrentBlock(continueBlock);

        return null;
    }


    private void branchIfTrue(Expression cond, IRBlock target, IRBlock fallThrough, IRBlock continueBlock) {
        switch(cond.type){
            case BINARY -> {
                BinaryExpression binaryExpression = (BinaryExpression) cond;
                IROperand left = binaryExpression.left.accept(this);
                IROperand right = binaryExpression.right.accept(this);

                switch(binaryExpression.operator){
                    case EQ -> {
                        emit(new Jeq(left, right, target, fallThrough, continueBlock));
                    }
                    case NE -> {
                        emit(new Jne(left, right, target, fallThrough, continueBlock));
                    }
                    case GT -> {
                        emit(new Jgt(left, right, target, fallThrough, continueBlock));
                    }
                    case GE -> {
                        emit(new Jge(left, right, target, fallThrough, continueBlock));
                    }
                    case LT -> {
                        emit(new Jlt(left, right, target, fallThrough, continueBlock));
                    }
                    case LE -> {
                        emit(new Jle(left, right, target, fallThrough, continueBlock));
                    }
                    default -> throw new IllegalStateException("Unexpected binary operator: " + binaryExpression.operator);
                }
            }
            default -> {
                IROperand condition = cond.accept(this);
                emit(new Jne(condition, new ImmediateOperand((byte) 0), target, fallThrough, continueBlock));
            }
        }
    }

    private void branchIfFalse(Expression cond, IRBlock target, IRBlock fallThrough, IRBlock continueBlock) {
        switch(cond.type){
            case BINARY -> {
                BinaryExpression binaryExpression = (BinaryExpression) cond;
                IROperand left = binaryExpression.left.accept(this);
                IROperand right = binaryExpression.right.accept(this);

                switch(binaryExpression.operator){
                    case EQ -> {
                        emit(new Jne(left, right, target, fallThrough, continueBlock));
                    }
                    case NE -> {
                        emit(new Jeq(left, right, target, fallThrough, continueBlock));
                    }
                    case GT -> {
                        emit(new Jle(left, right, target, fallThrough, continueBlock));
                    }
                    case GE -> {
                        emit(new Jlt(left, right, target, fallThrough, continueBlock));
                    }
                    case LT -> {
                        emit(new Jge(left, right, target, fallThrough, continueBlock));
                    }
                    case LE -> {
                        emit(new Jgt(left, right, target, fallThrough, continueBlock));
                    }
                    default -> throw new IllegalStateException("Unexpected binary operator: " + binaryExpression.operator);
                }
            }
            default -> {
                IROperand condition = cond.accept(this);
                emit(new Jeq(condition, new ImmediateOperand((byte) 0), target, fallThrough, continueBlock));
            }
        }
    }


    @Override
    public Void accept(FunctionDeclaration functionDeclaration) {
        return super.accept(functionDeclaration);
    }

    @Override
    public Symbol resolveSymbol(Token token) {
        Scope scope = getCurrentScope();

        Symbol symbol;

        if(currentUnit != null){
            symbol = currentUnit.resolveSymbol(scope, token.lexeme);
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
            currentUnit = new IRUnit(functionDeclaration);
            blocks.add(currentUnit);
        }

        super.visitStatement(statement);
    }

    @Override
    public IROperand accept(AssignmentExpression assignmentExpression) {

        var value = assignmentExpression.right.accept(this);

        var dest = assignmentExpression.left.accept(this);

        if(dest.type == IROperand.Type.LOCATION){
            emit(new Store((Location) dest, value));
        }else{
            emit(new Move(dest, value));
        }

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
        Symbol symbol = resolveSymbol(identifierExpression.token);
        return new Location(symbol);
    }

    @Override
    public IROperand accept(MemberAccessExpression memberAccessExpression) {
        throw new CompileException("member access expressions not supported", memberAccessExpression.token);
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
                return operand;
            }
            case NEGATE -> {
                emit(new Neg(operand));
                return operand;
            }
            case DEREFERENCE -> {
                throw new Error("dereference IR not implemented");
            }
            case ADDRESS_OF -> {
                throw new Error("address of IR not implemented");
            }
            case INCREMENT -> {
                if(unaryExpression.associativity == UnaryExpression.Associativity.LEFT){
                    emit(new Inc(operand));
                    return operand;
                }else{
                    var vr = allocVR();
                    emit(new Move(vr, operand));
                    emit(new Inc(operand));
                    return vr;
                }
            }
            case DECREMENT -> {
                if(unaryExpression.associativity == UnaryExpression.Associativity.LEFT){
                    emit(new Dec(operand));
                    return operand;
                }else{
                    var vr = allocVR();
                    emit(new Move(vr, operand));
                    emit(new Dec(operand));
                    return vr;
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + unaryExpression.operator);
        }
    }

    private VirtualRegister allocVR() {
        return currentUnit.createVirtualRegister();
    }

    private void emit(IRInstruction instruction){
        currentUnit.emit(instruction);
    }

    public List<IRUnit> getUnits() {
        return blocks;
    }
}
