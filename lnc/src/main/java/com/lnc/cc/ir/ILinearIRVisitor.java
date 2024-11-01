package com.lnc.cc.ir;

public interface ILinearIRVisitor<E> extends IIRVisitor<E>{
    @Override
    E accept(Label label);
}
