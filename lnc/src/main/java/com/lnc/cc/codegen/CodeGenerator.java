package com.lnc.cc.codegen;

import com.lnc.assembler.common.LinkMode;
import com.lnc.assembler.common.SectionInfo;
import com.lnc.assembler.linker.LinkTarget;
import com.lnc.cc.ir.*;
import com.lnc.cc.ir.operands.*;
import com.lnc.cc.optimization.LinearIRUnit;
import com.lnc.cc.optimization.OptimizationResult;
import com.lnc.cc.types.FunctionType;
import com.lnc.cc.types.TypeSpecifier;

import java.util.Comparator;
import java.util.List;

public class CodeGenerator implements ILinearIRVisitor<Void, Void> {

    private final OptimizationResult linearIRs;
    private final StringBuilder lnccode = new StringBuilder(), lndataSection = new StringBuilder();
    private GraphColoringRegisterAllocator registerAllocator;
    private LinearIRUnit currentUnit;

    public CodeGenerator(OptimizationResult optimizationResult) {
        this.linearIRs = optimizationResult;
    }

    public void generate() {


        lndataSection.append(".section LNCDATA\n\n");

        for (var entry : linearIRs.symbolTable().getSymbols().values()) {
            var type = entry.getType();
            if(type.type != TypeSpecifier.Type.FUNCTION)
                dataPageVariable(entry.getAsmName(), type.allocSize());
        }


        lnccode.append(".section LNCCODE\n\n");

        for (LinearIRUnit linearIRUnit : linearIRs.getLinearizedIRUnits()) {

            if(linearIRUnit.nonLinearUnit.getFunctionDeclaration().isForwardDeclaration())
                continue;

            currentUnit = linearIRUnit;

            registerAllocator = new GraphColoringRegisterAllocator(linearIRUnit);

            registerAllocator.allocate();

            generate(linearIRUnit);

            if (linearIRUnit.size() == 0) {
                instructionf("ret");
            }

            lnccode.append("\n\n");
        }
    }

    private void generate(LinearIRUnit linearIRUnit) {

        label(linearIRUnit.nonLinearUnit.getFunctionDeclaration().name.lexeme);

        if(requiresRegisterStacking(currentUnit)) {
            for (var reg : registerAllocator.usedRegisters.stream().sorted(Comparator.comparingInt(Register::ordinal)).toArray()) {
                if (reg != RegisterClass.RETURN.getRegisters()[0] || currentUnit.nonLinearUnit.getFunctionDeclaration().declarator.typeSpecifier().type == TypeSpecifier.Type.VOID)
                    instructionf("push %s", reg);
            }
        }

        var instr = linearIRUnit.head;

        while (instr != null) {
            instr.accept(this);
            instr = instr.getNext();
        }

        if(requiresRegisterStacking(currentUnit)) {
            label("_ret");
            for (var reg : registerAllocator.usedRegisters.stream().sorted(Comparator.comparingInt(Register::ordinal).reversed()).toArray()) {
                if (reg != RegisterClass.RETURN.getRegisters()[0] || currentUnit.nonLinearUnit.getFunctionDeclaration().declarator.typeSpecifier().type == TypeSpecifier.Type.VOID)
                    instructionf("pop %s", reg);
            }
            instructionf("ret");
        }
    }

    private boolean requiresRegisterStacking(LinearIRUnit currentUnit) {

        return (currentUnit.nonLinearUnit.getFunctionDeclaration().declarator.typeSpecifier().type == TypeSpecifier.Type.VOID && !registerAllocator.usedRegisters.isEmpty()) ||
                currentUnit.nonLinearUnit.getFunctionDeclaration().declarator.typeSpecifier().type != TypeSpecifier.Type.VOID && registerAllocator.usedRegisters.size() > 1;

    }


    @Override
    public Void accept(Goto aGoto) {

        instructionf("goto %s", aGoto.getTarget());

        return null;
    }

    @Override
    public Void accept(Dec dec) {

        dec.getOperand().accept(this);

        instructionf("dec %s", dec.getOperand().asm());

        return null;
    }

    @Override
    public Void accept(Inc inc) {

        inc.getOperand().accept(this);

        instructionf("inc %s", inc.getOperand().asm());

        return null;
    }

    @Override
    public Void accept(Jle jle) {

        jle.left.accept(this);
        jle.right.accept(this);

        instructionf("cmp %s, %s", jle.left.asm(), jle.right.asm());
        instructionf("jn %s", jle.getTarget());
        instructionf("jz %s", jle.getTarget());

        return null;
    }

    @Override
    public Void accept(Jeq je) {

        je.left.accept(this);

        je.right.accept(this);

        instructionf("cmp %s, %s", je.left.asm(), je.right.asm());
        instructionf("jz %s", je.getTarget());

        return null;
    }

    @Override
    public Void accept(Jlt jle) {

        jle.left.accept(this);

        jle.right.accept(this);

        instructionf("cmp %s, %s", jle.left.asm(), jle.right.asm());
        instructionf("jn %s", jle.getTarget());

        return null;
    }

    @Override
    public Void accept(Load load) {

        load.getSrc().accept(this);

        load.getDest().accept(this);

        instructionf("mov %s, %s", load.getSrc().asm(), load.getDest().asm());

        return null;
    }

    @Override
    public Void accept(Move move) {

        move.getSource().accept(this);

        move.getDest().accept(this);

        instructionf("mov %s, %s", move.getSource().asm(), move.getDest().asm());

        return null;
    }

    @Override
    public Void accept(Store store) {

        store.getDest().accept(this);

        store.getValue().accept(this);

        instructionf("mov %s, %s", store.getValue().asm(), store.getDest().asm());

        return null;
    }

    @Override
    public Void accept(Ret ret) {

        if(ret.getValue() != null) {
            ret.getValue().accept(this);
        }

        if(requiresRegisterStacking(currentUnit)){
            instructionf("goto _ret");
        }else{
            instructionf("ret");
        }

        return null;
    }

    @Override
    public Void accept(Neg neg) {

        neg.getOperand().accept(this);

        instructionf("neg %s", neg.getOperand().asm());

        return null;
    }

    @Override
    public Void accept(Not not) {

        not.getOperand().accept(this);

        instructionf("not %s", not.getOperand().asm());

        return null;
    }

    @Override
    public Void accept(Bin bin) {

        bin.left.accept(this);

        instructionf("%s %s, %s", bin.getOperator().toString().toLowerCase(), bin.left.asm(), bin.right.asm());

        return null;
    }

    @Override
    public Void accept(Call call) {

        if(call.getCallee() instanceof Location loc) {
            var fun = ((FunctionType) loc.getSymbol().getType()).functionDeclaration;

            IROperand[] arguments = call.getArguments();
            for (int i = 0; i < arguments.length; i++) {
                var arg = arguments[i];

                arg.accept(this);

                instructionf("mov %s, %s", arg.asm(), "[" + fun.unit.getSymbolTable().parameters[i].getAsmName() + "]");

            }

            instructionf("lcall %s", loc.getSymbol().getAsmName());

        }

        return null;
    }

    @Override
    public Void accept(ImmediateOperand immediateOperand) {
        return null;
    }

    @Override
    public Void accept(VirtualRegister vr) {
        return null;
    }

    @Override
    public Void accept(RegisterDereference rd) {
        if(rd.getStaticOffset() != 0){
            lnccode.append("    ").append("add ").append(rd.getReg().asm()).append(", ").append(rd.getStaticOffset() * rd.dereferencedType.allocSize()).append("\n");
        }
        return null;
    }

    @Override
    public Void accept(Location location) {
        return null;
    }

    @Override
    public Void accept(AddressOf addressOf) {
        return null;
    }

    @Override
    public Void accept(Label label) {

        label(label.block.toString());

        return null;
    }

    private void instructionf(String format, Object... args) {
        lnccode.append("    ").append(String.format(format, args)).append("\n");
    }


    private void label(String label) {
        lnccode.append(label).append(":\n");
    }

    private void dataPageVariable(String name, int size) {
        lndataSection.append(name).append(":\n");
        lndataSection.append("    .res ").append(size).append("\n");
    }

    public List<CompilerOutput> getOutput() {
        List<CompilerOutput> compilerOutputs = new java.util.ArrayList<>();
        compilerOutputs.add(new CompilerOutput(lnccode.toString(), new SectionInfo("LNCCODE", -1, LinkTarget.ROM, LinkMode.PAGE_FIT, false, false, false)));
        compilerOutputs.add(new CompilerOutput(lndataSection.toString(), new SectionInfo("LNCDATA", 0x2000, LinkTarget.RAM, LinkMode.FIXED, false, true, false)));
        return compilerOutputs;
    }
}
