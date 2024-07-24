package com.lnasm.compiler.parser.argument;

import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.common.Token;
import com.lnasm.compiler.linker.ILabelResolver;
import com.lnasm.compiler.linker.ILabelSectionLocator;

import java.io.IOException;

public class BinaryOp extends Argument{

    public final Argument left, right;

    public final Operator operator;

    public BinaryOp(Argument left, Argument right, Token token){
        super(token, Type.BINARY_OP, true);
        this.left = left;
        this.right = right;

        if(!(left.numerical && right.numerical)){
            throw new CompileException("invalid operands for binary operator: %s %s %s".formatted(left.type, token.lexeme, right.type), token);
        }

        this.operator = operatorFromToken(token);
    }

    private Operator operatorFromToken(Token token) {
        return switch (token.type) {
            case PLUS -> Operator.ADD;
            case MINUS -> Operator.SUB;
            case STAR -> Operator.MUL;
            case SLASH -> Operator.DIV;
            case BITWISE_LEFT -> Operator.SHL;
            case BITWISE_RIGHT -> Operator.SHR;
            case BITWISE_AND -> Operator.AND;
            case BITWISE_OR -> Operator.OR;
            case BITWISE_XOR -> Operator.XOR;
            default -> throw new CompileException("Invalid operator", token);
        };
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        return left.size(sectionLocator);
    }

    @Override
    public byte[] encode(ILabelResolver labelResolver, int instructionAddress) throws IOException {

        byte[] leftBytes = left.encode(labelResolver, instructionAddress);
        byte[] rightBytes = right.encode(labelResolver, instructionAddress);

        long leftVal = decode(leftBytes);
        long rightVal = decode(rightBytes);

        long result = switch (operator) {
            case ADD -> leftVal + rightVal;
            case SUB -> leftVal - rightVal;
            case MUL -> leftVal * rightVal;
            case DIV -> leftVal / rightVal;
            case SHL -> leftVal << rightVal;
            case SHR -> leftVal >> rightVal;
            case AND -> leftVal & rightVal;
            case OR -> leftVal | rightVal;
            case XOR -> leftVal ^ rightVal;
        };

        encode(leftBytes, (int) result);

        return leftBytes;
    }


    private static int decode(byte[] bytes){
        int result = 0;
        for (int i = 0, bytesLength = bytes.length; i < bytesLength; i++) {
            byte b = bytes[i];
            result <<= 8;
            result |= (b & 0xFF);
        }
        return result;
    }

    private static void encode(byte[] bytes, int value){
        for(int i = bytes.length - 1; i >= 0; i--){
            bytes[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        return left.getImmediateEncoding(sectionLocator);
    }

    public enum Operator {
        ADD("+"),
        SUB("-"),
        MUL("*"),
        DIV("/"),
        SHL("<<"),
        SHR(">>"),
        AND("&"),
        OR("|"),
        XOR("^")
        ;

        private final String name;

        Operator(String name){
            this.name = name;
        }
    }
}
