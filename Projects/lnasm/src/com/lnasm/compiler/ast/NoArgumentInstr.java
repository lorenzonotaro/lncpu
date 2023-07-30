package com.lnasm.compiler.ast;

import com.lnasm.compiler.*;

import java.util.Locale;

public class NoArgumentInstr implements Encodeable {

    static {
    }

    static class NoArgumentInstrMatcher implements Matcher {

        private final Token.Type tokenType;

        public NoArgumentInstrMatcher(Token.Type tokenType) {
            this.tokenType = tokenType;
        }

        @Override
        public Token.Type getKeyword() {
            return tokenType;
        }

        @Override
        public boolean matches(Argument... arguments) {
            return arguments.length == 0;
        }

        @Override
        public Encodeable make(Argument... arguments) {
            return new NoArgumentInstr(tokenType.toString().toLowerCase(Locale.ROOT));
        }
    }

    private final String instr;

    public NoArgumentInstr(String instr) {
        this.instr = instr;
    }

    @Override
    public byte[] encode(Linker linker, short addr) {
        return new byte[]{OpcodeMap.getOpcode(instr)};
    }

    @Override
    public int size() {
        return 1;
    }
}
