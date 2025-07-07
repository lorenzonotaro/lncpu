package com.lnc.cc.codegen;

import com.lnc.assembler.common.LinkMode;
import com.lnc.assembler.common.SectionInfo;
import com.lnc.assembler.linker.LinkTarget;
import com.lnc.cc.ir.operands.StructMemberAccess;
import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.*;
import com.lnc.cc.types.TypeSpecifier;

import java.util.*;
import java.util.stream.Stream;

public class CodeGenerator extends GraphicalIRVisitor implements IIROperandVisitor<String>{
    private final IR ir;
    private IRUnit currentUnit;

    private final StringBuilder code = new StringBuilder();
    private final StringBuilder dataSection = new StringBuilder();

    public CodeGenerator(IR ir){
        this.ir = ir;
    }

    public List<CompilerOutput> run(){

        dataSection.append(".section LNCDATA\n");

        for(var entry : ir.symbolTable().getSymbols().values()){
            var type = entry.getType();
            if(type.type != TypeSpecifier.Type.FUNCTION){
                dataPageVariable(entry.getAsmName(), type.allocSize());
            }
        }

        code.append(".section LNCCODE\n\n");

        for(IRUnit unit : ir.units()){

            if(unit.getFunctionDeclaration().isForwardDeclaration()){
                // Skip forward declarations
                continue;
            }

            currentUnit = unit;

            GraphColoringRegisterAllocator.run(unit);

            visit(unit);

            code.append("\n\n");
        }

        return List.of(
                new CompilerOutput(
                        code.toString(), new SectionInfo("LNCCODE", -1, LinkTarget.ROM, LinkMode.PAGE_FIT, false, false, false)),
                new CompilerOutput(
                        dataSection.toString(), new SectionInfo("LNCDATA", 0x2000, LinkTarget.RAM, LinkMode.FIXED, false, true, false))
        );
    }

    private void dataPageVariable(String asmName, int size) {
        if(size <= 0) {
            return; // Skip zero-sized variables
        }
        dataSection.append(String.format("%s:\n\t.res %d\n", asmName, size));
    }

    @Override
    public Void visit(Goto aGoto) {
        var target = aGoto.getTarget().toString();

        instrf("goto %s", target);

        return null;
    }

    @Override
    public Void visit(CondJump condJump) {

        var left = condJump.getLeft().accept(this);
        var right = condJump.getRight().accept(this);

        final String target = condJump.getTarget().toString();
        final String falseTarget = condJump.getFalseTarget().toString();

        instrf("cmp %s, %s", left, right);


        switch(condJump.getCond()){
            case EQ -> {
                instrf("je %s", target);

                enqueue(condJump.getFalseTarget());
                enqueue(condJump.getTarget());
            }
            case NE -> {
                instrf("je %s", falseTarget);

                enqueue(condJump.getTarget());
                enqueue(condJump.getFalseTarget());
            }
            case LT -> {
                instrf("jn %s", target);

                enqueue(condJump.getFalseTarget());
                enqueue(condJump.getTarget());
            }
            case LE -> {
                instrf("jn %s", target);
                instrf("je %s", target);

                enqueue(condJump.getFalseTarget());
                enqueue(condJump.getTarget());
            }
            case GT -> {
                instrf("jn %s", falseTarget);

                enqueue(condJump.getTarget());
                enqueue(condJump.getFalseTarget());
            }
            case GE -> {
                instrf("jn %s", falseTarget);
                instrf("je %s", falseTarget);

                enqueue(condJump.getTarget());
                enqueue(condJump.getFalseTarget());
            }
        }

        return null;
    }

    @Override
    public Void visit(Load load) {

        var target = load.getDest().accept(this);
        var source = load.getSrc().accept(this);

        instrf("mov %s, %s", source, target);

        return null;
    }

    @Override
    public Void visit(Move move) {

        var target = move.getDest().accept(this);
        var source = move.getSource().accept(this);

        instrf("mov %s, %s", source, target);

        return null;
    }

    @Override
    public Void visit(Store store) {
        var target = store.getDest().accept(this);
        var value = store.getValue().accept(this);

        instrf("mov %s, %s", value, target);

        return null;
    }

    @Override
    public Void visit(Ret ret) {
        instrf("goto _ret");
        return null;
    }

    @Override
    public Void visit(Bin bin) {

        var target = bin.getTarget().accept(this);
        var left = bin.getLeft().accept(this);
        var right = bin.getRight().accept(this);

        if(!target.equals(left)){
            instrf("mov %s, %s", left, target);
        }

        switch(bin.getOperator()){
            case ADD -> {
                instrf("add %s, %s", target, right);
            }
            case SUB -> {
                instrf("sub %s, %s", target, right);
            }
            case MUL -> {
                throw new UnsupportedOperationException("MUL operator is not supported in this code generator.");
            }
            case DIV -> {
                throw new UnsupportedOperationException("DIV operator is not supported in this code generator.");
            }
            case AND -> {
                instrf("and %s, %s", target, right);
            }
            case OR -> {
                instrf("or %s, %s", target, right);
            }
            case XOR -> {
                instrf("xor %s, %s", target, right);
            }
            default -> {
                throw new UnsupportedOperationException("Unsupported binary operator: " + bin.getOperator());
            }
        }
        return null;
    }

    @Override
    public Void visit(Call call) {
        instrf("call %s", call.getCallee().accept(this));
        return null;
    }

    @Override
    public Void visit(Unary unary) {

        var operand = unary.getOperand().accept(this);
        var target = unary.getTarget().accept(this);

        if(!operand.equals(target)){
            instrf("mov %s, %s", operand, target);
        }

        switch(unary.getOperator()){
            case NEGATE -> {
                throw new UnsupportedOperationException("NEGATE operator is not supported in this code generator.");
            }
            case NOT -> {
                instrf("not %s", target);
            }
            case DEREFERENCE -> {
                throw new UnsupportedOperationException("DEREFERENCE operator is not supported in this code generator.");
            }
            case ADDRESS_OF -> {
                throw new UnsupportedOperationException("ADDRESS_OF operator is not supported in this code generator.");
            }
            case INCREMENT -> {
                instrf("inc %s", target);
            }
            case DECREMENT -> {
                instrf("dec %s", target);
            }
        }
        return null;
    }

    @Override
    public Void visit(Push push) {
        instrf("push %s", push.getArg().accept(this));
        return null;
    }

    @Override
    public String visit(ImmediateOperand immediateOperand) {
        return immediateOperand.toString();
    }

    @Override
    public String visit(VirtualRegister vr) {
        return vr.getAssignedPhysicalRegister().getRegName();
    }

    @Override
    public String visit(Location location) {
        return String.format("[%s]", location.getSymbol().getAsmName());
    }

    @Override
    public String visit(AddressOf addressOf) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String visit(StructMemberAccess structMemberAccess) {
        throw new UnsupportedOperationException("StructMemberAccess is not supported in this code generator.");
    }

    @Override
    public String visit(ArrayElementAccess arrayElementAccess) {
        throw new UnsupportedOperationException("ArrayElementAccess is not supported in this code generator.");
    }

    @Override
    public String visit(StackFrameOperand stackFrameOperand) {
        return String.format("[BP + %d]", stackFrameOperand.getOffset());
    }

    @Override
    protected void visit(IRBlock block) {
        label(block.toString());
        super.visit(block);
    }

    @Override
    public void visit(IRUnit unit) {
        label(unit.getFunctionDeclaration().name.lexeme);

        int totalStackFrameSize = unit.getTotalStackFrameSize();

        if(totalStackFrameSize > 0) {
            instrf("push BP");
            instrf("mov BP, SP");
            instrf("sub SP, %d", totalStackFrameSize);
        }

        // preserve registers
        var registers = unit.getUsedRegisters().stream()
                .filter(r -> !CallingConvention.returnRegisterFor(unit.getFunctionDeclaration().declarator.typeSpecifier()).getRegisters().contains(r))
                .flatMap(r -> r.isCompound() ? Arrays.stream(r.getComponents()) : Stream.of(r))
                .map(Register::toString).toArray(String[]::new);

        for(String reg : registers) {
           instrf("push %s", reg);
        }

        // visit the function body
        super.visit(unit);

        label("_ret");
        // restore registers
        for(int i = registers.length - 1; i >= 0; i--) {
            instrf("pop %s", registers[i]);
        }

        if(totalStackFrameSize > 0) {
            // restore stack pointer and base pointer
            instrf("pop BP");
            instrf("sub SP, %d", totalStackFrameSize);
        }


        instrf("ret %s", totalStackFrameSize > 0 ? String.valueOf(totalStackFrameSize) : "");

        if(totalStackFrameSize > 0){
            code.append(" ").append(totalStackFrameSize);
        }
    }

    private void label(String lexeme) {
        code.append(lexeme).append(":\n");
    }

    private void instrf(String format, Object... args) {
        code.append("\t").append(String.format(format, args)).append("\n");
    }
}
