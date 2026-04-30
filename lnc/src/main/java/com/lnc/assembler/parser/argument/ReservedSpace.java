package com.lnc.assembler.parser.argument;

import com.lnc.assembler.common.IEncodeable;
import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.assembler.parser.CodeElement;
import com.lnc.assembler.parser.EncodedData;
import com.lnc.cc.codegen.CodeElementVisitor;
import com.lnc.common.ExtendedListIterator;

public class ReservedSpace extends CodeElement {
    private final int length;

    public ReservedSpace(int length){
        this.length = length;
    }

    public static CodeElement of(int size) {
        return new ReservedSpace(size);
    }

    @Override
    public <T> T accept(CodeElementVisitor<T> visitor, ExtendedListIterator<CodeElement> iterator) {
        return visitor.visit(this, iterator);
    }

    public String toString(){
        return ".res " + this.length;
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        return length;
    }

    @Override
    public byte[] encode(ILabelResolver labelResolver, int instructionAddress) {
        return new byte[length];
    }
}
