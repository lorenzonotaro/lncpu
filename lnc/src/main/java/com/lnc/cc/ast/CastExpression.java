package com.lnc.cc.ast;

import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.frontend.Token;

public class CastExpression extends Expression {
    public final TypeSpecifier targetType;
    public final Expression operand;

    public CastExpression(Token token, TypeSpecifier targetType, Expression operand) {
        super(Type.CAST, token);
        this.targetType = targetType;
        this.operand = operand;
        this.typeSpecifier = targetType;
    }

    @Override
    public <E> E accept(IExpressionVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
