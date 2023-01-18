package com.lnasm.compiler.ast;

import com.lnasm.compiler.*;

class MovRxRx implements Encodeable {
    static{
        //mov register, register
    }

    private final byte[] encoding;

    public MovRxRx(Argument src, Argument dest) {
        Argument.Register.ID srcReg = ((Argument.Register) src).reg;
        Argument.Register.ID destReg = ((Argument.Register) dest).reg;

        String instrName = "mov_" + srcReg + "_" + destReg;
        if(!OpcodeMap.isValid(instrName))
            throw new CompileException("invalid mov src/dest combination", src.token);

        this.encoding = new byte[]{OpcodeMap.getOpcode(instrName)};
    }

    @Override
    public byte[] encode(Linker linker, Segment currentCs) {
        return encoding;
    }

    @Override
    public int size() {
        return encoding.length;
    }
}

class MovRxRxDeref implements Encodeable {
    static{
        //mov register, register


    }

    private final byte[] encoding;

    public MovRxRxDeref(Argument src, Argument dest) {
        Argument.Register.ID srcReg = ((Argument.Register) src).reg;
        Argument.Register.ID destReg = ((Argument.Register)((Argument.Dereference)dest).value).reg;

        String instrName = "mov_" + srcReg + "_" + destReg + "_deref";
        if(!OpcodeMap.isValid(instrName))
            throw new CompileException("invalid mov src/dest combination", src.token);

        this.encoding = new byte[]{OpcodeMap.getOpcode(instrName)};
    }


    @Override
    public int size() {
        return encoding.length;
    }

    @Override
    public byte[] encode(Linker linker, Segment currentCs) {
        return encoding;
    }
}

class MovRxConstantDeref implements Encodeable {
    static{
        //mov register, register

    }

    private final byte[] encoding;

    public MovRxConstantDeref(Argument src, Argument dest) {
        Argument.Register.ID srcReg = ((Argument.Register) src).reg;
        byte value = ((Argument.Constant)((Argument.Dereference)dest).value).value;;

        String instrName = "mov_" + srcReg + "_ram";
        if(!OpcodeMap.isValid(instrName))
            throw new CompileException("invalid mov src/dest combination", src.token);

        this.encoding = new byte[]{OpcodeMap.getOpcode(instrName), value};
    }


    @Override
    public int size() {
        return encoding.length;
    }

    @Override
    public byte[] encode(Linker linker, Segment currentCs) {
        return encoding;
    }
}

class MovConstantDerefRx implements Encodeable {
    static{
        //mov register, register

    }

    private final byte[] encoding;

    public MovConstantDerefRx(Argument src, Argument dest) {
        byte srcConstant = ((Argument.Constant)((Argument.Dereference)src).value).value;;
        Argument.Register.ID destReg = ((Argument.Register) dest).reg;
        String instrName = "mov_" + (((Argument.Dereference)src).source == AddressSource.ROM ? "rom_" : "ram_") + destReg;
        if(!OpcodeMap.isValid(instrName))
            throw new CompileException("invalid mov src/dest combination", src.token);

        this.encoding = new byte[]{OpcodeMap.getOpcode(instrName), srcConstant};
    }


    @Override
    public int size() {
        return encoding.length;
    }

    @Override
    public byte[] encode(Linker linker, Segment currentCs) {
        return encoding;
    }
}

class MovConstantDerefConstantDeref implements Encodeable {
    static{
        //mov register, register

    }

    private final byte[] encoding;

    public MovConstantDerefConstantDeref(Argument src, Argument dest) {
        byte srcConstant = ((Argument.Constant)((Argument.Dereference)src).value).value;
        byte destConstant = ((Argument.Constant)((Argument.Dereference)dest).value).value;
        String instrName = "mov_rom_ram";
        if(!OpcodeMap.isValid(instrName))
            throw new CompileException("invalid mov src/dest combination", src.token);

        this.encoding = new byte[]{OpcodeMap.getOpcode(instrName), srcConstant, destConstant};
    }



    @Override
    public int size() {
        return encoding.length;
    }

    @Override
    public byte[] encode(Linker linker, Segment currentCs) {
        return encoding;
    }
}

class MovRxDerefRx implements Encodeable {
    static{
        //mov register, register

    }

    private final byte[] encoding;

    public MovRxDerefRx(Argument src, Argument dest) {
        Argument.Register.ID destReg = ((Argument.Register) dest).reg;
        Argument.Register.ID srcReg = ((Argument.Register)((Argument.Dereference)src).value).reg;

        String instrName = "mov_" + srcReg + "_deref_" + destReg;
        if(!OpcodeMap.isValid(instrName))
            throw new CompileException("invalid mov src/dest combination", src.token);

        this.encoding = new byte[]{OpcodeMap.getOpcode(instrName)};
    }


    @Override
    public int size() {
        return encoding.length;
    }

    @Override
    public byte[] encode(Linker linker, Segment currentCs) {
        return encoding;
    }
}

class MovConstantRx implements Encodeable {
    static{
        //mov register, register

    }

    private final byte[] encoding;

    public MovConstantRx(Argument src, Argument dest) {
        Argument.Register.ID destReg = ((Argument.Register) dest).reg;
        byte value = ((Argument.Constant)src).value;

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
    public byte[] encode(Linker linker, Segment currentCs) {
        return encoding;
    }
}

class MovConstantConstantDeref implements Encodeable {
    static{
        //mov register, register
        Matcher.addMatcher(new Matcher() {
            @Override
            public Token.Type getKeyword() {
                return Token.Type.MOV;
            }

            @Override
            public boolean matches(Argument... arguments) {
                return arguments.length == 2
                        && arguments[0].type == Argument.Type.CONSTANT
                        && arguments[1].type == Argument.Type.DEREFERENCE
                        && ((Argument.Dereference)arguments[1]).value.type == Argument.Type.CONSTANT
                        && ((Argument.Dereference)arguments[1]).source != AddressSource.ROM;
            }

            @Override
            public Encodeable make(Argument... arguments) {
                return new MovConstantConstantDeref(arguments[0], arguments[1]);
            }
        });
    }

    private final byte[] encoding;

    public MovConstantConstantDeref(Argument src, Argument dest) {
        byte srcValue = ((Argument.Constant)src).value;
        byte destValue = ((Argument.Constant)((Argument.Dereference)dest).value).value;

        String instrName = "mov_cst_ram";
        if(!OpcodeMap.isValid(instrName))
            throw new CompileException("invalid mov src/dest combination", src.token);

        this.encoding = new byte[]{OpcodeMap.getOpcode(instrName), srcValue, destValue};
    }


    @Override
    public int size() {
        return encoding.length;
    }

    @Override
    public byte[] encode(Linker linker, Segment currentCs) {
        return encoding;
    }
}
