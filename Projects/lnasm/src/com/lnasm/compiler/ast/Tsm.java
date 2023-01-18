package com.lnasm.compiler.ast;

import com.lnasm.compiler.*;


class TsmRx implements Encodeable {

    static{

    }

    private final byte[] encoded;

    TsmRx(Argument src) {
        String instrName = "tsm_" + ((Argument.Register) src).reg;

        if(!OpcodeMap.isValid(instrName))
            throw new CompileException("invalid tsm source register", src.token);

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

class TsmConstant implements Encodeable{
    private final byte[] encoded;

    public TsmConstant(Argument argument) {
        Argument.Constant cst = (Argument.Constant)argument;
        this.encoded = new byte[]{OpcodeMap.getOpcode("tsm_cst"), cst.value};
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

class TsmConstantDeref implements Encodeable{
    static{

    }

    private final byte[] encoded;

    public TsmConstantDeref(Argument argument) {
        Argument.Dereference deref = (Argument.Dereference)argument;
        String instrName = deref.source == AddressSource.ROM ? "tsm_rom" : "tsm_ram";
        this.encoded = new byte[]{OpcodeMap.getOpcode(instrName), ((Argument.Constant)deref.value).value};
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

