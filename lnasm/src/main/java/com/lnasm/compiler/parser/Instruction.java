package com.lnasm.compiler.parser;

import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.linker.ILabelResolver;
import com.lnasm.compiler.linker.ILabelSectionLocator;
import com.lnasm.compiler.common.OpcodeMap;
import com.lnasm.compiler.common.Token;
import com.lnasm.compiler.parser.argument.Argument;
import com.lnasm.io.ByteArrayChannel;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.stream.Stream;

public class Instruction extends CodeElement {
    private final Token opcode;
    private final Argument[] arguments;

    public Instruction(Token opcode, Argument[] arguments) {
        this.opcode = opcode;
        this.arguments = arguments;
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        if(isShortJump() && arguments.length == 1 && arguments[0].type == Argument.Type.LABEL){
            return 2;
        } else {
            return 1 + Stream.of(arguments).mapToInt(arg -> arg.size(sectionLocator)).sum();
        }
    }

    private boolean isShortJump() {
        return opcode.type == Token.Type.JZ || opcode.type == Token.Type.JC || opcode.type == Token.Type.JN || opcode.type == Token.Type.GOTO;
    }

    @Override
    public void encode(ILabelResolver labelResolver, WritableByteChannel channel, int instructionAddress) {
        if(isShortJump() && arguments.length == 1 && arguments[0].type == Argument.Type.LABEL){
            try {
                channel.write(ByteBuffer.allocateDirect(OpcodeMap.getOpcode(opcode.lexeme + "_cst")));
                try(ByteArrayChannel innerChannel = new ByteArrayChannel(2, false)){
                    arguments[0].encode(labelResolver, channel, instructionAddress);
                    byte[] innerBytes = innerChannel.toByteArray();
                    channel.write(ByteBuffer.wrap(Arrays.copyOfRange(innerBytes, 1, 2)));
                }
            } catch (Exception e) {
                throw new CompileException("failed to encode instruction", opcode);
            }
        }else{
            String immediateInstruction = opcode.lexeme + Stream.of(arguments).map(arg -> arg.getImmediateEncoding(labelResolver)).reduce("", (a, b) -> a + "_" + b);
            if (!OpcodeMap.isValid(immediateInstruction)) {
                throw new CompileException("invalid instruction (" + immediateInstruction + ")", opcode);
            } else {
                try {
                    channel.write(ByteBuffer.allocateDirect(OpcodeMap.getOpcode(immediateInstruction)));
                    //encode the arguments, in reverse order
                    for (int i = arguments.length - 1; i >= 0; i--) {
                        arguments[i].encode(labelResolver, channel, instructionAddress);
                    }
                } catch (Exception e) {
                    throw new CompileException("failed to encode instruction", opcode);
                }
            }
        }
    }
}
