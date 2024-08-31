package com.lnc.cc.ast;

import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;

public class BinaryExpression extends Expression {
    private final Expression left;
    private final Expression right;

    private final Operator operator;

    public BinaryExpression(Expression left, Expression right, Operator operator) {
        super(Expression.Type.BINARY);
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    public enum Operator {
        ADD,
        SUB,
        MUL,
        DIV,
        AND,
        OR,
        XOR,
        EQ,
        NE,
        LT,
        GT,
        LE,
        GE;

        public static Operator fromTokenType(Token token) {
            return switch (token.type) {
                case PLUS -> ADD;
                case MINUS -> SUB;
                case STAR -> MUL;
                case SLASH -> DIV;
                case AMPERSAND -> AND;
                case BITWISE_OR -> OR;
                case BITWISE_XOR -> XOR;
                case DOUBLE_EQUALS -> EQ;
                case NOT_EQUALS -> NE;
                case LESS_THAN -> LT;
                case GREATER_THAN -> GT;
                case LESS_THAN_OR_EQUAL -> LE;
                case GREATER_THAN_OR_EQUAL -> GE;
                default -> throw new CompileException("invalid binary operator: " + token, token);
            };
        }
    }
}
