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
        ADD("+", Type.ARITHMETIC_LOGICAL, true),
        SUB("-", Type.ARITHMETIC_LOGICAL, false),
        MUL("*", Type.ARITHMETIC_LOGICAL, true),
        DIV("/", Type.ARITHMETIC_LOGICAL, false),
        AND("&", Type.ARITHMETIC_LOGICAL, true),
        OR("|", Type.ARITHMETIC_LOGICAL, true),
        XOR("^", Type.ARITHMETIC_LOGICAL, true),
        EQ("==", Type.COMPARISON, true),
        NE("!=", Type.COMPARISON, true),
        SHL("<<", Type.ARITHMETIC_LOGICAL, false),
        SHR(">>", Type.ARITHMETIC_LOGICAL, false),
        LT("<", Type.COMPARISON, false),
        GT(">", Type.COMPARISON, false),
        LE("<=", Type.COMPARISON, false),
        GE(">=", Type.COMPARISON, false);

        public enum Type {
            ARITHMETIC_LOGICAL,
            COMPARISON
        };
        public final Type type;

        private final String str;
        private final boolean commutative;


        Operator(String str, Type type, boolean commutative) {
            this.str = str;
            this.commutative = commutative;
            this.type = type;
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

        @Override
        public String toString() {
            return str;
        }
    }
}
