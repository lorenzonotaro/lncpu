package com.lnc.assembler.parser;

import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.assembler.linker.LinkInfo;
import com.lnc.cc.codegen.CodeElementVisitor;
import com.lnc.common.ExtendedListIterator;

import java.util.Arrays;

public class EncodedData extends CodeElement{

    private final byte[] data;

    private EncodedData(byte[] data) {
        this.data = data;
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        return data.length;
    }

    @Override
    public byte[] encode(ILabelResolver labelResolver, LinkInfo linkInfo, int instructionAddress) {
        return data;
    }

    public static EncodedData of(byte[] data){
        return new EncodedData(data);
    }

    public static EncodedData ofString(String string) {

        // Allocate an array with an extra byte for the null terminator
        byte[] data = Arrays.copyOf(string.getBytes(), string.length() + 1);
        // Null-terminate the string
        data[string.length()] = 0;

        return new EncodedData(data);
    }

    @Override
    public <T> T accept(CodeElementVisitor<T> visitor, ExtendedListIterator<CodeElement> iterator) {
        return visitor.visit(this, iterator);
    }

    @Override
    public String toString() {
        StringBuilder val = new StringBuilder(".data ");

        for (byte datum : data) {
            val.append(String.format("0x%02x ", datum & 0xFF));
        }
        return val.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        EncodedData that = (EncodedData) o;
        return Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
