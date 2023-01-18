package com.lnasm.compiler.ast;

import com.lnasm.compiler.*;

class PollRx implements Encodeable {

    static{

    }

    private final byte[] encoded;

    PollRx(Argument dest) {
        String instrName = "poll_" + ((Argument.Register) dest).reg;

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
