package com.lnc.cc.ir.operands;

import com.lnc.cc.codegen.RegisterClass;
import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.ir.IRInstruction;
import com.lnc.cc.types.TypeSpecifier;

import java.util.Set;

public class RegisterDereference extends IROperand implements IReferenceable {

    private final VirtualRegister reg;

    public final TypeSpecifier dereferencedType;
    private int staticOffset;

    public RegisterDereference(VirtualRegister reg, TypeSpecifier dereferencedType, int staticOffset){
        super(Type.REGISTER_DEREFERENCE);
        this.dereferencedType = dereferencedType;
        if(reg.getRegisterClass() == RegisterClass.ANY){
            reg.setRegisterClass(RegisterClass.INDEX);
        }

        reg.checkReleased();
        this.reg = reg;
        this.staticOffset = staticOffset;
    }

    public void addToOffset(int offset){
        reg.checkReleased();
        staticOffset += offset;
    }

    @Override
    public String asm() {
        return "[" + reg.asm() + "]";
    }

    @Override
    public <T> T accept(IIROperandVisitor<T> visitor) {
        return visitor.accept(this);
    }

    @Override
    public String toString() {
        return reg.toString();
    }


    public VirtualRegister getReg() {
        return reg;
    }

    @Override
    public Set<IRInstruction> getReads() {
        return reg.getReads();
    }

    @Override
    public Set<IRInstruction> getWrites() {
        return reg.getWrites();
    }

    @Override
    public void addRead(IRInstruction instruction) {
        reg.addRead(instruction);
    }

    @Override
    public void addWrite(IRInstruction instruction) {
        reg.addWrite(instruction);
    }

    @Override
    public boolean removeRead(IRInstruction instruction) {
        return reg.removeRead(instruction);
    }

    @Override
    public boolean removeWrite(IRInstruction instruction) {
        return reg.removeWrite(instruction);
    }

    public int getStaticOffset() {
        return staticOffset;
    }
}
