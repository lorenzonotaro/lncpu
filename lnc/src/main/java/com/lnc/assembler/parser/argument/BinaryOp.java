package com.lnc.assembler.parser.argument;

import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;
import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;

/**
 * Represents a binary operation between two arguments, such as addition, subtraction,
 * multiplication, or division. Each BinaryOp consists of a left operand, a right operand,
 * and an operator specifying the type of binary operation to perform.
 *
 * A BinaryOp enforces that both the left and right arguments are instances of
 * NumericalArgument. If the arguments do not conform to this constraint, a
 * CompileException is thrown at instantiation.
 *
 * This class provides functionality to calculate the result of the binary operation,
 * encode its operands into binary format, and determine its size in a specific encoding
 * context.
 *
 * The available operators for binary operations are represented in the nested
 * Operator enum and include addition (ADD), subtraction (SUB), multiplication (MUL),
 * division (DIV), bitwise shifts (SHL, SHR), and bitwise operations (AND, OR, XOR).
 */
public class BinaryOp extends NumericalArgument{

    public final Argument left, right;

    public final Operator operator;

    public BinaryOp(Argument left, Argument right, Token token){
        super(token, Type.BINARY_OP);
        this.left = left;
        this.right = right;

        if(!(left instanceof NumericalArgument && right instanceof NumericalArgument)){
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
            case AMPERSAND -> Operator.AND;
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
    public byte[] encode(ILabelResolver labelResolver, int instructionAddress) {

        byte[] leftBytes = left.encode(labelResolver, instructionAddress);
        byte[] rightBytes = right.encode(labelResolver, instructionAddress);

        long result = getResult(leftBytes, rightBytes);

        encode(leftBytes, (int) result);

        return leftBytes;
    }

    private long getResult(byte[] leftBytes, byte[] rightBytes) {
        long leftVal = decode(leftBytes);
        long rightVal = decode(rightBytes);

        return switch (operator) {
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
    public String toString() {
        return "(%s %s %s)".formatted(left, operator.name, right);
    }

    @Override
    public String getImmediateEncoding(ILabelSectionLocator sectionLocator) {
        return left.getImmediateEncoding(sectionLocator);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BinaryOp other)) return false;

        return left.equals(other.left) && right.equals(other.right) && operator == other.operator;
    }

    @Override
    public int value(ILabelResolver labelResolver, int instructionAddress) {
        byte[] leftBytes = left.encode(labelResolver, instructionAddress);
        byte[] rightBytes = right.encode(labelResolver, instructionAddress);

        return (int) getResult(leftBytes, rightBytes);
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
