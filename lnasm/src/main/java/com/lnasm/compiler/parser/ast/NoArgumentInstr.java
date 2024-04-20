package com.lnasm.compiler.parser.ast;

import com.lnasm.compiler.*;
import com.lnasm.compiler.lexer.Token;
import com.lnasm.compiler.linker.AbstractLinker;
import com.lnasm.compiler.parser.Encodeable;
import com.lnasm.compiler.parser.OpcodeMap;
import com.lnasm.compiler.parser.LnasmParser;

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
        public Encodeable make(LnasmParser parser, Token instructionToken, Argument... arguments) {
            return new NoArgumentInstr(instructionToken, tokenType.toString().toLowerCase(Locale.ROOT));
        }
    }

    protected final Token token;
    private final String instr;

    public NoArgumentInstr(Token token, String instr) {
        this.token = token;
        this.instr = instr;
    }

    @Override
    public byte[] encode(AbstractLinker linker, short addr) {
        if(!OpcodeMap.isValid(instr))
            throw new CompileException("invalid instruction", token);
        return new byte[]{OpcodeMap.getOpcode(instr)};
    }

    @Override
    public int size() {
        return 1;
    }
}
