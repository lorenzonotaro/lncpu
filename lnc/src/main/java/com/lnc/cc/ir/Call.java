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

        if(returnTarget != null && returnTarget.type == IROperand.Type.VIRTUAL_REGISTER){
            VirtualRegister target = (VirtualRegister) returnTarget;
            target.checkReleased();

            target.setRegisterClass(RegisterClass.RETURN);
        }

        if(callee.type == IROperand.Type.VIRTUAL_REGISTER){
            ((VirtualRegister)callee).checkReleased();
        }

        for (IROperand irOperand : arguments) {
            if (irOperand.type == IROperand.Type.VIRTUAL_REGISTER) {
                ((VirtualRegister) irOperand).checkReleased();
            }
        }

        if(callee instanceof ReferencableIROperand rop){
            rop.addRead(this);
        }

        for (IROperand argument : arguments) {
            if (argument instanceof ReferencableIROperand rop) {
                rop.addRead(this);
            }
        }

        if(returnTarget instanceof ReferencableIROperand rop){
            rop.addWrite(this);
        }
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
        sb.append("call ");

        if(returnTarget != null)
            sb.append(returnTarget).append(" <- ");

        sb.append(callee);

        for (int i = 0; i < arguments.length; i++) {
            sb.append(", ");
            sb.append(arguments[i]);

        }
        return sb.toString();
    }

    @Override
    public <E> E accept(IIRVisitor<E> visitor) {
        return visitor.accept(this);
    }
}
