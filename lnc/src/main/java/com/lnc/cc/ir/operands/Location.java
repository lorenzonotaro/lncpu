package com.lnc.cc.ir.operands;

import com.lnc.cc.ir.IIROperandVisitor;
import com.lnc.cc.types.StorageLocation;
import com.lnc.cc.types.TypeSpecifier;

public abstract class Location extends IROperand {

    public enum LocationType{
        SYMBOL, STACK_FRAME, ARRAY_INDEX, DEREF /* = dynamic derived */, STRUCT_MEMBER, STATIC_DERIVED
    }

    public final LocationType locType;

    public Location(LocationType type) {
        super(Type.LOCATION);
        this.locType = type;
    }

    @Override
    public final <T> T accept(IIROperandVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /**
    * @return the kind (near or far) of a pointer to this location
    * */
    public abstract StorageLocation getPointerKind();

    public abstract TypeSpecifier getTypeSpecifier();
}
