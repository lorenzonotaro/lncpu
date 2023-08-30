package com.lnasm.compiler.ast;

import com.lnasm.compiler.*;

public class BinaryALUOp implements Encodeable{

    private final Token token;
    private final String op;
    private final String x;
    private final String y;
    private final Byte arg;

    private BinaryALUOp(Token token, String op, String x, String y, Byte arg){
        this.token = token;
        this.op = op;
        this.x = x;
        this.y = y;
        this.arg = arg;
    }

    @Override
    public byte[] encode(Linker linker, short addr) {
        String instr = op + "_" + x + "_" + y;
        if(!OpcodeMap.isValid(instr))
            throw new CompileException("invalid arguments", token);
        if(arg == null)
            return new byte[]{OpcodeMap.getOpcode(instr)};
        return new byte[]{OpcodeMap.getOpcode(instr), arg};
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
        public Encodeable make(Parser parser, Token instructionToken, Argument... arguments) {
            String register = arguments[0].token.lexeme.toLowerCase();

            if (arguments[1].type == Argument.Type.REGISTER) {
                return new BinaryALUOp(arguments[0].token, this.keyword.toString().toLowerCase(), register, arguments[1].token.lexeme.toLowerCase(), null);
            }
            return new BinaryALUOp(arguments[0].token, this.keyword.toString().toLowerCase(), register, "cst", ((Argument.Byte) arguments[1]).value);
        }
    }
}
