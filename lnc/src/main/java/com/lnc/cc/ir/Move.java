package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;

public class Move extends IRInstruction {
    private final IROperand source;
    private final IROperand dest;

    public Move(IROperand source, IROperand dest) {
        super();
        this.source = source;
        this.dest = dest;
    }


    public IROperand getSource() {
        return source;
    }

    public IROperand getDest() {
        return dest;
    }

    @Override
    public String toString() {
        return String.format("move %s <- %s", dest, source);
    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
