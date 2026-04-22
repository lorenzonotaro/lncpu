package com.lnc.cc.ast;

import com.lnc.cc.types.TypeSpecifier;
import com.lnc.common.frontend.Token;

public class SizeofExpression extends Expression{
    public enum TargetType {
        TYPE,
        EXPRESSION
    }

    public final TargetType targetType;
    public final TypeSpecifier type;
    public final Expression expression;

    public SizeofExpression(Token token, TypeSpecifier typeSpecifier) {
        super(Type.SIZEOF, token);
        this.targetType = TargetType.TYPE;
        this.type = typeSpecifier;
        this.expression = null;
    }
    public SizeofExpression(Token token, Expression expression) {
        super(Type.SIZEOF, token);
        this.targetType = TargetType.EXPRESSION;
        this.type = null;
        this.expression = expression;
    }

    @Override
    public <E> E accept(IExpressionVisitor<E> visitor) {
        return visitor.visit(this);
    }
}
