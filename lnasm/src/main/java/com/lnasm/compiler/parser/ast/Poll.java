package com.lnasm.compiler.parser.ast;

import com.lnasm.compiler.*;
import com.lnasm.compiler.linker.AbstractLinker;
import com.lnasm.compiler.parser.Encodeable;
import com.lnasm.compiler.parser.OpcodeMap;

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
    public byte[] encode(AbstractLinker linker, short addr) {
        return encoded;
    }
}
