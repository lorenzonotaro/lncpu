package com.lnc.cc.ast;

import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;

public class BinaryExpression extends Expression {
    public final Expression left;
    public final Expression right;

    public final Operator operator;

    public BinaryExpression(Expression left, Expression right, Token opToken) {
        super(Expression.Type.BINARY, opToken);
        this.left = left;
        this.right = right;
        this.operator = Operator.fromTokenType(opToken);
    }

    @Override
    public <E> E accept(IExpressionVisitor<E> visitor) {
        return visitor.accept(this);
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
