package com.lnasm.compiler.parser.ast;

import com.lnasm.compiler.*;
import com.lnasm.compiler.linker.AbstractLinker;
import com.lnasm.compiler.parser.Encodeable;
import com.lnasm.compiler.parser.OpcodeMap;

class PopRx implements Encodeable {

    private final byte[] encoded;

    PopRx(Argument dest) {
        String instrName = "pop_" + ((Argument.Register) dest).reg.toString().toLowerCase();

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

class PopIndirect implements Encodeable{
    private final byte[] encoded;

    public PopIndirect(Argument arg) {
        ImmediateParamEncoding dest = new ImmediateParamEncoding(arg);

        //concatenate args and opcode
        this.encoded = new byte[1 + dest.args.length];

        String immediateInstruction = "pop_" + dest.immediateName;

        if(!OpcodeMap.isValid(immediateInstruction))
            throw new CompileException("invalid pop destination", arg.token);

        encoded[0] = OpcodeMap.getOpcode(immediateInstruction);
        System.arraycopy(dest.args, 0, encoded, 1, dest.args.length);
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

