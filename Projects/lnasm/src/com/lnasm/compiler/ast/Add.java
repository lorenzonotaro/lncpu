package com.lnasm.compiler.ast;

import com.lnasm.compiler.*;

class AddRxRx implements Encodeable {
   private final byte[] encoding;

    public AddRxRx(Argument a, Argument b) {
        RegisterId aReg = ((Argument.Register) a).reg;
        RegisterId bReg = ((Argument.Register) b).reg;

        String instrName = "add_" + aReg + "_" + bReg;
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
