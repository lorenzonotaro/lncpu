package com.lnc.cc.ast;

import com.lnc.common.frontend.Token;

public class UnaryExpression extends Expression {
    public final Expression expression;
    public final Operator operator;
    public final Associativity associativity;

    public UnaryExpression(Expression expression, Operator operator, Associativity associativity) {
        super(Expression.Type.UNARY);
        this.expression = expression;
        this.operator = operator;
        this.associativity = associativity;
    }

    public enum Operator {
        NEGATE,
        NOT,
        DEREFERENCE,
        ADDRESS_OF,
        INCREMENT,
        DECREMENT;

        public static Operator fromTokenType(Token token) {
            return switch (token.type) {
                case MINUS -> NEGATE;
                case LOGICAL_NOT, BITWISE_NOT -> NOT;
                case STAR -> DEREFERENCE;
                case AMPERSAND -> ADDRESS_OF;
                case DOUBLE_PLUS -> INCREMENT;
                case DOUBLE_MINUS -> DECREMENT;
                default -> throw new IllegalArgumentException("Invalid token type for unary operator: " + token.type);
            };
        }
    }

    public enum Associativity {
        LEFT,
        RIGHT
    }
}
