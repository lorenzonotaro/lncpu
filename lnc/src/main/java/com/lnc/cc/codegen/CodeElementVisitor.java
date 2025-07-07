package com.lnc.cc.codegen;

import com.lnc.assembler.parser.EncodedData;
import com.lnc.assembler.parser.Instruction;

public interface CodeElementVisitor<T> {
    T visit(EncodedData encodedData);

    T visit(Instruction instruction);
}
