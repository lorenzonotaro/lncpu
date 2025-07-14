package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;

import java.util.Collection;
import java.util.List;

public class AddressOf extends IRInstruction {
    private IROperand operand;
    private IROperand result;

    public AddressOf(IROperand operand, IROperand result) {
        this.operand = operand;
        this.result = result;
    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        // return visitor.visit(this);
        return null;
    }

    @Override
    public String toString() {
        return result.toString() + " <- &" + operand.toString();
    }

    @Override
    public Collection<IROperand> getReads() {
        return List.of(operand);
    }

    @Override
    public Collection<IROperand> getWrites() {
        return List.of(result);
    }

    @Override
    public void replaceOperand(IROperand oldOp, IROperand newOp) {
        if (operand.equals(oldOp)) {
            operand = newOp;
        } else if (result.equals(oldOp)) {
            result = newOp;
        }
    }

    public IROperand getOperand() {
        return operand;
    }

    public void setOperand(IROperand operand) {
        this.operand = operand;
    }

    public IROperand getResult() {
        return result;
    }

    public void setResult(IROperand result) {
        this.result = result;
    }
}
