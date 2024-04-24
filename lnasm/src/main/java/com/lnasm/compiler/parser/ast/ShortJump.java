package com.lnasm.compiler.parser.ast;

import com.lnasm.LNASM;
import com.lnasm.Logger;
import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.lexer.Token;
import com.lnasm.compiler.linker.AbstractLinker;
import com.lnasm.compiler.common.Encodeable;
import com.lnasm.compiler.common.OpcodeMap;
import com.lnasm.compiler.parser.LnasmParser;

public class ShortJump implements Encodeable {

    private final String jumpInstr;
    private final String parentLabel;
    private final Argument target;

    ShortJump(String jumpInstr, String parentLabel, Argument target) {
        this.jumpInstr = jumpInstr;
        this.parentLabel = parentLabel;
        this.target = target;
    }

    @Override
    public byte[] encode(AbstractLinker linker, short currentAddr) {
        byte low = 0;
        switch (target.type) {
            case LABEL -> {
                Argument.LabelRef lr = (Argument.LabelRef) target;
                short targetAddr = linker.resolveLabel(parentLabel, lr.labelName, lr.token);
                byte high = (byte) (targetAddr >> 8);

                if ((targetAddr & 0xFF00) != (currentAddr & 0xFF00) && !LNASM.settings.get("-Wshort-jump-out-of-range", Boolean.class))
                    Logger.compileWarning("referenced label in short jump is outside of code segment. Use 'l" + jumpInstr + "' instead (-Wshort-jump-out-of-range)", target.token);

                low = (byte) targetAddr;
            }
            case BYTE -> {
                Argument.Byte b = (Argument.Byte) target;
                low = b.value;
            }
            default -> throw new CompileException("invalid jump target", target.token);
        }

        if(!OpcodeMap.isValid(jumpInstr))
            throw new CompileException("invalid jump instruction", target.token);

        return new byte[]{OpcodeMap.getOpcode(jumpInstr), low};
    }

    @Override
    public int size() {
        return 2;
    }

    static class SJumpInstrMatcher implements Matcher {
        private final Token.Type jInstr;

        public SJumpInstrMatcher(Token.Type jInstr) {
            this.jInstr = jInstr;
        }

        @Override
        public Token.Type getKeyword() {
            return jInstr;
        }

        @Override
        public boolean matches(Argument... arguments) {
            return arguments.length == 1 &&
                    (arguments[0].type == Argument.Type.BYTE ||
                            arguments[0].type == Argument.Type.LABEL);
        }

        @Override
        public Encodeable make(LnasmParser parser, Token instructionToken, Argument... arguments) {
            return new ShortJump(jInstr.toString().toLowerCase(), parser.getCurrentParentLabel(), arguments[0]);
        }
    }
}