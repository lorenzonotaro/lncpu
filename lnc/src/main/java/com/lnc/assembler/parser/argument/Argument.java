package com.lnc.assembler.parser.argument;

import com.lnc.assembler.common.*;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.common.frontend.Token;

/**
 * Represents an abstract argument used in the encoding or representation
 * of assembly language components. Each argument is associated with a specific
 * token and type, and can be extended to represent more concrete argument types.
 *
 * The Argument class defines methods for obtaining string representations,
 * encoding arguments into binary formats, and comparing argument objects for equality.
 *
 * Subclasses of Argument are expected to provide implementations for the abstract methods
 * and may add additional functionality specific to their type.
 */
public abstract class Argument implements IEncodeable {
    public final Token token;
    public final Type type;

    public Argument(Token token, Type type) {
        this.token = token;
        this.type = type;
    }

    public abstract String toString();

    public abstract String getImmediateEncoding(ILabelSectionLocator sectionLocator);

    @Override
    public abstract boolean equals(Object obj);

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
