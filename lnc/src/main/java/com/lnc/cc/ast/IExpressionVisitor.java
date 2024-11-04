package com.lnc.cc.ast;

public interface IExpressionVisitor<E> {
    E accept(AssignmentExpression assignmentExpression);

    E accept(BinaryExpression binaryExpression);

    E accept(CallExpression callExpression);

    E accept(IdentifierExpression identifierExpression);

    E accept(MemberAccessExpression memberAccessExpression);

    E accept(NumericalExpression numericalExpression);

    E accept(StringExpression stringExpression);

    E accept(SubscriptExpression subscriptExpression);

    E accept(UnaryExpression unaryExpression);
}
