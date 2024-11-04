package com.lnc.cc.optimization;

import com.lnc.cc.ir.*;

public class LinearIRUnit extends BranchingIRVisitor {

    public final IRUnit nonLinearUnit;

    private int nextIndex = 0;
    public IRInstruction head;
    private IRInstruction current;


    public LinearIRUnit(IRUnit nonLinearUnit) {
        this.nonLinearUnit = nonLinearUnit;
        head = current = null;
    }

    @Override
    public Void accept(Jle jle) {
        append(jle);
        return super.accept(jle);
    }

    @Override
    public Void accept(Jeq je) {
        append(je);
        return super.accept(je);
    }

    @Override
    public Void accept(Jlt jle) {
        append(jle);
        return super.accept(jle);
    }

    @Override
    protected void visit(IRBlock block) {

        append(new Label(block));

        super.visit(block);
    }

    @Override
    public Void accept(Goto aGoto) {
        append(aGoto);
        return null;
    }

    @Override
    public Void accept(Dec dec) {
        append(dec);
        return null;
    }

    @Override
    public Void accept(Inc inc) {
        append(inc);
        return null;
    }

    @Override
    public Void accept(Load load) {
        append(load);
        return null;
    }

    @Override
    public Void accept(Move move) {
        append(move);
        return null;
    }

    @Override
    public Void accept(Store store) {
        append(store);
        return null;
    }

    @Override
    public Void accept(Ret sub) {
        append(sub);
        return null;
    }

    @Override
    public Void accept(Neg neg) {
        append(neg);
        return null;
    }

    @Override
    public Void accept(Not not) {
        append(not);
        return null;
    }

    @Override
    public Void accept(Bin bin) {
        append(bin);
        return null;
    }

    @Override
    public Void accept(Call call) {
        append(call);
        return null;
    }

    @Override
    protected void append(IRInstruction instruction) {
        if (head == null) {
            head = instruction;
            current = instruction;
        } else {
            current.setNext(instruction);
            instruction.setPrev(current);
            current = instruction;
        }

        instruction.setIndex(nextIndex++);
    }

    public void visit() {

        System.out.println("Linear IR: " + nonLinearUnit.getFunctionDeclaration().toString());

        nonLinearUnit.getSymbolTable().visit();

        var instr = head;

        while (instr != null) {
            System.out.println((instr instanceof Label ? "" : "    ") + instr + "{" + instr.getLoopNestedLevel() + "}");
            instr = instr.getNext();
        }
    }

    public void linearize() {
        visit(nonLinearUnit.getStartBlock());
    }

    public int size() {
        return nextIndex;
    }
}
