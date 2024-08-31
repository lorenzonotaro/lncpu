package com.lnc.cc.ast;

import com.lnc.common.frontend.Token;

public class MemberAccessExpression extends Expression {
    public final Expression left;
    private final Token right;
    private final Token accessOperator;

    public MemberAccessExpression(Expression left, Token right, Token accessOperator) {
        super(Type.MEMBER_ACCESS);
        this.left = left;
        this.right = right;
        this.accessOperator = accessOperator;
    }

}
