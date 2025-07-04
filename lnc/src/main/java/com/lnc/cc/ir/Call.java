package com.lnc.cc.ir;

import com.lnc.cc.codegen.RegisterClass;
import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;

public class Call extends IRInstruction {
    private final IROperand returnTarget;

    private final IROperand callee;
    private final IROperand[] arguments;

    public Call(IROperand returnTarget, IROperand callee, IROperand[] arguments) {
        super();
        this.returnTarget = returnTarget;
        this.callee = callee;
        this.arguments = arguments;
    }

    public IROperand getReturnTarget() {
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

        if (returnTarget != null && returnTarget instanceof VirtualRegister) {
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
        return visitor.accept(this);
    }
}
