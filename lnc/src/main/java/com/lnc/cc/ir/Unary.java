package com.lnc.cc.ir;

import com.lnc.cc.ast.UnaryExpression;
import com.lnc.cc.ir.operands.IROperand;

import java.util.Collection;
import java.util.List;

public class Unary extends IRInstruction {

    private IROperand target;
    private IROperand operand;
    private final UnaryExpression.Operator operator;

    public Unary(IROperand target, IROperand operand, UnaryExpression.Operator operator) {
        this.target = target;
        this.operand = operand;
        this.operator = operator;
    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("%s <- %s %s", target, operator, operand);
    }

    @Override
    public Collection<IROperand> getReadOperands() {
        return List.of(operand);
    }

    @Override
    public Collection<IROperand> getWriteOperands() {
        return List.of(target);
    }

    @Override
    public void replaceOperand(IROperand oldOp, IROperand newOp) {
        if (operand.equals(oldOp)) {
            operand = newOp;
        } else if (target.equals(oldOp)) {
            target = newOp; // This line is not strictly necessary since target is final, but included for consistency
        }
    }

    public IROperand getTarget() {
        return target;
    }

    public IROperand getOperand() {
        return operand;
    }

    public void setOperand(IROperand operand) {
        this.operand = operand;
    }

    public UnaryExpression.Operator getOperator() {
        return operator;
    }

    public void setTarget(IROperand target) {
        if (target == null) {
            throw new IllegalArgumentException("Target operand cannot be null");
        }
        this.target = target;
    }
}
