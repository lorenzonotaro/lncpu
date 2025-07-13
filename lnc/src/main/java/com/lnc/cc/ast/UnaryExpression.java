package com.lnc.cc.ast;

import com.lnc.common.frontend.Token;

public class    UnaryExpression extends Expression {
    public final Expression operand;
    public final Operator operator;
    public final UnaryPosition unaryPosition;

    public UnaryExpression(Expression operand, Token token, UnaryPosition unaryPosition) {
        super(Expression.Type.UNARY, token);
        this.operand = operand;
        this.operator = Operator.fromTokenType(token);
        this.unaryPosition = unaryPosition;
    }

    @Override
    public <E> E accept(IExpressionVisitor<E> visitor) {
        return visitor.visit(this);
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

    public enum UnaryPosition {
        PRE,
        POST
    }
}
