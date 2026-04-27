package com.lnc.cc.ast;

import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.frontend.Token;

public class VaPopExpression extends Expression {
    public VaPopExpression(Token previous, TypeSpecifier typeSpecifier) {
        super(Type.VA_POP, previous);
        setTypeSpecifier(typeSpecifier);
    }

    @Override
    public <E> E accept(IExpressionVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
