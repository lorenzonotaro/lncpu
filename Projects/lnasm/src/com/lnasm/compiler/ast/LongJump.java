package com.lnasm.compiler.ast;

import com.lnasm.compiler.*;

class LongJump implements Encodeable {

    private final String instruction;
    private final Argument.LongAddress address;

    LongJump(String instruction, Argument address) {
        this.instruction = instruction;
        this.address = (Argument.LongAddress) address;
    }

    @Override
    public byte[] encode(Linker linker, Segment currentCs) {
        if (address.high.type != Argument.Type.CONSTANT || !(address.low.type == Argument.Type.CONSTANT || address.low.type == Argument.Type.LABEL)){
            throw new CompileException("invalid cs:pc combination", address.token);
        }else {
            byte cs = ((Argument.Constant)address.high).value;
            if (address.low.type == Argument.Type.LABEL) {
                byte labelTo = linker.resolveLabel(cs, ((Argument.LabelRef) address.low).labelName, address.token);
                return new byte[]{OpcodeMap.getOpcode(instruction), cs, labelTo};
            }else throw new Error("invalid case");
        }
    }

    @Override
    public int size() {
        return 3;
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
                    (arguments[0].type == Argument.Type.L_ADDRESS);
        }

        @Override
        public Encodeable make(Argument... arguments) {
            return new LongJump("l" + jInstr.toString().toLowerCase(), arguments[0]);
        }
    }
}
