package com.lnc.assembler.linker;

// converts a compiled binary to an immediate representation

import com.lnc.common.Logger;
import com.lnc.common.frontend.CompileException;
import com.lnc.assembler.common.OpcodeMap;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class Disassembler{

    private final Map<Integer, String> reverseLookupLabels;

    private final List<SectionBuilder.Descriptor> sectionDescriptors;

    private final int longestLabelLength;
    private byte[] output;

    public Disassembler(Map<Integer, Set<String>> reverseSymbolTable, List<SectionBuilder.Descriptor> sectionDescriptors) {
        this.sectionDescriptors = sectionDescriptors;

        // collect each set in the reverseSymbolTable into a comma-separated string, with ': ' at the end
        this.reverseLookupLabels = reverseSymbolTable.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().collect(Collectors.joining(", ", "", ": "))));

        // find the longest label length
        this.longestLabelLength = reverseLookupLabels.values().stream().mapToInt(String::length).max().orElse(1);
    }

    public boolean disassemble(byte[] output) {
        try{
            StringBuilder sb = new StringBuilder();
            int currentAddress = 0;
            for (var descriptor : sectionDescriptors) {
                sb.append("######## Section '%s', origin at 0x%06x (size = 0x0%04x)\n\n".formatted(descriptor.sectionInfo().getName(), descriptor.start(), descriptor.length()));
                currentAddress = descriptor.start();
                while(currentAddress < descriptor.start() + descriptor.length()){
                    String labels = reverseLookupLabels.getOrDefault(currentAddress, "");

                    byte opcode = output[currentAddress];

                    String opCodeImmediate;
                    int instructionLength;

                    try{
                        opCodeImmediate = OpcodeMap.getImmediateName(opcode);
                        instructionLength = OpcodeMap.getCodeLength(opCodeImmediate);
                    }catch(IllegalArgumentException e){
                        throw new LinkException("invalid opcode: %02x".formatted(opcode));
                    }

                    String instructionDesc = "%02x (%s)".formatted(opcode, opCodeImmediate);

                    StringBuilder params = new StringBuilder();

                    for (int i = 1; i < instructionLength; i++) {
                        if(currentAddress + i >= output.length){
                            params.append("XX ");
                        }else {
                            params.append("%02x ".formatted(output[currentAddress + i]));
                        }
                    }

                    sb.append(("\t%" + longestLabelLength + "s %06x:\t%-20s %16s\n").formatted(labels, currentAddress, instructionDesc, params));

                    currentAddress += instructionLength;
                }

                sb.append("\n\n");
            }

            this.output = sb.toString().getBytes(StandardCharsets.UTF_8);

            return true;
        }catch(CompileException e){
            e.log();
        }catch(LinkException e){
            e.log();
        }catch(Exception e){
            Logger.error("An unexpected error occurred while disassembling the binary: %s".formatted(e.getMessage()));
        }
        return false;
    }

    public byte[] getOutput(){
        return output;
    }
}
