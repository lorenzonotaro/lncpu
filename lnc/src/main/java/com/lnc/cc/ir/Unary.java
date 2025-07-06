package com.lnc.cc.ir;

import com.lnc.cc.ast.UnaryExpression;
import com.lnc.cc.ir.operands.IROperand;

public class Unary extends IRInstruction {

    private final IROperand target;
    private IROperand operand;
    private final UnaryExpression.Operator operator;
    private final UnaryExpression.UnaryPosition unaryPosition;

    public Unary(IROperand target, IROperand operand, UnaryExpression.Operator operator, UnaryExpression.UnaryPosition unaryPosition) {
        this.target = target;
        this.operand = operand;
        this.operator = operator;
        this.unaryPosition = unaryPosition;
    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format("%s <- %s %s", target, operator, operand);
    }

    public UnaryExpression.UnaryPosition getUnaryPosition() {
        return unaryPosition;
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
}
