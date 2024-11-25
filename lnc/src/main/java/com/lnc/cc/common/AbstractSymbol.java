package com.lnc.cc.common;

import com.lnc.cc.types.StructDefinitionType;
import com.lnc.cc.types.TypeSpecifier;

public abstract class AbstractSymbol {
    public abstract String getAsmName();

    public abstract TypeSpecifier getType();
}
