package com.lnasm.compiler.parser;

import com.lnasm.LNASM;
import com.lnasm.Logger;
import com.lnasm.compiler.common.CompileException;
import com.lnasm.compiler.linker.ILabelResolver;
import com.lnasm.compiler.linker.ILabelSectionLocator;
import com.lnasm.compiler.common.OpcodeMap;
import com.lnasm.compiler.common.Token;
import com.lnasm.compiler.parser.argument.Argument;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Instruction extends CodeElement {
    private final Token opcode;
    private final Argument[] arguments;

    private final static Map<String, Integer> IMMEDIATE_ENCODING_ENCODING_LOOKUP = Map.of(
            "abs", 0,
            "page0", 1,
            "dcst", 2,
            "cst", 3

    );

    private int size = -1;

    public Instruction(Token opcode, Argument[] arguments) {
        this.opcode = opcode;
        this.arguments = arguments;
    }

    @Override
    public int size(ILabelSectionLocator sectionLocator) {
        if(size != -1){
            return size;
        }

        if(isShortJump() && arguments.length == 1 && arguments[0].type == Argument.Type.LABEL){
            return size = 2;
        } else {
            return size = 1 + Stream.of(arguments).mapToInt(arg -> arg.size(sectionLocator)).sum();
        }
    }

    private boolean isShortJump() {
        return opcode.type == Token.Type.JZ || opcode.type == Token.Type.JC || opcode.type == Token.Type.JN || opcode.type == Token.Type.GOTO;
    }

    @Override
    public byte[] encode(ILabelResolver labelResolver, int instructionAddress) {
        byte[] result = new byte[size(labelResolver)];
        if(isShortJump() && arguments.length == 1 && arguments[0].type == Argument.Type.LABEL){
            try {
                result[0] = OpcodeMap.getOpcode(opcode.lexeme + "_cst");
                byte[] targetBuffer = arguments[0].encode(labelResolver, instructionAddress);

                if((targetBuffer[0] << 8) != (instructionAddress & 0xFF00) && !LNASM.settings.get("-Wshort-jump-out-of-range", Boolean.class)){
                    Logger.compileWarning("referenced label in short jump is outside of code segment. Use 'l" + opcode.lexeme.toLowerCase() + "' instead (-Wshort-jump-out-of-range)", arguments[0].token);                }

                result[1] = targetBuffer[1];
            } catch (IOException | IndexOutOfBoundsException e) {
                throw new CompileException("failed to encode instruction", opcode);
            }
        }else{
            String immediateInstruction = opcode.lexeme + Stream.of(arguments).map(arg -> arg.getImmediateEncoding(labelResolver)).reduce("", (a, b) -> a + "_" + b);
            if (!OpcodeMap.isValid(immediateInstruction)) {
                throw new CompileException("invalid instruction (" + immediateInstruction + ")", opcode);
            } else {
                try {
                    result[0] = OpcodeMap.getOpcode(immediateInstruction);
                    int offset = 1;
                    for (Argument argument : sortArgumentsForEncoding(arguments, labelResolver)) {
                        byte[] arg = argument.encode(labelResolver, instructionAddress);
                        for (byte b : arg) {
                            result[offset++] = b;
                        }
                    }
                } catch (IOException | IndexOutOfBoundsException e) {
                    throw new CompileException("failed to encode instruction", opcode);
                }
            }
        }

        return result;
    }

    private Argument[] sortArgumentsForEncoding(Argument[] arguments, ILabelResolver labelResolver) {
        // enumerate array to pair element with index
        List<AbstractMap.SimpleEntry<Integer, Argument>> list = new ArrayList<>();

        for (int i = 0; i < arguments.length; i++) {
            list.add(new AbstractMap.SimpleEntry<>(i, arguments[i]));
        }

        return list.stream().sorted((a, b) -> {
            var aValue = (int) IMMEDIATE_ENCODING_ENCODING_LOOKUP.getOrDefault(a.getValue().getImmediateEncoding(labelResolver), -1);
            var bValue = (int) IMMEDIATE_ENCODING_ENCODING_LOOKUP.getOrDefault(b.getValue().getImmediateEncoding(labelResolver), -1);
            return aValue == bValue ? (a.getKey() - b.getKey()) : aValue - bValue;
        }).map(AbstractMap.SimpleEntry::getValue).toArray(Argument[]::new);
    }


}
