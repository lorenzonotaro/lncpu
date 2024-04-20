package com.lnasm.compiler.parser.ast;

import com.lnasm.compiler.parser.LnasmParser;
import com.lnasm.compiler.parser.RegisterId;
import com.lnasm.compiler.lexer.Token;

public abstract class Argument{
    final Token token;
    final Type type;

    public Argument(Token token, Type type) {
        this.token = token;
        this.type = type;
    }

    public static class Register extends Argument{
        final RegisterId reg;

        public Register(Token token) {
            super(token, Type.REGISTER);
            this.reg = RegisterId.fromString(token.lexeme);
        }

    }

    public static class Dereference extends Argument{

        final Argument value;

        public Dereference(Argument value) {
            super(value.token, Type.DEREFERENCE);
            this.value = value;
        }

    }

    public static class LongAddress extends Argument{
        final Argument high, low;

        public LongAddress(Argument high, Argument low) {
            super(high.token, Type.L_ADDRESS);
            this.high = high;
            this.low = low;
        }

    }

    public static class Word extends Argument{
        final short value;

        public Word(Token token) {
            super(token, Type.WORD);
            this.value = LnasmParser.ensureShort(token, (Integer) token.literal);
        }
    }
    public static class Byte extends Argument{
        final byte value;

        public Byte(Token token) {
            super(token, Type.BYTE);
            this.value = LnasmParser.ensureByte(token, (Integer) token.literal);
        }

    }

    public static class LabelRef extends Argument{
        final String labelName;

        public LabelRef(Token token) {
            super(token, Type.LABEL);
            this.labelName = token.lexeme;
        }

    }

    enum Type {
        REGISTER,
        DEREFERENCE,
        L_ADDRESS,
        WORD,
        BYTE,
        LABEL
    }

}
