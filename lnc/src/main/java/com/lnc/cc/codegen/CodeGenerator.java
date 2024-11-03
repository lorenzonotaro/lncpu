package com.lnc.cc.codegen;

import com.lnc.assembler.common.LinkMode;
import com.lnc.assembler.common.SectionInfo;
import com.lnc.assembler.linker.LinkTarget;
import com.lnc.cc.ir.*;
import com.lnc.cc.optimization.LinearIRUnit;
import com.lnc.cc.types.FunctionType;
import com.lnc.cc.types.TypeSpecifier;

import java.util.Comparator;
import java.util.List;

public class CodeGenerator implements ILinearIRVisitor<Void> {

    private final IR ir;
    private final StringBuilder lnccode = new StringBuilder(), lndataSection = new StringBuilder();
    private GraphColoringRegisterAllocator registerAllocator;
    private LinearIRUnit currentUnit;

    public CodeGenerator(IR ir) {
        this.ir = ir;
    }

    public void generate() {


        lndataSection.append(".section LNCDATA\n\n");

        for (var entry : ir.symbolTable().getSymbols().values()) {
            var type = entry.getType();
            if(type.type != TypeSpecifier.Type.FUNCTION)
                dataPageVariable(entry.getFlatSymbolName(), type.size());
        }


        lnccode.append(".section LNCCODE\n\n");

        for (IRUnit unit : ir.units()) {

            if(unit.getFunctionDeclaration().isForwardDeclaration())
                continue;

            LinearIRUnit linearIRUnit = new LinearIRUnit(unit);
            linearIRUnit.linearize();

            currentUnit = linearIRUnit;

            registerAllocator = new GraphColoringRegisterAllocator(linearIRUnit);

            registerAllocator.allocate();

            generate(linearIRUnit);

            if (linearIRUnit.size() == 0) {
                instructionf("ret");
            }
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

        instructionf("dec %s", dec.getOperand().asm());

        return null;
    }

    @Override
    public Void accept(Inc inc) {

        instructionf("inc %s", inc.getOperand().asm());

        return null;
    }

    @Override
    public Void accept(Jle jle) {

        instructionf("cmp %s, %s", jle.left.asm(), jle.right.asm());
        instructionf("jn %s", jle.getTarget());
        instructionf("jz %s", jle.getTarget());

        return null;
    }

    @Override
    public Void accept(Jeq je) {

        instructionf("cmp %s, %s", je.left.asm(), je.right.asm());
        instructionf("jz %s", je.getTarget());

        return null;
    }

    @Override
    public Void accept(Jlt jle) {

        instructionf("cmp %s, %s", jle.left.asm(), jle.right.asm());
        instructionf("jn %s", jle.getTarget());

        return null;
    }

    @Override
    public Void accept(Load load) {

        instructionf("mov %s, %s", load.getOperand().asm(), load.getVR().asm());

        return null;
    }

    @Override
    public Void accept(Move move) {

        instructionf("mov %s, %s", move.getSource().asm(), move.getDest().asm());

        return null;
    }

    @Override
    public Void accept(Store store) {

        instructionf("mov %s, %s", store.getValue().asm(), store.getDest().asm());

        return null;
    }

    @Override
    public Void accept(Ret ret) {

        if(requiresRegisterStacking(currentUnit)){
            instructionf("goto _ret");
        }else{
            instructionf("ret");
        }

        return null;
    }

    @Override
    public Void accept(Neg neg) {

        instructionf("neg %s", neg.getOperand().asm());

        return null;
    }

    @Override
    public Void accept(Not not) {

        instructionf("not %s", not.getOperand().asm());

        return null;
    }

    @Override
    public Void accept(Bin bin) {

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

                instructionf("mov %s, %s", arg.asm(), fun.unit.getSymbolTable().parameters[i].getFlatSymbolName());

            }

            instructionf("lcall %s", loc.asm());

        }

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
