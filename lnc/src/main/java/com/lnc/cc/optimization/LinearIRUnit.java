package com.lnc.cc.optimization;

import com.lnc.cc.ir.*;

public class LinearIRUnit {

    public final IRUnit nonLinearUnit;

    private int nextIndex = 0;
    public IRInstruction head;
    private IRInstruction current;


    public LinearIRUnit(IRUnit nonLinearUnit) {
        this.nonLinearUnit = nonLinearUnit;
        head = current = null;
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

    public int size() {
        return nextIndex;
    }

    public void append(IRInstruction instruction) {
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
}
