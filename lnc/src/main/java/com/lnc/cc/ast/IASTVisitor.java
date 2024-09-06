package com.lnc.cc.ast;

public interface IASTVisitor<S, E> extends IStatementVisitor<S>, IExpressionVisitor<E> { }
