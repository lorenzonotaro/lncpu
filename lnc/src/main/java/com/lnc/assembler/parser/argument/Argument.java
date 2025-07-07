package com.lnc.assembler.parser.argument;

import com.lnc.assembler.common.*;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.common.frontend.Token;

public abstract class Argument implements IEncodeable {
    public final Token token;
    public final Type type;

    public Argument(Token token, Type type) {
        this.token = token;
        this.type = type;
    }

    public abstract String toString();

    public abstract String getImmediateEncoding(ILabelSectionLocator sectionLocator);

    public enum Type {
        REGISTER("register"),
        DEREFERENCE("dereference"),
        COMPOSITE("composite"),
        WORD("word"),
        BYTE("byte"),
        LABEL("label"),
        BINARY_OP("binary"),
        CAST("cast"),

        REGISTER_OFFSET("register offset"),
        ;


        public final String name;

        Type(String name){
            this.name = name;
        }

        @Override
        public String toString(){
            return name;
        }
    }

}
