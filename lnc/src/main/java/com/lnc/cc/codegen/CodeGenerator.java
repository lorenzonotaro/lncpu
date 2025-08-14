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

        outputConstSection();

        for(IRUnit unit : ir.units()){

            if(unit.getFunctionDeclaration().isForwardDeclaration()){
                // Skip forward declarations
                continue;
            }

            currentOutput = new CompilerOutput(unit, new SectionInfo("LNC_" + unit.getFunctionDeclaration().name.lexeme, -1, LinkTarget.ROM, LinkMode.PAGE_FIT, false, false, false));

            GraphColoringRegisterAllocator.AllocationInfo allocationInfo = GraphColoringRegisterAllocator.run(unit);

            PostRAOptimizer.run(unit, allocationInfo);

            unit.compileFrameInfo();

            visit(unit);

            outputs.add(currentOutput);

            super.reset();
        }

        return outputs;
    }

    private void outputConstSection() {
        this.currentOutput = new CompilerOutput(null, new SectionInfo("LNC_CONST", -1, LinkTarget.ROM, LinkMode.PAGE_FIT, false, false, false));

        for(var constSymbol : ir.symbolTable().getConstants().values()){
            label(constSymbol.getAsmName());
            currentOutput.append(constSymbol.getValue());
        }

        outputs.add(currentOutput);
    }

    private void outputDataSection() {

        this.currentOutput = new CompilerOutput(null, new SectionInfo("LNCDATA", 0x2000, LinkTarget.RAM, LinkMode.FIXED, false, true, false));

        for(var entry : ir.symbolTable().getSymbols().values()){
            var type = entry.getTypeSpecifier();
            if(type.type != TypeSpecifier.Type.FUNCTION && entry.isStatic() && !entry.getTypeQualifier().isExtern()){
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
        IRBlock falseTarget = condJump.getFalseTarget();
        IRBlock continueTo = condJump.getContinueTo();

        if(continueTo != null){
            enqueue(continueTo);
        }

        instrf(TokenType.CMP, left, right);

        // Emit minimal conditional jumps and schedule targets using LIFO (push in reverse order)
        switch(condJump.getCond()){
            case EQ -> {
                // Z == 1 => true; fallthrough to false
                instrf(TokenType.JZ, CodeGenUtils.labelRef(trueTarget));

                enqueue(trueTarget);   // push first
                enqueue(falseTarget);  // so false is visited next (fallthrough)

                instrf(TokenType.GOTO, CodeGenUtils.labelRef(falseTarget));
            }
            case NE -> {
                // complement via JZ to false; fallthrough to true
                instrf(TokenType.JZ, CodeGenUtils.labelRef(falseTarget));
                enqueue(falseTarget);
                enqueue(trueTarget);

                instrf(TokenType.GOTO, CodeGenUtils.labelRef(trueTarget));
            }
            case LT -> {
                // C == 1 => true; fallthrough to false
                instrf(TokenType.JC, CodeGenUtils.labelRef(trueTarget));
                enqueue(trueTarget);
                enqueue(falseTarget);

                instrf(TokenType.GOTO, CodeGenUtils.labelRef(falseTarget));
            }
            case GE -> {
                // complement via JC to false; fallthrough to true
                instrf(TokenType.JC, CodeGenUtils.labelRef(falseTarget));
                enqueue(falseTarget);
                enqueue(trueTarget);

                instrf(TokenType.GOTO, CodeGenUtils.labelRef(trueTarget));
            }
            case LE -> {
                // C == 1 or Z == 1 => true; fallthrough to false
                instrf(TokenType.JC, CodeGenUtils.labelRef(trueTarget));
                instrf(TokenType.JZ, CodeGenUtils.labelRef(trueTarget));
                enqueue(trueTarget);
                enqueue(falseTarget);

                instrf(TokenType.GOTO, CodeGenUtils.labelRef(falseTarget));
            }
            case GT -> {
                // complement via (JC || JZ) to false; fallthrough to true
                instrf(TokenType.JC, CodeGenUtils.labelRef(falseTarget));
                instrf(TokenType.JZ, CodeGenUtils.labelRef(falseTarget));
                enqueue(falseTarget);
                enqueue(trueTarget);

                instrf(TokenType.GOTO, CodeGenUtils.labelRef(trueTarget));
            }
        }

        return null;
    }


    @Override
    public Void visit(Move move) {

        int destSize = move.getDest().getTypeSpecifier().allocSize();
        int srcSize = move.getSource().getTypeSpecifier().allocSize();

        var target = move.getDest().accept(this);
        var source = move.getSource().accept(this);

        if(destSize != srcSize){
            throw new IllegalArgumentException("Mov argument size mismatch: " +
                    destSize + " vs " +
                    srcSize);
        }

        if(destSize == 1){
            instrf(TokenType.MOV, source, target);
        }else{
            var splitSource = CodeGenUtils.splitWord(source);
            var splitTarget = CodeGenUtils.splitWord(target);

            instrf(TokenType.MOV, splitSource[0], splitTarget[0]);
            instrf(TokenType.MOV, splitSource[1], splitTarget[1]);
        }


        return null;
    }

    @Override
    public Void visit(Ret ret) {
        instrf(TokenType.GOTO, new LabelRef(Token.__internal(TokenType.IDENTIFIER, "_ret")));
        return null;
    }

    @Override
    public Void visit(Bin bin) {

        var dest = bin.getDest().accept(this);
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
    public Void visit(Push push) {
        instrf(TokenType.PUSH, push.getArg().accept(this));
        return null;
    }

    @Override
    public Void visit(Unary unary) {
        var target = unary.getTarget().accept(this);
        var operand = unary.getOperand().accept(this);

        var targetStr = target.toString();
        var operandStr = operand.toString();

        switch(unary.getOperator()){
            case NEGATE -> {
                // for negate, we can negate the value and add 1
                instrf(TokenType.NOT, target);
                instrf(TokenType.INC, target);
            }
            case NOT -> {
                instrf(TokenType.NOT, target);
            }
            case INCREMENT -> {
                instrf(TokenType.INC, target);
            }
            case DECREMENT -> {
                instrf(TokenType.DEC, target);
            }
            default -> {
                // The IR lowering pass should have resolved the other unary operators
                throw new UnsupportedOperationException("Unary operator is unsupported by this code generator: " + unary.getOperator());
            }
        }

        return null;
    }

    @Override
    public Argument visit(ImmediateOperand immediateOperand) {
        int value = immediateOperand.getValue();
        return IntUtils.inByteRange(value) ? CodeGenUtils.immByte(value) : CodeGenUtils.immWord(value);
    }

    @Override
    public Argument visit(VirtualRegister vr) {
        return visitRegister(vr);
    }

    private Argument visitRegister(VirtualRegister vr) {
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
        // by this time, if this is still a location, it is either a static local or a global variable
        return CodeGenUtils.labelRef(location.getSymbol().getName());
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
    public Argument visit(Deref deref) {
        return new Dereference(deref.getTarget().accept(this));
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
