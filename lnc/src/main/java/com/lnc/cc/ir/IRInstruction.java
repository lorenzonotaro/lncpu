package com.lnc.cc.ir;

public abstract class IRInstruction {
    public abstract <E> E accept(IIRVisitor<E> visitor);
}
