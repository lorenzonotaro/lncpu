package com.lnasm.compiler.parser.argument;

import com.lnasm.compiler.common.*;
import com.lnasm.compiler.linker.ILabelSectionLocator;

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
        BINARY_OP("binary");

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
