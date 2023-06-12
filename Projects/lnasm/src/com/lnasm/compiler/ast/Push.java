package com.lnasm.compiler.ast;

import com.lnasm.compiler.*;

class PushRx implements Encodeable{


    private final byte[] encoded;

    public PushRx(Argument src) {

        RegisterId srcReg = ((Argument.Register)src).reg;
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

class PushIndirect implements Encodeable{
    private final byte[] encoded;

    public PushIndirect(Argument arg) {
ImmediateParamEncoding src = new ImmediateParamEncoding(arg);

        //concatenate args and opcode
        this.encoded = new byte[1 + src.args.length];

        String immediateInstruction = "push_" + src.immediateName;

        if(!OpcodeMap.isValid(immediateInstruction))
            throw new CompileException("invalid push source", arg.token);

        encoded[0] = OpcodeMap.getOpcode(immediateInstruction);
        System.arraycopy(src.args, 0, encoded, 1, src.args.length);
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


        this.encoded = new byte[]{OpcodeMap.getOpcode(instrName), ((Argument.Byte)src).value};
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

