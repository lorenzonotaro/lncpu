package com.lnc.cc.ir;

import java.util.Set;

public interface IReferenceable {
    Set<IRInstruction> getReads();

    Set<IRInstruction> getWrites();

    void addRead(IRInstruction instruction);

    void addWrite(IRInstruction instruction);

    boolean removeRead(IRInstruction instruction);

    boolean removeWrite(IRInstruction instruction);
}
