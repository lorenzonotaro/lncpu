package com.lnasm.compiler.ast;

import com.lnasm.compiler.*;

class OrRxRx implements Encodeable {
    private final byte[] encoding;

    public OrRxRx(Argument a, Argument b) {
        Argument.Register.ID aReg = ((Argument.Register) a).reg;
        Argument.Register.ID bReg = ((Argument.Register) b).reg;

        String instrName = "or_" + aReg + "_" + bReg;
        if(!OpcodeMap.isValid(instrName))
            throw new CompileException("invalid operand combination", a.token);

        this.encoding = new byte[]{OpcodeMap.getOpcode(instrName)};
    }


    @Override
    public int size() {
        return encoding.length;
    }

    @Override
    public byte[] encode(Linker linker, Segment currentCs) {
        return encoding;
    }
}