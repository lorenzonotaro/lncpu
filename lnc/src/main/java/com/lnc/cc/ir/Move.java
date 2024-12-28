package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;
import com.lnc.cc.ir.operands.VirtualRegister;

public class Move extends IRInstruction {
    private final IROperand source;
    private final IROperand dest;

    public Move(IROperand source, IROperand dest) {
        super();
        this.source = source;
        this.dest = dest;

        if(source.type == IROperand.Type.VIRTUAL_REGISTER){
            ((VirtualRegister)source).checkReleased();
        }

        if(dest.type == IROperand.Type.VIRTUAL_REGISTER){
            ((VirtualRegister)dest).checkReleased();
        }

        if(source instanceof IReferenceable rop){
            rop.addRead(this);
        }

        if(dest instanceof IReferenceable rop) {
            rop.addWrite(this);
        }
    }


    public IROperand getSource() {
        return source;
    }

    public IROperand getDest() {
        return dest;
    }

    @Override
    public String toString() {
        return String.format("mov %s, %s", source, dest);
    }

    @Override
    public <E> E accept(IIRInstructionVisitor<E> visitor) {
        return visitor.accept(this);
    }
}
