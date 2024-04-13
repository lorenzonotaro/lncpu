package com.lnasm.compiler.parser.ast;

import com.lnasm.compiler.*;
import com.lnasm.compiler.linker.AbstractLinker;
import com.lnasm.compiler.parser.Encodeable;
import com.lnasm.compiler.parser.OpcodeMap;
import com.lnasm.compiler.parser.RegisterId;

class OrRxRx implements Encodeable {
    private final byte[] encoding;

    public OrRxRx(Argument a, Argument b) {
        RegisterId aReg = ((Argument.Register) a).reg;
        RegisterId bReg = ((Argument.Register) b).reg;

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
    public byte[] encode(AbstractLinker linker, short addr) {
        return encoding;
    }
}