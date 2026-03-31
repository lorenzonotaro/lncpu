package com.lnc.cc.ir.operands;

import com.lnc.cc.types.PointerType;
import com.lnc.cc.types.StorageLocation;
import com.lnc.cc.types.TypeSpecifier;

import java.util.List;

public class DerefLocation extends Location {

    private IROperand target;

    public DerefLocation(IROperand target) {
        super(LocationType.DEREF);
        if(target.getTypeSpecifier().type != TypeSpecifier.Type.POINTER) {
            throw new IllegalArgumentException("DerefLocation target must be of pointer type");
        }
        this.target = target;
    }
    @Override
    public StorageLocation getPointerKind() {
        return ((PointerType) target.getTypeSpecifier()).getPointerKind();
    }

    @Override
    public TypeSpecifier getTypeSpecifier() {
        return ((PointerType) target.getTypeSpecifier()).getBaseType();
    }

    @Override
    public String toString() {
        return "*" + target.toString();
    }

    public IROperand getTarget() {
        return target;
    }

    @Override
    public List<VirtualRegister> getVRReads() {
        return target.getVRReads();
    }

    public void setTarget(IROperand target) {
        this.target = target;
    }
}
