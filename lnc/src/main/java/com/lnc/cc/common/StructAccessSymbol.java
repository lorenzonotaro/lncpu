package com.lnc.cc.common;

import com.lnc.cc.types.StructFieldEntry;
import com.lnc.cc.types.StructType;
import com.lnc.cc.types.TypeSpecifier;

public class StructAccessSymbol extends AbstractSymbol{
    private final AbstractSymbol structSymbol;
    private final int offset;
    private final TypeSpecifier fieldType;

    public StructAccessSymbol(AbstractSymbol structSymbol, String fieldName){
        this.structSymbol = structSymbol;

        if(structSymbol.getType().type != TypeSpecifier.Type.STRUCT){
            throw new IllegalArgumentException("StructAccessSymbol must be initialized with a struct symbol");
        }

        var structTypeDef = ((StructType) structSymbol.getType());
        StructFieldEntry field = structTypeDef.getDefinition().getField(fieldName);
        this.offset = field.getOffset();
        this.fieldType = field.getField().declarator.typeSpecifier();
    }

    @Override
    public String getAsmName() {
        if(offset == 0){
            return structSymbol.getAsmName();
        }else{
            return "(" + structSymbol.getAsmName() + " + " + offset + ")";
        }
    }

    @Override
    public TypeSpecifier getType() {
        return fieldType;
    }
}
