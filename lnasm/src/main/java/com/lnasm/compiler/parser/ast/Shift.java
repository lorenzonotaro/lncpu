package com.lnasm.compiler.parser.ast;

import com.lnasm.compiler.*;
import com.lnasm.compiler.linker.AbstractLinker;
import com.lnasm.compiler.parser.Encodeable;
import com.lnasm.compiler.parser.OpcodeMap;

class Shl implements Encodeable {

    private final byte[] encoded;

    Shl(Argument dest) {
        String instrName = "shl_" + ((Argument.Register) dest).reg;

        if(!OpcodeMap.isValid(instrName))
            throw new CompileException("invalid shl operand (only RA is allowed)", dest.token);

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

class Shr implements Encodeable {

    private final byte[] encoded;

    Shr(Argument dest) {
        String instrName = "shr_" + ((Argument.Register) dest).reg;

        if(!OpcodeMap.isValid(instrName))
            throw new CompileException("invalid shl operand (only RA is allowed)", dest.token);

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
