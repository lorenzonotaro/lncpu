package com.lnc.cc.ir;

import com.lnc.cc.ast.BinaryExpression;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;

public class Bin extends IRInstruction {
    public final IROperand left;
    public final IROperand right;
    private final BinaryExpression.Operator operator;

    public Bin(IROperand left, IROperand right, BinaryExpression.Operator operator) {
        super();
        this.left = left;
        this.right = right;
        this.operator = operator;

        if(left.type == IROperand.Type.VIRTUAL_REGISTER){
            ((VirtualRegister)left).checkReleased();
        }

        if(right.type == IROperand.Type.VIRTUAL_REGISTER){
            ((VirtualRegister)right).checkReleased();
        }

        if(left instanceof ReferencableIROperand rop){
            rop.addRead(this);
        }

        if(right instanceof ReferencableIROperand rop) {
            rop.addRead(this);
        }

        if(operator != BinaryExpression.Operator.ADD && operator != BinaryExpression.Operator.SUB && operator != BinaryExpression.Operator.AND && operator != BinaryExpression.Operator.OR && operator != BinaryExpression.Operator.XOR){
            throw new RuntimeException("invalid operator for Bin: %s".formatted(operator));
        }

    }

    @Override
    public String toString() {
        return String.format("%s %s, %s", this.operator.toString().toLowerCase(), this.left, this.right);
    }

    @Override
    public <E> E accept(IIRVisitor<E> visitor) {
        return visitor.accept(this);
    }

    public BinaryExpression.Operator getOperator() {
        return operator;
    }
}
