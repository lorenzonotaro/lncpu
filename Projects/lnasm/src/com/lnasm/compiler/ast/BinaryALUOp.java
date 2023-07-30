package com.lnasm.compiler.ast;

import com.lnasm.compiler.Encodeable;
import com.lnasm.compiler.Linker;
import com.lnasm.compiler.OpcodeMap;
import com.lnasm.compiler.Token;

public class BinaryALUOp implements Encodeable{

    private final String op;
    private final String x;
    private final String y;
    private final Byte arg;

    private BinaryALUOp(String op, String x, String y, Byte arg){
        this.op = op;
        this.x = x;
        this.y = y;
        this.arg = arg;
    }

    @Override
    public byte[] encode(Linker linker, short addr) {
        if(arg == null)
            return new byte[]{OpcodeMap.getOpcode(op + "_" + x + "_" + y)};
        return new byte[]{OpcodeMap.getOpcode(op + "_" + x + "_" + y), arg};
    }

    @Override
    public int size() {
        return arg != null ? 2 : 1;

    }

    static class BinaryALUOpMatcher implements Matcher {

        private final Token.Type keyword;

        public BinaryALUOpMatcher(Token.Type keyword) {
            this.keyword = keyword;
        }

        @Override
        public Token.Type getKeyword() {
            return this.keyword;
        }

        @Override
        public boolean matches(Argument... arguments) {
            return arguments.length == 2 && arguments[0].type == Argument.Type.REGISTER
                    && (arguments[1].type == Argument.Type.REGISTER || arguments[1].type == Argument.Type.BYTE);
        }

        @Override
        public Encodeable make(Argument... arguments) {
            String register = arguments[0].token.lexeme.toLowerCase();

            if (arguments[1].type == Argument.Type.REGISTER) {
                return new BinaryALUOp(this.keyword.toString().toLowerCase(), register, arguments[1].token.lexeme.toLowerCase(), null);
            }
            return new BinaryALUOp(this.keyword.toString().toLowerCase(), register, "cst", ((Argument.Byte) arguments[1]).value);
        }
    }
}
