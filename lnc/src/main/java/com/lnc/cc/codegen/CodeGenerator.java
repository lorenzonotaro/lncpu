package com.lnc.cc.codegen;

import com.lnc.assembler.common.LinkMode;
import com.lnc.assembler.common.SectionInfo;
import com.lnc.assembler.linker.LinkTarget;
import com.lnc.assembler.parser.EncodedData;
import com.lnc.assembler.parser.Instruction;
import com.lnc.assembler.parser.argument.*;
import com.lnc.assembler.parser.argument.Byte;
import com.lnc.cc.ir.operands.StructMemberAccess;
import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.*;
import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.IntUtils;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

import java.util.*;
import java.util.stream.Stream;

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

            visit(unit);
        }

        return outputs;
    }

    private void outputDataSection() {
        var dataOutput = new CompilerOutput(null, new SectionInfo("LNCDATA", 0x2000, LinkTarget.RAM, LinkMode.FIT, false, true, false));

        for(var entry : ir.symbolTable().getSymbols().values()){
            var type = entry.getType();
            if(type.type != TypeSpecifier.Type.FUNCTION){
                dataPageVariable(entry.getAsmName(), type.allocSize());
            }
        }
    }

    @Override
    public Void visit(Goto aGoto) {
        var target = labelRef(aGoto.getTarget());

        instrf(TokenType.GOTO, target);

        return null;
    }

    @Override
    public Void visit(CondJump condJump) {

        var left = condJump.getLeft().accept(this);
        var right = condJump.getRight().accept(this);

        final var target = labelRef(condJump.getTarget());
        final var falseTarget = labelRef(condJump.getFalseTarget());

        instrf(TokenType.CMP, left, right);


        switch(condJump.getCond()){
            case EQ -> {
                instrf(TokenType.JZ, target);

                enqueue(condJump.getFalseTarget());
                enqueue(condJump.getTarget());
            }
            case NE -> {
                instrf(TokenType.JZ, falseTarget);

                enqueue(condJump.getTarget());
                enqueue(condJump.getFalseTarget());
            }
            case LT -> {
                instrf(TokenType.JC, target);

                enqueue(condJump.getFalseTarget());
                enqueue(condJump.getTarget());
            }
            case LE -> {
                instrf(TokenType.JZ, target);
                instrf(TokenType.JZ, target);

                enqueue(condJump.getFalseTarget());
                enqueue(condJump.getTarget());
            }
            case GT -> {
                instrf(TokenType.JC, falseTarget);

                enqueue(condJump.getTarget());
                enqueue(condJump.getFalseTarget());
            }
            case GE -> {
                instrf(TokenType.JC, falseTarget);
                instrf(TokenType.JC, falseTarget);

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

        var target = bin.getTarget().accept(this);
        var left = bin.getLeft().accept(this);
        var right = bin.getRight().accept(this);

        if(!bin.getTarget().equals(bin.getLeft())){
            instrf(TokenType.MOV, left, target);
        }

        switch(bin.getOperator()){
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
                throw new UnsupportedOperationException("Unsupported binary operator: " + bin.getOperator());
            }
        }
        return null;
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
        return IntUtils.inByteRange(value) ? immByte(value) : immWord(value);
    }

    private Argument immByte(int value) {
        return new Byte(Token.__internal(TokenType.INTEGER, value));
    }

    private Argument immWord(int value) {
        return new Word(Token.__internal(TokenType.INTEGER, value));
    }

    @Override
    public Argument visit(VirtualRegister vr) {
        Register assignedPhysicalRegister = vr.getAssignedPhysicalRegister();
        if(assignedPhysicalRegister.isCompound()){
            Register[] components = assignedPhysicalRegister.getComponents();
            var high = components[0];
            var low = components[1];
            return new Composite(reg(high), reg(low));
        }else{
            return reg(assignedPhysicalRegister);
        }
    }

    private Argument reg(Register physReg) {
        return new com.lnc.assembler.parser.argument.Register(Token.__internal(physReg.getTokenType(), physReg.toString()));
    }

    @Override
    public Argument visit(Location location) {
        String asmName = location.getSymbol().getAsmName();
        return labelRef(asmName);
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
        return new Dereference(
                new Composite(
                        new com.lnc.assembler.parser.argument.Register(Token.__internal(TokenType.BP, "BP")),
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
        label(unit.getFunctionDeclaration().name.lexeme);

        int totalStackFrameSize = unit.getTotalStackFrameSize();

        if(totalStackFrameSize > 0) {
            instrf(TokenType.PUSH, reg(TokenType.BP));
            instrf(TokenType.MOV, reg(TokenType.SP), reg(TokenType.BP));
            instrf(TokenType.SUB, reg(TokenType.SP), immByte(totalStackFrameSize));
        }

        // preserve registers
        var registers = unit.getUsedRegisters().stream()
                .filter(r -> !CallingConvention.returnRegisterFor(unit.getFunctionDeclaration().declarator.typeSpecifier()).getRegisters().contains(r))
                .flatMap(r -> r.isCompound() ? Arrays.stream(r.getComponents()) : Stream.of(r))
                .toArray(Register[]::new);

        for(Register reg : registers) {
            instrf(TokenType.PUSH, reg(reg));
        }

        // visit the function body
        super.visit(unit);

        label("_ret");
        // restore registers
        for(int i = registers.length - 1; i >= 0; i--) {
            instrf(TokenType.POP, reg(registers[i]));
        }

        if(totalStackFrameSize > 0) {
            // restore stack pointer and base pointer
            instrf(TokenType.POP, reg(TokenType.BP));
            instrf(TokenType.SUB, reg(TokenType.SP), immByte(totalStackFrameSize));
        }


        if(totalStackFrameSize > 0) {
            // return to caller
            instrf(TokenType.RET, immByte(totalStackFrameSize));
        } else {
            // no stack frame, just return
            instrf(TokenType.RET);

        }
    }

    private Argument reg(TokenType regId) {
        return new com.lnc.assembler.parser.argument.Register(Token.__internal(regId, regId.toString()));
    }

    private void dataPageVariable(String asmName, int size) {
        label(asmName);
        currentOutput.append(EncodedData.of(new byte[size]));
    }

    private void label(String lexeme) {
        currentOutput.addLabel(lexeme);
    }

    private void instrf(TokenType opcode, Argument... args) {
        currentOutput.append(new Instruction(Token.__internal(opcode, opcode.toString()), args));
    }

    private LabelRef labelRef(IRBlock target) {
        return new LabelRef(Token.__internal(TokenType.IDENTIFIER, target.toString()));
    }

    private Argument labelRef(String asmName) {
        return new LabelRef(Token.__internal(TokenType.IDENTIFIER, asmName));
    }

}
