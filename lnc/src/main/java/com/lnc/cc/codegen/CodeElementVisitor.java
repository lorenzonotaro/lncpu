package com.lnc.cc.codegen;

import com.lnc.assembler.parser.CodeElement;
import com.lnc.assembler.parser.EncodedData;
import com.lnc.assembler.parser.Instruction;
import com.lnc.assembler.parser.argument.ReservedSpace;
import com.lnc.common.ExtendedListIterator;

public interface CodeElementVisitor<T> {
    T visit(EncodedData encodedData, ExtendedListIterator<CodeElement> iterator);

    T visit(ReservedSpace reservedSpace, ExtendedListIterator<CodeElement> iterator);

    T visit(Instruction instruction, ExtendedListIterator<CodeElement> iterator);

}
