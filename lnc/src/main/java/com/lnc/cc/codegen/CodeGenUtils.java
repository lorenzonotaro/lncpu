package com.lnc.cc.codegen;

import com.lnc.assembler.parser.Instruction;
import com.lnc.assembler.parser.argument.*;
import com.lnc.assembler.parser.argument.Byte;
import com.lnc.assembler.parser.argument.Register;
import com.lnc.cc.ast.Expression;
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

    public static Argument deref(Argument argument) {
        return new Dereference(argument);
    }

    public static Argument[] splitWord(Argument argument){
        if(argument.type == Argument.Type.WORD){
            return new Argument[]{
                    new Byte(Token.__internal(TokenType.INTEGER, ((Word) argument).value >> 8 & 0xFF)),
                    new Byte(Token.__internal(TokenType.INTEGER, ((Word) argument).value & 0xFF))
            };
        } else if (argument.type == Argument.Type.BYTE) {
            return new Argument[]{new Byte(Token.__internal(TokenType.INTEGER, 0)), argument};
        } else if(argument.type == Argument.Type.LABEL) {
            return new Argument[]{
                    new NumberCast(new BinaryOp(new LabelRef(argument.token), new Byte(Token.__internal(TokenType.INTEGER, 8)), Token.__internal(TokenType.BITWISE_RIGHT, ">>")), argument.token, Token.__internal(TokenType.IDENTIFIER, "byte")),
                    new NumberCast(argument, argument.token, Token.__internal(TokenType.IDENTIFIER, "byte"))
            };
        } else if(argument.type == Argument.Type.COMPOSITE) {
            return new Argument[]{
                    ((Composite) argument).high,
                    ((Composite) argument).low
            };
        }else if(argument.type == Argument.Type.DEREFERENCE) {
            Argument inner = ((Dereference) argument).inner;
            Argument[] splitWord = splitWord(inner);
            return new Argument[]{
                    new Dereference(splitWord[0]),
                    new Dereference(splitWord[1])
            };
        } else if(argument.type == Argument.Type.REGISTER_OFFSET) {
            var regOffset = (RegisterOffset) argument;
            return new Argument[] {
                    regOffset,
                    new RegisterOffset(
                            regOffset.register,
                            regOffset.token,
                            new BinaryOp(
                                    regOffset.offset,
                                    new Byte(Token.__internal(TokenType.INTEGER, 1)),
                                    Token.__internal(TokenType.PLUS, "+")
                            )
                    )
            };
        } else {
            throw new IllegalArgumentException("Argument must be a Word or Byte type");
        }
    }
}
