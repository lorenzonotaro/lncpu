package com.lnasm.compiler;

import com.lnasm.LNASM;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OpcodeMap {
    private static final Map<String, OpcodeInfo> byImmediateName = new HashMap<>();

    private static final Map<Byte, OpcodeInfo>  byOpcode = new HashMap<>();

    static{
        List<String> lines = new BufferedReader(new InputStreamReader(LNASM.class.getClassLoader().getResourceAsStream("opcodes.tsv"))).lines().collect(Collectors.toList());

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
        OpcodeInfo info = byImmediateName.get(name);
        if(info == null)
            throw new IllegalArgumentException("invalid instruction: " + name);
        return info.opcode;
    }

    public static byte getCodeLength(String name){
        OpcodeInfo info = byImmediateName.get(name);
        if(info == null)
            throw new IllegalArgumentException("invalid instruction");
        return info.codeLength;
    }

    public static byte getClockCycles(String name){
        OpcodeInfo info = byImmediateName.get(name);
        if(info == null)
            throw new IllegalArgumentException("invalid instruction");
        return info.clockCycles;
    }

    public static boolean isValid(String name){
        return name != null && byImmediateName.containsKey(name);
    }

    public static String getImmediateName(byte opcode) {
        return byOpcode.get(opcode).immediateName;
    }

    private record OpcodeInfo(String immediateName, byte opcode, byte codeLength, byte clockCycles) {
    }
}
