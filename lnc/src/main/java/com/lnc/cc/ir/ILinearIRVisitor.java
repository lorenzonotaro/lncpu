package com.lnc.cc.ir;

public interface ILinearIRVisitor<E, T> extends IIRVisitor<E, T>{
    @Override
    E visit(Label label);
}
