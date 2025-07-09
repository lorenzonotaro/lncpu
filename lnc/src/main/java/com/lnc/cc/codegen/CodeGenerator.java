package com.lnc.cc.codegen;

import com.lnc.assembler.common.LinkMode;
import com.lnc.assembler.common.SectionInfo;
import com.lnc.assembler.linker.LinkTarget;
import com.lnc.assembler.parser.EncodedData;
import com.lnc.assembler.parser.argument.*;
import com.lnc.assembler.parser.argument.Byte;
import com.lnc.cc.ast.BinaryExpression;
import com.lnc.cc.ir.operands.StructMemberAccess;
import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.*;
import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.IntUtils;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

import java.util.*;

public class CodeGenerator extends GraphicalIRVisitor implements IIROperandVisitor<Argument>{
    private final IR ir;

    private CompilerOutput currentOutput;

    private final List<CompilerOutput> outputs = new ArrayList<>();

    public CodeGenerator(IR ir){
        this.ir = ir;
    }

    public List<CompilerOutput> run(){

        outputDataSection();

        for(IRUnit unit : ir.units()){

            if(unit.getFunctionDeclaration().isForwardDeclaration()){
                // Skip forward declarations
                continue;
            }

            currentOutput = new CompilerOutput(unit, new SectionInfo("LNC_" + unit.getFunctionDeclaration().name.lexeme, -1, LinkTarget.ROM, LinkMode.PAGE_FIT, false, false, false));

            GraphColoringRegisterAllocator.run(unit);

            unit.compileFrameInfo();

            visit(unit);

            outputs.add(currentOutput);

            super.reset();
        }

        return outputs;
    }

    private void outputDataSection() {

        this.currentOutput = new CompilerOutput(null, new SectionInfo("LNCDATA", 0x2000, LinkTarget.RAM, LinkMode.FIXED, false, true, false));

        for(var entry : ir.symbolTable().getSymbols().values()){
            var type = entry.getType();
            if(type.type != TypeSpecifier.Type.FUNCTION && entry.isStatic() && !entry.isForward()){
                dataPageVariable(entry.getName(), type.allocSize());
            }
        }

        outputs.add(currentOutput);
    }

    @Override
    public Void visit(Goto aGoto) {
        var target = CodeGenUtils.labelRef(aGoto.getTarget());

        instrf(TokenType.GOTO, target);

        return null;
    }

    @Override
    public Void visit(CondJump condJump) {

        var left = condJump.getLeft().accept(this);
        var right = condJump.getRight().accept(this);

        IRBlock trueTarget = condJump.getTarget();
        final var targetStr = CodeGenUtils.labelRef(trueTarget);
        IRBlock falseTarget = condJump.getFalseTarget();
        final var falseTargetStr = CodeGenUtils.labelRef(falseTarget);

        instrf(TokenType.CMP, left, right);

        boolean preserveBranchOrdering = condJump.preserveBranchOrdering();

        switch(condJump.getCond()){
            case EQ -> {
                instrf(TokenType.JZ, targetStr);

                enqueueCondJumpTargets(preserveBranchOrdering, trueTarget, falseTarget);

            }
            case NE -> {
                instrf(TokenType.JZ, falseTargetStr);

                enqueueCondJumpTargets(false, falseTarget, trueTarget);

            }
            case LT -> {
                instrf(TokenType.JC, targetStr);
                enqueueCondJumpTargets(preserveBranchOrdering, trueTarget, falseTarget);

            }
            case LE -> {
                instrf(TokenType.JC, targetStr);
                instrf(TokenType.JZ, targetStr);

                enqueueCondJumpTargets(preserveBranchOrdering, trueTarget, falseTarget);

            }
            case GT -> {
                instrf(TokenType.JC, falseTargetStr);
                instrf(TokenType.JZ, falseTargetStr);

                enqueueCondJumpTargets(false, falseTarget, trueTarget);

            }
            case GE -> {
                instrf(TokenType.JC, falseTargetStr);

                enqueueCondJumpTargets(false, falseTarget, trueTarget);
            }
        }

        return null;
    }

    private void enqueueCondJumpTargets(boolean loopTest, IRBlock first, IRBlock second) {
        if(loopTest){
            instrf(TokenType.GOTO, CodeGenUtils.labelRef(second));
            enqueue(second);
            enqueue(first);
        }else{
            enqueue(first);
            enqueue(second);
        }
    }

    @Override
    public Void visit(Load load) {

        var target = load.getDest().accept(this);
        var source = load.getSrc().accept(this);

        instrf(TokenType.MOV, source, target);

        return null;
    }

    @Override
    public Void visit(Move move) {

        var target = move.getDest().accept(this);
        var source = move.getSource().accept(this);

        instrf(TokenType.MOV, source, target);

        return null;
    }

    @Override
    public Void visit(Store store) {
        var target = store.getDest().accept(this);
        var value = store.getValue().accept(this);

        instrf(TokenType.MOV, value, target);

        return null;
    }

    @Override
    public Void visit(Ret ret) {
        instrf(TokenType.GOTO, new LabelRef(Token.__internal(TokenType.IDENTIFIER, "_ret")));
        return null;
    }

    @Override
    public Void visit(Bin bin) {

        var dest = bin.getTarget().accept(this);
        var left = bin.getLeft().accept(this);
        var right = bin.getRight().accept(this);

        var destStr = dest.toString();
        var leftStr = left.toString();
        var rightStr = right.toString();

        BinaryExpression.Operator operator = bin.getOperator();
        if(destStr.equals(leftStr)){
            emitBinOp(operator, dest, right);
        }else if(operator.isCommutative() && destStr.equals(rightStr)){
            // If the operator is commutative, we can swap left and right
            emitBinOp(operator, dest, left);
        }else{
            instrf(TokenType.MOV, left, dest);
            emitBinOp(operator, dest, right);
        }

        return null;
    }

    private void emitBinOp(BinaryExpression.Operator op, Argument target, Argument right) {
        switch(op){
            case ADD -> {
                instrf(TokenType.ADD, target, right);
            }
            case SUB -> {
                instrf(TokenType.SUB, target, right);
            }
            case MUL -> {
                throw new UnsupportedOperationException("MUL operator is not supported in this code generator.");
            }
            case DIV -> {
                throw new UnsupportedOperationException("DIV operator is not supported in this code generator.");
            }
            case AND -> {
                instrf(TokenType.AND, target, right);
            }
            case OR -> {
                instrf(TokenType.OR, target, right);
            }
            case XOR -> {
                instrf(TokenType.XOR, target, right);
            }
            default -> {
                throw new UnsupportedOperationException("Unsupported binary operator: " + op);
            }
        }
    }

    @Override
    public Void visit(Call call) {
        instrf(TokenType.LCALL, call.getCallee().accept(this));
        return null;
    }

    @Override
    public Void visit(Unary unary) {

        var operand = unary.getOperand().accept(this);
        var target = unary.getTarget().accept(this);

        if(!unary.getOperand().equals(unary.getTarget())){
            instrf(TokenType.MOV, operand, target);
        }

        switch(unary.getOperator()){
            case NEGATE -> {
                throw new UnsupportedOperationException("NEGATE operator is not supported in this code generator.");
            }
            case NOT -> {
                instrf(TokenType.NOT, target);
            }
            case DEREFERENCE -> {
                throw new UnsupportedOperationException("DEREFERENCE operator is not supported in this code generator.");
            }
            case ADDRESS_OF -> {
                throw new UnsupportedOperationException("ADDRESS_OF operator is not supported in this code generator.");
            }
            case INCREMENT -> {
                instrf(TokenType.INC, target);
            }
            case DECREMENT -> {
                instrf(TokenType.DEC, target);
            }
        }
        return null;
    }

    @Override
    public Void visit(Push push) {
        instrf(TokenType.PUSH, push.getArg().accept(this));
        return null;
    }

    @Override
    public Argument visit(ImmediateOperand immediateOperand) {
        int value = immediateOperand.getValue();
        return IntUtils.inByteRange(value) ? CodeGenUtils.immByte(value) : CodeGenUtils.immWord(value);
    }

    @Override
    public Argument visit(VirtualRegister vr) {
        Register assignedPhysicalRegister = vr.getAssignedPhysicalRegister();
        if(assignedPhysicalRegister.isCompound()){
            Register[] components = assignedPhysicalRegister.getComponents();
            var high = components[0];
            var low = components[1];
            return new Composite(CodeGenUtils.reg(high), CodeGenUtils.reg(low));
        }else{
            return CodeGenUtils.reg(assignedPhysicalRegister);
        }
    }

    @Override
    public Argument visit(Location location) {

        if(location.getSymbol().isParameter()){
            return getUnit().getParameterOperandMapping().get(location.getSymbol().getName()).accept(this);
        }else if(location.getSymbol().isStatic()){
            String asmName = location.getSymbol().getAsmName();
            return CodeGenUtils.deref(CodeGenUtils.labelRef(asmName));
        }else{
            Integer localOffset = getUnit().getFrameInfo().localOffsets().get(location.getSymbol().getName());
            if(localOffset != null){
                return new Dereference(
                        new RegisterOffset(
                                new com.lnc.assembler.parser.argument.Register(Token.__internal(TokenType.BP, "BP")),
                                Token.__internal(TokenType.PLUS, "+"),
                                new Byte(Token.__internal(TokenType.INTEGER, localOffset)
                                )
                        ));
            }else{
                // global symbol
                return CodeGenUtils.labelRef(location.getSymbol().getName());
            }
        }
    }


    @Override
    public Argument visit(AddressOf addressOf) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Argument visit(StructMemberAccess structMemberAccess) {
        throw new UnsupportedOperationException("StructMemberAccess is not supported in this code generator.");
    }

    @Override
    public Argument visit(ArrayElementAccess arrayElementAccess) {
        throw new UnsupportedOperationException("ArrayElementAccess is not supported in this code generator.");
    }

    @Override
    public Argument visit(StackFrameOperand stackFrameOperand) {
        Token opToken = stackFrameOperand.getOperandType() == StackFrameOperand.OperandType.LOCAL ?
                Token.__internal(TokenType.PLUS, "+") :
                Token.__internal(TokenType.MINUS, "-");
        return new Dereference(
                new RegisterOffset(
                        new com.lnc.assembler.parser.argument.Register(Token.__internal(TokenType.BP, "BP")),
                        opToken,
                        new Byte(Token.__internal(TokenType.INTEGER, stackFrameOperand.getOffset()))
                )
        );
    }

    @Override
    protected void visit(IRBlock block) {
        label(block.toString());
        super.visit(block);
    }

    @Override
    public void visit(IRUnit unit) {
        unitLabel();

        // visit the function body
        super.visit(unit);

        label("_ret");
        instrf(TokenType.RET);
    }

    private void dataPageVariable(String asmName, int size) {
        label(asmName);
        currentOutput.append(EncodedData.of(new byte[size]));
    }

    private void label(String lexeme) {
        currentOutput.addLabel(lexeme);
    }


    private void unitLabel() {
        currentOutput.addUnitLabel();
    }


    private void instrf(TokenType opcode, Argument... args) {
        currentOutput.append(CodeGenUtils.instr(opcode, args));
    }

}
