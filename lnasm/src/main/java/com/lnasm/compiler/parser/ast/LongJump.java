package com.lnasm.compiler.parser.ast;

import com.lnasm.Logger;
import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.lexer.Token;
import com.lnasm.compiler.linker.AbstractLinker;
import com.lnasm.compiler.common.Encodeable;
import com.lnasm.compiler.common.OpcodeMap;
import com.lnasm.compiler.parser.LnasmParser;
import com.lnasm.compiler.parser.RegisterId;

class LongJump implements Encodeable {

    private final String jumpInstr;
    private final String parentLabel;
    private final Argument target;
    private boolean isIFullRcRd;

    LongJump(String jumpInstr, String parentLabel, Argument target, boolean isIFullRcRd) {
        this.jumpInstr = jumpInstr;
        this.parentLabel = parentLabel;
        this.target = target;
        this.isIFullRcRd = isIFullRcRd;
    }

    @Override
    public byte[] encode(AbstractLinker linker, short currentAddr) {
        byte high = 0, low = 0;
        switch (target.type){
            case L_ADDRESS -> {
                Argument.LongAddress la = (Argument.LongAddress) target;
                if(la.high.type == Argument.Type.BYTE && la.low.type == Argument.Type.BYTE){
                    high = ((Argument.Byte) la.high).value;
                    low = ((Argument.Byte) la.low).value;
                }else if(isIFullRcRd){
                    return new byte[]{OpcodeMap.getOpcode(jumpInstr + "_ifullrcrd")};
                }else{
                    throw new CompileException("invalid jump target", target.token);
                }
            }
            case LABEL -> {
                Argument.LabelRef lr = (Argument.LabelRef) target;
                short targetAddr = linker.resolveLabel(parentLabel, lr.labelName, lr.token);
                high = (byte) (targetAddr >> 8);
                low = (byte) targetAddr;
            }
            case WORD -> {
                Argument.Word w = (Argument.Word) target;
                high = (byte) (w.value >> 8);
                low = (byte) (w.value & 0xFF);
            }
            default -> throw new CompileException("invalid jump target", target.token);
        }

        if(high == (byte) (currentAddr >> 8) && !jumpInstr.equals("lcall"))
            Logger.compileWarning("A long jump is performed to an address in the same code block. Consider using '" + jumpInstr.substring(1) + "' instead (-Woptimizable-long-jump)", target.token);

        if(!OpcodeMap.isValid(jumpInstr))
            throw new CompileException("invalid jump instruction", target.token);
        return new byte[]{OpcodeMap.getOpcode(jumpInstr), high, low};
    }

    @Override
    public int size() {
        return isIFullRcRd ? 1 : 3;
    }

    static class LJumpInstrMatcher implements Matcher {
        private final Token.Type jInstr;

        public LJumpInstrMatcher(Token.Type jInstr) {
            this.jInstr = jInstr;
        }

        @Override
        public Token.Type getKeyword() {
            return jInstr;
        }

        @Override
        public boolean matches(Argument... arguments) {
            return arguments.length == 1 &&
                    (arguments[0].type == Argument.Type.L_ADDRESS ||
                            arguments[0].type == Argument.Type.LABEL ||
                            arguments[0].type == Argument.Type.WORD);
        }

        @Override
        public Encodeable make(LnasmParser parser, Token instructionToken, Argument... arguments) {
            boolean isIFullRcRd = false;

            if(arguments[0].type == Argument.Type.L_ADDRESS){
                Argument.LongAddress la = (Argument.LongAddress) arguments[0];
                isIFullRcRd = la.high.type == Argument.Type.REGISTER && ((Argument.Register)la.high).reg.equals(RegisterId.RC) && la.low.type == Argument.Type.REGISTER && ((Argument.Register)la.low).reg.equals(RegisterId.RD);
            }

            return new LongJump(jInstr.toString().toLowerCase(), parser.getCurrentParentLabel(), arguments[0], isIFullRcRd);
        }
    }
}
