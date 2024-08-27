package com.lnc.assembler.parser.argument;

import com.lnc.assembler.common.*;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.common.frontend.Token;

public abstract class Argument implements IEncodeable {
    public final Token token;
    public final Type type;
    public final boolean numerical;

    public Argument(Token token, Type type, boolean numerical) {
        this.token = token;
        this.type = type;
        this.numerical = numerical;
    }

    public abstract String getImmediateEncoding(ILabelSectionLocator sectionLocator);

    public enum Type {
        REGISTER("register"),
        DEREFERENCE("dereference"),
        COMPOSITE("composite"),
        WORD("word"),
        BYTE("byte"),
        LABEL("label"),
        BINARY_OP("binary"),
        CAST("cast");


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
