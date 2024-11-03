package com.lnc.cc.codegen;

import com.lnc.cc.ir.*;
import com.lnc.cc.optimization.LinearIRUnit;
import com.lnc.cc.types.FunctionType;
import com.lnc.cc.types.TypeSpecifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class CodeGenerator implements ILinearIRVisitor<Void> {

    private final IR ir;
    private final StringBuilder code = new StringBuilder();
    private final StringBuilder epilogue = new StringBuilder(), prologue = new StringBuilder();
    private GraphColoringRegisterAllocator registerAllocator;
    private LinearIRUnit currentUnit;

    public CodeGenerator(IR ir) {
        this.ir = ir;
    }

    public void generate() {
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


        label(prologue, linearIRUnit.nonLinearUnit.getFunctionDeclaration().name.lexeme);

        if(requiresRegisterStacking(currentUnit)) {
            for (var reg : registerAllocator.usedRegisters.stream().sorted(Comparator.comparingInt(Register::ordinal)).toArray()) {
                if (reg != RegisterClass.RETURN.getRegisters()[0])
                    instructionf(prologue, "push %s", reg);
            }
        }

        var instr = linearIRUnit.head;

        while (instr != null) {
            instr.accept(this);
            instr = instr.getNext();
        }

        if(requiresRegisterStacking(currentUnit)) {
            label(epilogue, "_ret");
            for (var reg : registerAllocator.usedRegisters.stream().sorted(Comparator.comparingInt(Register::ordinal).reversed()).toArray()) {
                if (reg != RegisterClass.RETURN.getRegisters()[0])
                    instructionf(epilogue, "pop %s", reg);
            }
            instructionf(epilogue, "ret");
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

            instructionf("call %s", loc.asm());

        }

        return null;
    }

    @Override
    public Void accept(Label label) {

        label(label.block.toString());

        return null;
    }

    private void instructionf(String format, Object... args) {
        instructionf(code, format, args);
    }

    private void instructionf(StringBuilder sb, String format, Object... args) {
        sb.append("    ").append(String.format(format, args)).append("\n");

    }

    private void label(String label) {
        label(code, label);
    }

    private void label(StringBuilder sb, String label) {
        sb.append(label).append(":\n");
    }

    public String getCode() {
        return prologue + code.toString() + epilogue;
    }
}
