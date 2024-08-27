package com.lnc.assembler.parser;

import com.lnc.LNC;
import com.lnc.common.Logger;
import com.lnc.common.frontend.CompileException;
import com.lnc.assembler.linker.ILabelResolver;
import com.lnc.assembler.linker.ILabelSectionLocator;
import com.lnc.assembler.common.OpcodeMap;
import com.lnc.common.frontend.Token;
import com.lnc.assembler.linker.LinkInfo;
import com.lnc.assembler.parser.argument.Argument;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class Instruction extends CodeElement {
    private final Token opcode;
    private final Argument[] arguments;

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

    private boolean isShortJump() {
        return opcode.type == Token.Type.JZ || opcode.type == Token.Type.JC || opcode.type == Token.Type.JN || opcode.type == Token.Type.GOTO;
    }

    @Override
    public byte[] encode(ILabelResolver labelResolver, LinkInfo linkInfo, int instructionAddress) {
        byte[] result = new byte[size(labelResolver)];
        if(isShortJump() && arguments.length == 1 && arguments[0].type == Argument.Type.LABEL){
            try {
                result[0] = OpcodeMap.getOpcode(opcode.lexeme + "_cst");
                byte[] targetBuffer = arguments[0].encode(labelResolver, linkInfo, instructionAddress);

                if((targetBuffer[0] << 8) != (instructionAddress & 0xFF00) && !LNC.settings.get("-Wshort-jump-out-of-range", Boolean.class)){
                    Logger.compileWarning("referenced label in short jump is outside of code segment. Use 'l" + opcode.lexeme.toLowerCase() + "' instead (-Wshort-jump-out-of-range)", arguments[0].token);                }

                result[1] = targetBuffer[1];
            } catch (IOException | IndexOutOfBoundsException e) {
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
                        byte[] arg = argument.encode(labelResolver, linkInfo, instructionAddress);
                        for (byte b : arg) {
                            result[offset++] = b;
                        }
                    }
                } catch (IOException | IndexOutOfBoundsException e) {
                    throw new CompileException("failed to encode instruction", opcode);
                }
            }

            if(immediateInstruction.startsWith("mov_") && immediateInstruction.endsWith("_ds")){
                linkInfo.dsSet = true;
            }else if(immediateInstruction.startsWith("mov_") && immediateInstruction.endsWith("_ss")){
                linkInfo.ssSet = true;
            }

            
            if(immediateInstruction.contains("datap") && !linkInfo.dsSet && !LNC.settings.get("-Wdata-page-access-before-setup", Boolean.class)){
                Logger.compileWarning("data page addressing mode before setting up DS register", opcode);
            }


            if((immediateInstruction.startsWith("push") || immediateInstruction.startsWith("pop") || immediateInstruction.startsWith("lcall") || immediateInstruction.startsWith("ret") || immediateInstruction.startsWith("iret")) &&
            !linkInfo.ssSet && !LNC.settings.get("-Wstack-access-before-setup", Boolean.class)){
                Logger.compileWarning("stack access before setting up SS register", opcode);
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


}
