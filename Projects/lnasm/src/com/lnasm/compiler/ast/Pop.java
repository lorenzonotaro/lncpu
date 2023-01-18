package com.lnasm.compiler.ast;

import com.lnasm.compiler.*;

class PopRx implements Encodeable {

    private final byte[] encoded;

    PopRx(Argument dest) {
        String instrName = "pop_" + ((Argument.Register) dest).reg;

        if(!OpcodeMap.isValid(instrName))
            throw new CompileException("invalid pop dest register", dest.token);

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
