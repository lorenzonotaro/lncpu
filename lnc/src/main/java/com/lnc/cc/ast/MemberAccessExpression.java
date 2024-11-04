package com.lnc.cc.ast;

import com.lnc.common.frontend.Token;

public class MemberAccessExpression extends Expression {
    public final Expression left;
    public final Token right;
    public final Token accessOperator;

    public MemberAccessExpression(Expression left, Token right, Token accessOperator) {
        super(Type.MEMBER_ACCESS, accessOperator);
        this.left = left;
        this.right = right;
        this.accessOperator = accessOperator;
    }

    @Override
    public <E> E accept(IExpressionVisitor<E> visitor) {
        return visitor.accept(this);
    }
}
