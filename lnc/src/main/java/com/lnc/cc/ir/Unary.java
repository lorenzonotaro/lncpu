package com.lnc.cc.ir;

import com.lnc.cc.ast.UnaryExpression;
import com.lnc.cc.ir.operands.IROperand;

public class Unary extends IRInstruction {

    private final IROperand target;
    private final IROperand operand;
    private final UnaryExpression.Operator operator;

    public Unary(IROperand target, IROperand operand, UnaryExpression.Operator operator) {
        this.target = target;
        this.operand = operand;
        this.operator = operator;
    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.accept(this);
    }

    @Override
    public String toString() {
        return String.format("%s <- %s %s", target, operator, operand);
    }
}
