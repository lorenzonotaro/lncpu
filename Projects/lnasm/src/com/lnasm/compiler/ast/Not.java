package com.lnasm.compiler.ast;

import com.lnasm.compiler.*;

class NotRx implements Encodeable {

    private final byte[] encoded;

    NotRx(Argument dest) {
        String instrName = "not_" + ((Argument.Register) dest).reg;

        if(!OpcodeMap.isValid(instrName))
            throw new CompileException("invalid not operand", dest.token);

        encoded = new byte[]{OpcodeMap.getOpcode(instrName)};
    }


    @Override
    public int size() {
        return encoded.length;
    }

    @Override
    public byte[] encode(Linker linker, Segment currentCs) {
        return encoded;
    }
}
