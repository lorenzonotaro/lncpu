package com.lnasm.compiler.parser.argument;

import com.lnasm.compiler.common.*;
import com.lnasm.compiler.linker.AbstractLinker;

public abstract class Argument implements IEncodeable {
    public final Token token;
    public final Type type;

    public Argument(Token token, Type type) {
        this.token = token;
        this.type = type;
    }

    public abstract String getImmediateEncoding(ILabelSectionLocator sectionLocator, AbstractLinker linker);

    public enum Type {
        REGISTER("register"),
        DEREFERENCE("dereference"),
        COMPOSITE("composite"),
        WORD("word"),
        BYTE("byte"),
        LABEL("label");

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
