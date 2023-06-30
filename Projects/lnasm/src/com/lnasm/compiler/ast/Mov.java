package com.lnasm.compiler.ast;

import com.lnasm.compiler.*;

class MovRxRx implements Encodeable {
    static{
        //mov register, register
    }

    private final byte[] encoding;

    public MovRxRx(Argument src, Argument dest) {
        RegisterId srcReg = ((Argument.Register) src).reg;
        RegisterId destReg = ((Argument.Register) dest).reg;

        String instrName = "mov_" + srcReg + "_" + destReg;
        if(!OpcodeMap.isValid(instrName))
            throw new CompileException("invalid mov src/dest combination", src.token);

        this.encoding = new byte[]{OpcodeMap.getOpcode(instrName)};
    }

    @Override
    public byte[] encode(Linker linker, short addr) {
        return encoding;
    }

    @Override
    public int size() {
        return encoding.length;
    }
}

class MovIndirect implements Encodeable {
    static{
        //mov register, register


    }

    private final byte[] encoding;

    public MovIndirect(Argument srcArg, Argument destArg) {
        Argument.Type srcType = srcArg.type;
        Argument.Type destType = destArg.type;

        ImmediateParamEncoding src = null;
        ImmediateParamEncoding dest = null;

        src = new ImmediateParamEncoding(srcArg);
        dest = new ImmediateParamEncoding(destArg);

        //concatenate args and opcode
        this.encoding = new byte[1 + src.args.length + dest.args.length];

        String immediateInstruction = "mov_" + src.immediateName + "_" + dest.immediateName;

        if(!OpcodeMap.isValid(immediateInstruction))
            throw new CompileException("invalid mov src/dest combination", srcArg.token);

        this.encoding[0] = OpcodeMap.getOpcode(immediateInstruction);
        System.arraycopy(src.args, 0, this.encoding, 1, src.args.length);
        System.arraycopy(dest.args, 0, this.encoding, 1 + src.args.length, dest.args.length);
    }


    @Override
    public int size() {
        return encoding.length;
    }

    @Override
    public byte[] encode(Linker linker, short addr) {
        return encoding;
    }


}

class MovConstantRx implements Encodeable {
    static{
        //mov register, register

    }

    private final byte[] encoding;

    public MovConstantRx(Argument src, Argument dest) {
        RegisterId destReg = ((Argument.Register) dest).reg;
        byte value = ((Argument.Byte)src).value;
        String instrName = "mov_cst_" + destReg;
        if(!OpcodeMap.isValid(instrName))
            throw new CompileException("invalid mov src/dest combination", src.token);

        this.encoding = new byte[]{OpcodeMap.getOpcode(instrName), value};
    }

    @Override
    public int size() {
        return encoding.length;
    }

    @Override
    public byte[] encode(Linker linker, short addr) {
        return encoding;
    }
}