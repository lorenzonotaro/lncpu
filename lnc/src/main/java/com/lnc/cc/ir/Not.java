package com.lnc.cc.ir;

public class Not extends IRInstruction {
    private final IROperand operand;

    public Not(IROperand operand) {
        this.operand = operand;
    }
    public IROperand getOperand() {
        return operand;
    }

    @Override
    public String toString() {
        return "not " + operand;
    }
}
