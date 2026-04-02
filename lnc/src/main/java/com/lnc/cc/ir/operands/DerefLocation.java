package com.lnc.cc.ir.operands;

import com.lnc.cc.types.PointerType;
import com.lnc.cc.types.StorageLocation;
import com.lnc.cc.types.TypeSpecifier;

import java.util.List;

public class DerefLocation extends Location {

    private final TypeSpecifier typeSpecifier;
    private IROperand target;

    public DerefLocation(IROperand target) {
        super(LocationType.DEREF);
        TypeSpecifier typeSpecifier = target.getTypeSpecifier();
        if(typeSpecifier.type != TypeSpecifier.Type.POINTER) {
            throw new IllegalArgumentException("DerefLocation target must be of pointer type");
        }
        this.target = target;
        this.typeSpecifier = typeSpecifier;
    }

    public DerefLocation(IROperand target, TypeSpecifier typeSpecifier) {
        super(LocationType.DEREF);
        if(target.getTypeSpecifier().type != TypeSpecifier.Type.POINTER) {
            throw new IllegalArgumentException("DerefLocation target must be of pointer type");
        }
        this.target = target;
        this.typeSpecifier = typeSpecifier;
    }

    @Override
    public StorageLocation getPointerKind() {
        return ((PointerType) target.getTypeSpecifier()).getPointerKind();
    }

    @Override
    public TypeSpecifier getTypeSpecifier() {
        return typeSpecifier;
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
        if (target instanceof VirtualRegister vr) {
            return List.of(vr);
        }
        return target.getVRReads();
    }

    public void setTarget(IROperand target) {
        this.target = target;
    }
}
