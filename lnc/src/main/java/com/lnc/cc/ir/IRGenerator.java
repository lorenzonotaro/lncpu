package com.lnc.cc.ir;

import com.lnc.cc.codegen.RegisterClass;
import com.lnc.cc.common.*;
import com.lnc.cc.ast.*;
import com.lnc.cc.types.FunctionType;
import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.IntUtils;
import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class IRGenerator extends ScopedASTVisitor<IROperand> {


    private final List<IRUnit> blocks = new ArrayList<>();

    private final FlatSymbolTable globalSymbolTable;
    private final Scope globalScope;

    private IRUnit currentUnit;

    public IRGenerator(AST ast) {
        super(ast);
        this.globalScope = ast.getGlobalScope();
        globalSymbolTable = FlatSymbolTable.flatten(ast.getGlobalScope());
    }



    @Override
    public Void accept(WhileStatement whileStatement) {

        IRBlock startBlock = currentUnit.newBlock();
        IRBlock bodyBlock = currentUnit.newBlock();
        IRBlock continueBlock = currentUnit.newBlock();

        currentUnit.getCurrentBlock().setNext(startBlock);

        currentUnit.setCurrentBlock(startBlock);

        currentUnit.enterLoop(new LoopInfo(startBlock, continueBlock));

        branchIfFalse(whileStatement.condition, forwardTo(continueBlock), forwardTo(bodyBlock));

        currentUnit.setCurrentBlock(bodyBlock);

        visitStatement(whileStatement.body);

        emit(new Goto(startBlock));

        currentUnit.exitLoop();

        startBlock.setNext(continueBlock);

        currentUnit.setCurrentBlock(continueBlock);

        return null;
    }

    @Override
    public Void accept(DoWhileStatement doWhileStatement) {

        IRBlock bodyBlock = currentUnit.newBlock();
        IRBlock continueBlock = currentUnit.newBlock();

        currentUnit.getCurrentBlock().setNext(bodyBlock);

        currentUnit.setCurrentBlock(bodyBlock);

        currentUnit.enterLoop(new LoopInfo(bodyBlock, continueBlock));

        visitStatement(doWhileStatement.body);

        branchIfFalse(doWhileStatement.condition, forwardTo(continueBlock), forwardTo(bodyBlock));

        currentUnit.exitLoop();

        bodyBlock.setNext(continueBlock);
        currentUnit.setCurrentBlock(continueBlock);

        return null;
    }

    @Override
    public Void accept(ContinueStatement continueStatement) {
        LoopInfo loopInfo = currentUnit.getCurrentLoopInfo();

        if(loopInfo == null){
            throw new CompileException("continue statement outside of loop", continueStatement.token);
        }

        emit(new Goto(loopInfo.continueTarget()));

        return null;
    }

    @Override
    public Void accept(BreakStatement breakStatement) {
        LoopInfo loopInfo = currentUnit.getCurrentLoopInfo();

        if(loopInfo == null){
            throw new CompileException("break statement outside of loop", breakStatement.token);
        }

        emit(new Goto(loopInfo.breakTarget()));

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

        currentUnit.enterLoop(new LoopInfo(startBlock, continueBlock));

        branchIfFalse(forStatement.condition, forwardTo(continueBlock), forwardTo(bodyBlock));

        currentUnit.setCurrentBlock(bodyBlock);
        visitStatement(forStatement.body);

        if (forStatement.increment != null) {
            IROperand operand = forStatement.increment.accept(this);

            if(operand.type == IROperand.Type.VIRTUAL_REGISTER){
                releaseVR((VirtualRegister) operand);
            }

        }

        emit(new Goto(startBlock));

        currentUnit.exitLoop();

        startBlock.setNext(continueBlock);

        currentUnit.setCurrentBlock(continueBlock);

        return null;

    }

    private IRBlock forwardTo(IRBlock continueBlock) {
        IRBlock block = currentUnit.newBlock();

        block.emit(new Goto(continueBlock));

        return block;
    }

    @Override
    public Void accept(ReturnStatement returnStatement) {
        IROperand value = null;

        if(returnStatement.value != null){
            value = returnStatement.value.accept(this);
        }

        if(value == null) {
            emit(new Ret(null));
        }else{
            if (value.type != IROperand.Type.VIRTUAL_REGISTER || ((VirtualRegister) value).getRegisterClass() != RegisterClass.ANY) {
                var vr = allocVR();
                emit(new Move(value, vr));
                value = vr;
            }

            emit(new Ret(value));
        }


        if(value != null && value.type == IROperand.Type.VIRTUAL_REGISTER){
            releaseVR((VirtualRegister) value);
        }

        return null;
    }

    @Override
    public Void accept(IfStatement ifStatement) {

        IRBlock currentBlock = currentUnit.getCurrentBlock();
        IRBlock thenBlock = currentUnit.newBlock();
        IRBlock continueBlock = currentUnit.newBlock();

        if(ifStatement.elseStatement != null) {

            IRBlock elseBlock = currentUnit.newBlock();

            branchIfFalse(ifStatement.condition, elseBlock, thenBlock);

            currentUnit.setCurrentBlock(thenBlock);
            visitStatement(ifStatement.thenStatement);
            emit(new Goto(continueBlock));

            currentUnit.setCurrentBlock(elseBlock);
            visitStatement(ifStatement.elseStatement);
            emit(new Goto(continueBlock));

        }else{
            branchIfFalse(ifStatement.condition, forwardTo(continueBlock), thenBlock);

            currentUnit.setCurrentBlock(thenBlock);
            visitStatement(ifStatement.thenStatement);
            emit(new Goto(continueBlock));
        }

        currentBlock.setNext(continueBlock);
        currentUnit.setCurrentBlock(continueBlock);

        return null;
    }

    private void branchIfFalse(Expression cond, IRBlock takenBranch, IRBlock nonTakenBranch) {
        if (Objects.requireNonNull(cond.type) == Expression.Type.BINARY) {
            BinaryExpression binaryExpression = (BinaryExpression) cond;
            IROperand left = binaryExpression.left.accept(this);
            IROperand right = binaryExpression.right.accept(this);

            if(left.type == IROperand.Type.LOCATION) {
                var vr = allocVR();
                emit(new Load(vr, (Location) left));
                left = vr;
            }

            switch (binaryExpression.operator) {
                case EQ -> emit(new Jeq(left, right, nonTakenBranch, takenBranch));
                case NE -> emit(new Jeq(left, right, takenBranch, nonTakenBranch));
                case GT -> emit(new Jle(left, right, takenBranch, nonTakenBranch));
                case GE -> emit(new Jlt(left, right, takenBranch, nonTakenBranch));
                case LT -> emit(new Jlt(left, right, nonTakenBranch, takenBranch));
                case LE -> emit(new Jle(left, right, nonTakenBranch, takenBranch));
                default -> {
                    emit(new Jeq(left, new ImmediateOperand((byte) 0), takenBranch, nonTakenBranch));
                }
            }

            if(left.type == IROperand.Type.VIRTUAL_REGISTER){
                releaseVR((VirtualRegister) left);
            }

            if(right.type == IROperand.Type.VIRTUAL_REGISTER){
                releaseVR((VirtualRegister) right);
            }

        } else {
            IROperand condition = cond.accept(this);

            if(condition.type == IROperand.Type.LOCATION) {
                var vr = allocVR();
                emit(new Load(vr, (Location) condition));
                condition = vr;
            }

            emit(new Jeq(condition, new ImmediateOperand((byte) 0), nonTakenBranch, takenBranch));

            if(condition.type == IROperand.Type.VIRTUAL_REGISTER){
                releaseVR((VirtualRegister) condition);
            }
        }
    }

    private void branchIfTrue(Expression cond, IRBlock takenBranch, IRBlock nonTakenBranch) {
        if (Objects.requireNonNull(cond.type) == Expression.Type.BINARY) {
            BinaryExpression binaryExpression = (BinaryExpression) cond;
            IROperand left = binaryExpression.left.accept(this);
            IROperand right = binaryExpression.right.accept(this);

            if(left.type == IROperand.Type.LOCATION) {
                var vr = allocVR();
                emit(new Load(vr, (Location) left));
                left = vr;
            }

            switch (binaryExpression.operator) {
                case EQ -> emit(new Jeq(left, right, takenBranch, nonTakenBranch));
                case NE -> emit(new Jeq(left, right, nonTakenBranch, takenBranch));
                case GT -> emit(new Jle(left, right, nonTakenBranch, takenBranch));
                case GE -> emit(new Jlt(left, right, nonTakenBranch, takenBranch));
                case LT -> emit(new Jlt(left, right, takenBranch, nonTakenBranch));
                case LE -> emit(new Jle(left, right, takenBranch, nonTakenBranch));
                default -> {
                    emit(new Jeq(left, new ImmediateOperand((byte) 0), nonTakenBranch, takenBranch));
                }
            }

            if(left.type == IROperand.Type.VIRTUAL_REGISTER){
                releaseVR((VirtualRegister) left);
            }

            if(right.type == IROperand.Type.VIRTUAL_REGISTER){
                releaseVR((VirtualRegister) right);
            }

        } else {
            IROperand condition = cond.accept(this);

            if(condition.type == IROperand.Type.LOCATION) {
                var vr = allocVR();
                emit(new Load(vr, (Location) condition));
                condition = vr;
            }

            emit(new Jeq(condition, new ImmediateOperand((byte) 0), takenBranch, nonTakenBranch));

            if(condition.type == IROperand.Type.VIRTUAL_REGISTER){
                releaseVR((VirtualRegister) condition);
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

            symbol = globalSymbolTable.resolveSymbol(globalScope, token.lexeme);


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

        if(value.type == IROperand.Type.VIRTUAL_REGISTER){
            releaseVR((VirtualRegister) value);
        }

        return dest;
    }

    @Override
    public IROperand accept(BinaryExpression binaryExpression) {

        if(binaryExpression.left.type == Expression.Type.NUMERICAL && binaryExpression.right.type == Expression.Type.NUMERICAL){
            return coalesce((NumericalExpression) binaryExpression.left,
                    (NumericalExpression) binaryExpression.right,
                    binaryExpression.operator)
                    .accept(this);
        }

        IROperand left = binaryExpression.left.accept(this);
        IROperand right = binaryExpression.right.accept(this);

        if(left.type == IROperand.Type.IMMEDIATE && right.type == IROperand.Type.IMMEDIATE){
            return coalesce(((ImmediateOperand) left).getValue(), ((ImmediateOperand) right).getValue(), binaryExpression.operator)
                    .accept(this);
        }

        if(left.type == IROperand.Type.LOCATION) {
            var vr = allocVR();
            emit(new Load(vr, (Location) left));
            left = vr;
        }

        if(right.type == IROperand.Type.LOCATION) {
            var vr = allocVR();
            emit(new Load(vr, (Location) right));
            right = vr;
        }

        if(left.type == IROperand.Type.IMMEDIATE && right.type != IROperand.Type.IMMEDIATE){
            if(binaryExpression.operator.isCommutative()){
                IROperand temp = left;
                left = right;
                right = temp;
            }else if(binaryExpression.operator == BinaryExpression.Operator.SUB) {
                IROperand temp = left;
                left = right;
                right = temp;
                binaryExpression.operator = BinaryExpression.Operator.ADD;
                emit(new Not(left));
                emit(new Inc(left));
            }else{
                IROperand vr = allocVR();
                emit(new Move(vr, left));
                left = vr;
            }
        }

        emit(new Bin(left, right, binaryExpression.operator));

        if(right.type == IROperand.Type.VIRTUAL_REGISTER){
            releaseVR((VirtualRegister) right);
        }

        return left;
    }

    private NumericalExpression coalesce(int a, int b, BinaryExpression.Operator operator) {
        switch (operator) {
            case ADD -> {
                return new NumericalExpression(Token.__internal(TokenType.INTEGER, a + b));
            }
            case SUB -> {
                return new NumericalExpression(Token.__internal(TokenType.INTEGER, a - b));
            }
            case MUL -> {
                return new NumericalExpression(Token.__internal(TokenType.INTEGER, a * b));
            }
            case DIV -> {
                return new NumericalExpression(Token.__internal(TokenType.INTEGER, a / b));
            }
            case AND -> {
                return new NumericalExpression(Token.__internal(TokenType.INTEGER, a & b));
            }
            case OR -> {
                return new NumericalExpression(Token.__internal(TokenType.INTEGER, a | b));
            }
            case XOR -> {
                return new NumericalExpression(Token.__internal(TokenType.INTEGER, a ^ b));
            }
            case EQ -> {
                return new NumericalExpression(Token.__internal(TokenType.INTEGER, a == b ? 1 : 0));
            }
            case NE -> {
                return new NumericalExpression(Token.__internal(TokenType.INTEGER, a != b ? 1 : 0));
            }
            case LT -> {
                return new NumericalExpression(Token.__internal(TokenType.INTEGER, a < b ? 1 : 0));
            }
            case GT -> {
                return new NumericalExpression(Token.__internal(TokenType.INTEGER, a > b ? 1 : 0));
            }
            case LE -> {
                return new NumericalExpression(Token.__internal(TokenType.INTEGER, a <= b ? 1 : 0));
            }
            case GE -> {
                return new NumericalExpression(Token.__internal(TokenType.INTEGER, a >= b ? 1 : 0));
            }
            default -> throw new CompileException("invalid operator for coalesce: " + operator, Token.__internal(TokenType.INTEGER, 0));
        }

    }

    private NumericalExpression coalesce(NumericalExpression left, NumericalExpression right, BinaryExpression.Operator operator) {
        return coalesce(left.value, right.value, operator);
    }

    @Override
    public IROperand accept(CallExpression callExpression) {

        var args = new ArrayList<IROperand>();

        for (Expression arg : callExpression.arguments) {
            args.add(arg.accept(this));
        }

        IROperand callee = callExpression.callee.accept(this);

        VirtualRegister destVr = null;

        if(callee.type == IROperand.Type.LOCATION){
            Symbol symbol = ((Location) callee).getSymbol();

            if(symbol.getType().type != TypeSpecifier.Type.FUNCTION){
                throw new CompileException("symbol is not a function: " + symbol.getName(), callExpression.token);
            }

            if (((FunctionType) symbol.getType()).returnType.type != TypeSpecifier.Type.VOID) {
                destVr = allocVR();
            }
        }else{
            destVr = allocVR();
        }

        emit(new Call(destVr, callee, args.toArray(new IROperand[0])));

        if(callee.type == IROperand.Type.VIRTUAL_REGISTER){
            assert callee instanceof VirtualRegister;
            releaseVR((VirtualRegister) callee);
        }

        for (IROperand arg : args) {
            if(arg.type == IROperand.Type.VIRTUAL_REGISTER){
                releaseVR((VirtualRegister) arg);
            }
        }

        return destVr;
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


        switch (unaryExpression.operator){
            case NOT -> {
                emit(new Not(operand));
                return operand;
            }
            case NEGATE -> {
                emit(new Neg(operand));
                return operand;
            }
            case DEREFERENCE -> throw new Error("dereference IR not implemented");
            case ADDRESS_OF -> throw new Error("address of IR not implemented");
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
                    emit(new Move(operand, vr));
                    emit(new Dec(operand));
                    return vr;
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + unaryExpression.operator);
        }
    }

    private VirtualRegister allocVR() {
        return currentUnit.getVrManager().getRegister();
    }

    private void releaseVR(VirtualRegister vr) {
        currentUnit.getVrManager().releaseRegister(vr);
    }

    private void emit(IRInstruction instruction){
        currentUnit.emit(instruction);
    }

    public IR getResult() {
        return new IR(blocks, globalSymbolTable);
    }



}
