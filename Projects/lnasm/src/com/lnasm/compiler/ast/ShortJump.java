package com.lnasm.compiler.ast;

import com.lnasm.compiler.*;

class ShortJump implements Encodeable {
    private final String instruction;
    private final Argument address;

    ShortJump(String instruction, Argument address) {
        this.instruction = instruction;
        this.address = address;
    }

    @Override
    public byte[] encode(Linker linker, Segment currentCs) {
        if (address.type == Argument.Type.CONSTANT){
            return new byte[]{OpcodeMap.getOpcode(instruction), ((Argument.Constant)address).value};
        }else if(address.type == Argument.Type.LABEL){
            byte labelTo = currentCs.resolveLabel(((Argument.LabelRef)address).labelName, address.token);
            return new byte[]{OpcodeMap.getOpcode(instruction), labelTo};
        }else throw new Error("invalid case");
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
                    (arguments[0].type == Argument.Type.CONSTANT ||
                            (arguments[0].type == Argument.Type.LABEL));
        }

        @Override
        public Encodeable make(Argument... arguments) {
            return new ShortJump(jInstr.toString().toLowerCase(), arguments[0]);
        }
    }
}
