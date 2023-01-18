package com.lnasm.compiler.ast;

import com.lnasm.compiler.*;

class AndRxRx implements Encodeable {

    private final byte[] encoding;

    public AndRxRx(Argument a, Argument b) {
        Argument.Register.ID aReg = ((Argument.Register) a).reg;
        Argument.Register.ID bReg = ((Argument.Register) b).reg;

        String instrName = "and_" + aReg + "_" + bReg;
        if(!OpcodeMap.isValid(instrName))
            throw new CompileException("invalid operand combination", a.token);

        this.encoding = new byte[]{OpcodeMap.getOpcode(instrName)};
    }

    @Override
    public byte[] encode(Linker linker, Segment currentCs) {
        return encoding;
    }

    @Override
    public int size() {
        return encoding.length;
    }
}