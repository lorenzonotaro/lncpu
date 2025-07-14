package com.lnc.assembler.common;

import com.lnc.LNC;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OpcodeMap {
    private static final Map<String, OpcodeInfo> byImmediateName = new HashMap<>();

    private static final Map<Byte, OpcodeInfo>  byOpcode = new HashMap<>();

    static{
        List<String> lines = new BufferedReader(new InputStreamReader(LNC.class.getClassLoader().getResourceAsStream("opcodes.tsv"))).lines().collect(Collectors.toList());

        lines.remove(0);
        for (String line : lines) {
            String[] values = line.split("\t");
            String immediateName = values[1];
            byte opcode = (byte)Integer.parseInt(values[0].replace("0x",""), 16);
            OpcodeInfo opcodeInfo = new OpcodeInfo(immediateName, opcode, (byte)Integer.parseInt(values[2]), (byte)Integer.parseInt(values[3]));
            byImmediateName.put(immediateName, opcodeInfo);
            byOpcode.put(opcode, opcodeInfo);
        }
    }

    public static byte getOpcode(String name){
        OpcodeInfo info = byImmediateName.get(name.toLowerCase());
        if(info == null)
            throw new IllegalArgumentException("invalid instruction: " + name);
        return info.opcode;
    }

    public static byte getCodeLength(String name){
        OpcodeInfo info = byImmediateName.get(name.toLowerCase());
        if(info == null)
            throw new IllegalArgumentException("invalid instruction");
        return info.codeLength;
    }

    public static byte getClockCycles(String name){
        OpcodeInfo info = byImmediateName.get(name.toLowerCase());
        if(info == null)
            throw new IllegalArgumentException("invalid instruction");
        return info.clockCycles;
    }

    public static boolean isValid(String name){
        return name != null && byImmediateName.containsKey(name.toLowerCase());
    }

    public static String getImmediateName(byte opcode) {
        var opcodeInfo = byOpcode.get(opcode);

        if(opcodeInfo == null)
            throw new IllegalArgumentException("invalid opcode: " + opcode);

        return opcodeInfo.immediateName;
    }

    private static String immediateParamToLnasm(String immediateParam){
        return switch(immediateParam){
            case "cst" -> "<byte>";
            case "dcst" -> "<word>";
            case "datap" -> "[<data page address>]";
            case "abs" -> "[<full address>]";
            case "ird" -> "[RD]";
            case "ircrd" -> "[RC:RD]";
            case "rcrd" -> "RC:RD";
            case "bpoffset" -> "BP +/ <offset>";
            case "ibpoffset" -> "[BP +/ <offset>]";
            default -> immediateParam.toUpperCase();
        };
    }

    public static String toLnasmPseudocode(String immediateInstruction) {
        String[] splitted = immediateInstruction.split("_");

        if(splitted.length < 1)
            return "invalid instruction";
        else{
            String opcode = splitted[0];
            String[] immediateParams = new String[splitted.length - 1];
            System.arraycopy(splitted, 1, immediateParams, 0, immediateParams.length);
            return opcode + " " + Stream.of(immediateParams).map(OpcodeMap::immediateParamToLnasm).collect(Collectors.joining(", "));
        }
    }

    public static String[] getSimilarInstructions(String immediateInstruction) {

        String[] splitted = immediateInstruction.split("_");
        if (splitted.length == 1 && byImmediateName.containsKey(splitted[0])) {
            return new String[] { splitted[0] };
        } else if (splitted.length == 2) {
            return new ArrayList<>(byImmediateName.keySet().stream()
                    .filter(name -> name.startsWith(splitted[0] + "_") && !name.equals(immediateInstruction))
                    .toList()).toArray(new String[0]);
        } else if (splitted.length == 3) {
            return new ArrayList<>(byImmediateName.keySet().stream()
                    .filter(
                            name -> name.startsWith(splitted[0] + "_" + splitted[1]) ||
                                    (name.startsWith(splitted[0] + "_") && name.endsWith(splitted[2])))
                    .toList()).toArray(new String[0]);
        }

        return new String[0];
    }

    private record OpcodeInfo(String immediateName, byte opcode, byte codeLength, byte clockCycles) {
    }
}
