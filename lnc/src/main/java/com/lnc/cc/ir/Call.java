package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;

public class Call extends IRInstruction {
    private VirtualRegister returnTarget;

    private final IROperand callee;
    private IROperand[] arguments;

    public Call(VirtualRegister returnTarget, IROperand callee, IROperand[] arguments) {
        super();
        this.returnTarget = returnTarget;
        this.callee = callee;
        this.arguments = arguments;
    }

    public VirtualRegister getReturnTarget() {
        return returnTarget;
    }

    public IROperand[] getArguments() {
        return arguments;
    }

    public IROperand getCallee() {
        return callee;
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (returnTarget != null) {
            sb.append(returnTarget).append(" <- ");
        }

        sb.append("call ");

        sb.append(callee);

        if( arguments != null && arguments.length > 0) {
            sb.append("(");
            for (int i = 0; i < arguments.length; i++) {
                sb.append(", ");
                sb.append(arguments[i]);

            }
            sb.append(")");
        }
        return sb.toString();
    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.visit(this);
    }

    public void setReturnTarget(IROperand returnTarget) {
        if (!(returnTarget instanceof VirtualRegister)) {
            throw new IllegalArgumentException("Return target must be a VirtualRegister");
        }
        this.returnTarget = (VirtualRegister) returnTarget;
    }

    public void setArguments(IROperand[] array) {
        if (array == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        this.arguments = array;
    }
}
