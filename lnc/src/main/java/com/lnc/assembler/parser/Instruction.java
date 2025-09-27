package com.lnc.assembler.parser;

import com.lnc.LNC;
import com.lnc.cc.codegen.CodeElementVisitor;
import com.lnc.common.ExtendedListIterator;
import com.lnc.common.Logger;
import com.lnc.common.frontend.CompileException;
import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.assembler.common.OpcodeMap;
import com.lnc.common.frontend.Token;
import com.lnc.assembler.parser.argument.Argument;
import com.lnc.common.frontend.TokenType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents an instruction within a code structure. An instruction consists of an opcode and any associated arguments.
 * This class is responsible for calculating the size of the instruction, encoding the instruction into machine-readable format,
 * and providing functionality to determine if the instruction is a short jump.
 */
public class Instruction extends CodeElement {
    private final Token opcode;
    private Argument[] arguments;

    private final static Map<String, Integer> IMMEDIATE_ENCODING_ORDER_LOOKUP = Map.of(
            "abs", 0,
            "datap", 1,
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

    public boolean isShortJump() {
        return opcode.type == TokenType.JZ || opcode.type == TokenType.JC || opcode.type == TokenType.JN || opcode.type == TokenType.GOTO;
    }

    @Override
    public byte[] encode(ILabelResolver labelResolver, int instructionAddress) {
        byte[] result = new byte[size(labelResolver)];
        if(isShortJump() && arguments.length == 1 && arguments[0].type == Argument.Type.LABEL){
            try {
                result[0] = OpcodeMap.getOpcode(opcode.lexeme + "_cst");
                byte[] targetBuffer = arguments[0].encode(labelResolver, instructionAddress);

                if((targetBuffer[0] << 8) != (instructionAddress & 0xFF00) && !LNC.settings.get("-Wshort-jump-out-of-range", Boolean.class)){
                    Logger.compileWarning("referenced label in short jump is outside of code segment. Use 'l" + opcode.lexeme.toLowerCase() + "' instead (-Wshort-jump-out-of-range)", arguments[0].token);                }

                result[1] = targetBuffer[1];
            } catch (IndexOutOfBoundsException e) {
                throw new CompileException("failed to encode instruction", opcode);
            }
        }else{
            String immediateInstruction = opcode.lexeme + Stream.of(arguments).map(arg -> arg.getImmediateEncoding(labelResolver)).reduce("", (a, b) -> a + "_" + b);
            if (!OpcodeMap.isValid(immediateInstruction)) {
                throw new CompileException(invalidInstructionMessage(immediateInstruction), opcode);
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
                } catch (IndexOutOfBoundsException e) {
                    throw new CompileException("failed to encode instruction", opcode);
                }
            }

        }

        return result;
    }

    private String invalidInstructionMessage(String immediateInstruction) {
        StringBuilder sb = new StringBuilder("invalid instruction (" + OpcodeMap.toLnasmPseudocode(immediateInstruction) + ").");

        String[] similar = OpcodeMap.getSimilarInstructions(immediateInstruction);

        if (similar.length > 0) {
            sb.append(" Did you mean: \n\n");
            for (int i = 0; i < similar.length; i++) {
                sb.append("\t\t").append(OpcodeMap.toLnasmPseudocode(similar[i])).append("\n");
            }
        }

        sb.append("\n");


        return sb.toString();
    }

    private Argument[] sortArgumentsForEncoding(Argument[] arguments, ILabelResolver labelResolver) {
        // enumerate array to pair element with index
        List<AbstractMap.SimpleEntry<Integer, Argument>> list = new ArrayList<>();

        for (int i = 0; i < arguments.length; i++) {
            list.add(new AbstractMap.SimpleEntry<>(i, arguments[i]));
        }

        return list.stream().sorted((a, b) -> {
            var aValue = (int) IMMEDIATE_ENCODING_ORDER_LOOKUP.getOrDefault(a.getValue().getImmediateEncoding(labelResolver), -1);
            var bValue = (int) IMMEDIATE_ENCODING_ORDER_LOOKUP.getOrDefault(b.getValue().getImmediateEncoding(labelResolver), -1);
            return aValue == bValue ? (a.getKey() - b.getKey()) : aValue - bValue;
        }).map(AbstractMap.SimpleEntry::getValue).toArray(Argument[]::new);
    }

    public Argument[] getArguments() {
        return arguments;
    }

    public Token getOpcode() {
        return opcode;
    }

    @Override
    public <T> T accept(CodeElementVisitor<T> visitor, ExtendedListIterator<CodeElement> iterator) {
        return visitor.visit(this, iterator);
    }

    @Override
    public String toString() {
        return opcode.lexeme.toLowerCase() + " " +
                Arrays.stream(arguments)
                        .map(Argument::toString)
                        .collect(Collectors.joining(", "));
    }

    public void setArguments(Argument[] arguments) {
        this.arguments = arguments;
    }
}
