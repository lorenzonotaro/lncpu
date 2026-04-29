package com.lnc.cc.ast;

import com.lnc.common.frontend.CompileException;
import com.lnc.common.frontend.Token;

public class BinaryExpression extends Expression {
    public Expression left;
    public Expression right;

    public Operator operator;

    public BinaryExpression(Expression left, Expression right, Token opToken) {
        super(Expression.Type.BINARY, opToken);
        this.left = left;
        this.right = right;
        this.operator = Operator.fromTokenType(opToken);
    }

    @Override
    public <E> E accept(IExpressionVisitor<E> visitor) {
        return visitor.visit(this);
    }

    public enum Operator {
        ADD(true, false),
        SUB(false, false),
        MUL(true, false),
        DIV(false, false),
        AND(true, false),
        OR(true, false),
        XOR(true, false),
        EQ(true, true),
        NE(true, true),
        SHL(false, true),
        SHR(false, true),
        LT(false, true),
        GT(false, true),
        LE(false, true),
        GE(false, true);

        private final boolean commutative;

        private final boolean comparison;

        Operator(){
            this(false, false);
        }

        Operator(boolean commutative, boolean comparison) {
            this.commutative = commutative;
            this.comparison = comparison;
        }

        public static Operator fromTokenType(Token token) {
            return switch (token.type) {
                case PLUS, PLUS_EQUALS -> ADD;
                case MINUS, MINUS_EQUALS -> SUB;
                case STAR -> MUL;
                case SLASH -> DIV;
                case AMPERSAND, BITWISE_AND_EQUALS -> AND;
                case BITWISE_OR, BITWISE_OR_EQUALS -> OR;
                case BITWISE_XOR, BITWISE_XOR_EQUALS -> XOR;
                case DOUBLE_EQUALS -> EQ;
                case NOT_EQUALS -> NE;
                case LESS_THAN -> LT;
                case GREATER_THAN -> GT;
                case LESS_THAN_OR_EQUAL -> LE;
                case GREATER_THAN_OR_EQUAL -> GE;
                case BITWISE_LEFT, BITWISE_SHIFT_LEFT_EQUALS -> SHL;
                case BITWISE_RIGHT, BITWISE_SHIFT_RIGHT_EQUALS -> SHR;
                default -> throw new CompileException("invalid binary operator: " + token, token);
            };
        }

        public boolean isCommutative() {
            return commutative;
        }

        public boolean isComparison() {
            return comparison;
        }
    }
}
