package com.lnasm.compiler.ast;

import com.lnasm.compiler.*;

class PushRx implements Encodeable{


    private final byte[] encoded;

    public PushRx(Argument src) {

        Argument.Register.ID srcReg = ((Argument.Register)src).reg;
        String instrName = "push_" + srcReg;
        if(!OpcodeMap.isValid(instrName))
            throw new CompileException("invalid push source register", src.token);

        this.encoded = new byte[]{OpcodeMap.getOpcode(instrName)};
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

class PushDeref implements Encodeable{
    private final byte[] encoded;

    public PushDeref(Argument src) {

        String instrName = "push_rom";
        if(!OpcodeMap.isValid(instrName))
            throw new CompileException("invalid push source register", src.token);

        this.encoded = new byte[]{OpcodeMap.getOpcode(instrName), ((Argument.Constant)((Argument.Dereference)src).value).value};
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

class PushConstant implements Encodeable{

    private final byte[] encoded;

    public PushConstant(Argument src) {

        String instrName = "push_cst";
        if(!OpcodeMap.isValid(instrName))
            throw new CompileException("invalid push source register", src.token);

        this.encoded = new byte[]{OpcodeMap.getOpcode(instrName), ((Argument.Constant)src).value};
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

