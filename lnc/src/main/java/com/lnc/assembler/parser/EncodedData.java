package com.lnc.assembler.parser;

import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.assembler.parser.argument.Byte;
import com.lnc.assembler.parser.argument.NumericalArgument;
import com.lnc.cc.codegen.CodeElementVisitor;
import com.lnc.common.ExtendedListIterator;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * EncodedData represents a specific implementation of CodeElement that holds
 * raw binary data. This class enables encoding, traversal, and interaction
 * with data elements in an assembly or similar low-level structure.
 *
 * EncodedData is immutable, encapsulating a byte array that cannot be modified
 * once the object is created.
 */
public class EncodedData extends CodeElement{

    protected final NumericalArgument[] data;

    protected EncodedData(NumericalArgument[] data) {
        this.data = data;
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        return Arrays.stream(data).mapToInt(arg -> arg.size(sectionLocator)).sum();
    }

    @Override
    public byte[] encode(ILabelResolver labelResolver, int instructionAddress) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for(NumericalArgument arg : data){
            try {
                baos.write(arg.encode(labelResolver, instructionAddress));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return baos.toByteArray();
    }

    public static EncodedData of(byte[] data){
        List<NumericalArgument> args = new ArrayList<>();
        for(byte datum : data){
            args.add(new Byte(Token.__internal(TokenType.INTEGER, (int) datum)));
        }

        return new EncodedData(args.toArray(NumericalArgument[]::new));
    }

    public static EncodedData ofString(String string) {

        // Allocate an array with an extra byte for the null terminator
        byte[] data = Arrays.copyOf(string.getBytes(), string.length() + 1);
        // Null-terminate the string
        data[string.length()] = 0;

        return of(data);
    }

    @Override
    public <T> T accept(CodeElementVisitor<T> visitor, ExtendedListIterator<CodeElement> iterator) {
        return visitor.visit(this, iterator);
    }

    @Override
    public String toString() {
        StringBuilder val = new StringBuilder(".data ");

        for (NumericalArgument datum : data) {
            val.append(datum.toString()).append(" ");
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
