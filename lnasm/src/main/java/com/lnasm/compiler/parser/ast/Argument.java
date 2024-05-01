package com.lnasm.compiler.parser.ast;

import com.lnasm.compiler.common.*;
import com.lnasm.compiler.parser.RegisterId;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public abstract class Argument implements IEncodeable {
    public final Token token;
    public final Type type;
    protected Argument(Token token, Type type) {
        this.token = token;
        this.type = type;
    }
    public abstract String getImmediateEncoding();

    public static class Register extends Argument{
        final RegisterId reg;

        public Register(Token token) {
            super(token, Type.REGISTER);
            this.reg = RegisterId.fromString(token.lexeme);
        }


        @Override
        public int size(ILabelSectionLocator sectionLocator) {
            return 0;
        }

        @Override
        public void encode(ILabelResolver labelResolver, WritableByteChannel channel){
            // do nothing
        }

        @Override
        public String getImmediateEncoding() {
            return reg.toString();
        }
    }

    public static class Constant extends Argument{
        final byte value;

        public Constant(Token token) {
            super(token, Type.CONSTANT);
            this.value = token.getValueAsByte();
        }

        @Override
        public int size(ILabelSectionLocator sectionLocator) {
            return 1;
        }

        @Override
        public void encode(ILabelResolver labelResolver, WritableByteChannel channel) throws IOException {
            channel.write(ByteBuffer.wrap(new byte[]{value}));
        }

        @Override
        public String getImmediateEncoding() {
            return "cst";
        }
    }

    public static class Word extends Argument{
        final short value;

        public Word(Token token) {
            super(token, Type.WORD);
            this.value = token.getValueAsShort();
        }

        @Override
        public int size(ILabelSectionLocator sectionLocator) {
            return 2;
        }

        @Override
        public void encode(ILabelResolver labelResolver, WritableByteChannel channel) throws IOException {
            ByteBuffer buffer = ByteBuffer.allocate(2);
            buffer.putShort(value);
            channel.write(buffer);
        }

        @Override
        public String getImmediateEncoding() {
            return "wcst";
        }
    }

    public static class LabelRef extends Argument{
        final String label;

        public LabelRef(Token token) {
            super(token, Type.LABEL);
            this.label = token.lexeme;
        }

        @Override
        public int size(ILabelSectionLocator sectionLocator) {
            return sectionLocator.getSectionName(label).type == SectionType.PAGE0 ? 1 : 2;
        }

        @Override
        public void encode(ILabelResolver labelResolver, WritableByteChannel channel) throws IOException {
            short address = labelResolver.resolve(label);
            ByteBuffer buffer = ByteBuffer.allocate(2);
            buffer.putShort(address);
            channel.write(buffer);
        }

        @Override
        public String getImmediateEncoding() {
            return "wcst";
        }
    }

    public static class Absolute extends Argument{
        final short address;

        public Absolute(Token token) {
            super(token, Type.ABSOLUTE);
            this.address = token.getValueAsShort();
        }

        @Override
        public int size(ILabelSectionLocator sectionLocator) {
            return 2;
        }

        @Override
        public void encode(ILabelResolver labelResolver, WritableByteChannel channel) throws IOException {
            ByteBuffer buffer = ByteBuffer.allocate(2);
            buffer.putShort(address);
            channel.write(buffer);
        }

        @Override
        public String getImmediateEncoding() {
            return "abs";
        }
    }

    public static class IPage0 extends Argument{
        final byte address;

        public IPage0(Token token) {
            super(token, Type.IPAGE0);
            this.address = token.getValueAsByte();
        }

        @Override
        public int size(ILabelSectionLocator sectionLocator) {
            return 1;
        }

        @Override
        public void encode(ILabelResolver labelResolver, WritableByteChannel channel) throws IOException {
            ByteBuffer buffer = ByteBuffer.allocate(1);
            buffer.put(address);
            channel.write(buffer);
        }

        @Override
        public String getImmediateEncoding() {
            return "ipage0";
        }
    }

    public static class IPage0Rd extends Argument{
        final byte address;

        public IPage0Rd(Token token) {
            super(token, Type.IPAGE0RD);
            this.address = token.getValueAsByte();
        }

        @Override
        public int size(ILabelSectionLocator sectionLocator) {
            return 1;
        }

        @Override
        public void encode(ILabelResolver labelResolver, WritableByteChannel channel) throws IOException {
            ByteBuffer buffer = ByteBuffer.allocate(1);
            buffer.put(address);
            channel.write(buffer);
        }

        @Override
        public String getImmediateEncoding() {
            return "ipage0rd";
        }
    }

    public static class IFullRd extends Argument{

        public IFullRd(Token token) {
            super(token, Type.IFULLRCRD);
        }

        @Override
        public int size(ILabelSectionLocator sectionLocator) {
            return 0;
        }

        @Override
        public void encode(ILabelResolver labelResolver, WritableByteChannel channel) throws IOException {
        }

        @Override
        public String getImmediateEncoding() {
            return "ifullrcrd";
        }
    }

    public enum Type {
        REGISTER,
        CONSTANT,
        IPAGE0,
        IPAGE0RD,
        IFULLRCRD,
        ABSOLUTE,
        LABEL,
        WORD,
    }

}
