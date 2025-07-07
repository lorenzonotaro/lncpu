package com.lnc.cc.codegen;

import com.lnc.assembler.parser.Instruction;
import com.lnc.assembler.parser.argument.*;
import com.lnc.assembler.parser.argument.Byte;
import com.lnc.assembler.parser.argument.Register;
import com.lnc.cc.ir.IRBlock;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

public class CodeGenUtils {
    public static Argument reg(TokenType regId) {
        return new Register(Token.__internal(regId, regId.toString()));
    }

    public static Instruction instr(TokenType opcode, Argument... args) {
        return new Instruction(Token.__internal(opcode, opcode.toString()), args);
    }

    public static LabelRef labelRef(IRBlock target) {
        return new LabelRef(Token.__internal(TokenType.IDENTIFIER, target.toString()));
    }

    public static Argument labelRef(String asmName) {
        return new LabelRef(Token.__internal(TokenType.IDENTIFIER, asmName));
    }

    public static Argument immByte(int value) {
        return new Byte(Token.__internal(TokenType.INTEGER, value));
    }

    public static Argument immWord(int value) {
        return new Word(Token.__internal(TokenType.INTEGER, value));
    }

    public static Argument bin(Argument left, Argument right, TokenType operator) {
        return new BinaryOp(
                left,
                right,
                Token.__internal(operator, operator.toString())
        );
    }

    public static Argument cast(Argument arg, String castType) {
        return new NumberCast(arg, arg.token, Token.__internal(TokenType.IDENTIFIER, castType));
    }

    public static Argument reg(com.lnc.cc.codegen.Register physReg) {
        return new Register(Token.__internal(physReg.getTokenType(), physReg.toString()));
    }
}
