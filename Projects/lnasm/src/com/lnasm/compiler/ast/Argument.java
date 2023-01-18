package com.lnasm.compiler.ast;

import com.lnasm.compiler.Parser;
import com.lnasm.compiler.Token;

public abstract class Argument{
    final Token token;
    final Type type;

    public Argument(Token token, Type type) {
        this.token = token;
        this.type = type;
    }

    public static class Register extends Argument{
        final ID reg;

        public Register(Token token) {
            super(token, Type.REGISTER);
            this.reg = ID.fromString(token.lexeme);
        }

        enum ID{
            RA(true), RB(true), RC(true), RD(true), MDS(false), SS(false), SP(false), SDS(false);

            private final boolean generalPurpose;

            ID(boolean generalPurpose) {
                this.generalPurpose = generalPurpose;
            }

            @Override
            public String toString() {
                return super.toString().toLowerCase();
            }

            public static ID fromString(String str){
                return ID.valueOf(str.toUpperCase());
            }

            public boolean isGeneralPurpose() {
                return generalPurpose;
            }
        }
    }

    public static class Dereference extends Argument{

        final AddressSource source;
        final Argument value;

        public Dereference(AddressSource source, Argument value) {
            super(value.token, Type.DEREFERENCE);
            this.source = source;
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

    public static class Constant extends Argument{
        final byte value;

        public Constant(Token token) {
            super(token, Type.CONSTANT);
            this.value = Parser.ensureByte(token, (Integer) token.literal);
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
        CONSTANT,
        LABEL
    }

}
