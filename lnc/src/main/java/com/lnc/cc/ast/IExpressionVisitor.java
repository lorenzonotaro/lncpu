package com.lnc.cc.ast;

public interface IExpressionVisitor<E> {
    E visit(AssignmentExpression assignmentExpression);

    E visit(BinaryExpression binaryExpression);

    E visit(CallExpression callExpression);

    E visit(IdentifierExpression identifierExpression);

    E visit(MemberAccessExpression memberAccessExpression);

    E visit(NumericalExpression numericalExpression);

    E visit(StringExpression stringExpression);

    E visit(SubscriptExpression subscriptExpression);

    E visit(UnaryExpression unaryExpression);
}
