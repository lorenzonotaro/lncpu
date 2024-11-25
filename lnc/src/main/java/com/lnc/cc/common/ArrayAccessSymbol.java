package com.lnc.cc.common;

import com.lnc.cc.types.ArrayType;
import com.lnc.cc.types.TypeSpecifier;

public class ArrayAccessSymbol extends AbstractSymbol{

    private final AbstractSymbol arraySymbol;
    private final int offset;
    private final ArrayType arrayType;

    public ArrayAccessSymbol(AbstractSymbol arraySymbol, int offset){
        this.arraySymbol = arraySymbol;
        var type = arraySymbol.getType();

        if(type.type != TypeSpecifier.Type.ARRAY){
            throw new IllegalArgumentException("ArrayAccessSymbol must be initialized with an array symbol");
        }

        this.arrayType = (ArrayType) type;

        this.offset = offset;
    }

    @Override
    public String getAsmName() {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(arraySymbol.getAsmName());
        int baseSize = arrayType.baseType.allocSize();

        if(offset != 0){
            sb.append(" + ").append(offset * baseSize);
        }

        return sb.append(")").toString();
    }

    @Override
    public TypeSpecifier getType() {
        return arrayType.baseType;
    }
}
