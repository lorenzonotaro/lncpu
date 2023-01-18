package com.lnasm.compiler.ast;

import com.lnasm.compiler.*;

class Cmp implements Encodeable {
    private final byte[] encoding;

    public Cmp(Argument a, Argument b) {
        Argument.Register.ID aReg = ((Argument.Register) a).reg;
        Argument.Register.ID bReg = ((Argument.Register) b).reg;

        String instrName = "cmp_" + aReg + "_" + bReg;
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
