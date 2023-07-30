package com.lnasm.compiler;

import com.lnasm.LNASM;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OpcodeMap {
    private static final Map<String, OpcodeInfo> opcodes = new HashMap<>();

    static{
        List<String> lines = new BufferedReader(new InputStreamReader(LNASM.class.getClassLoader().getResourceAsStream("opcodes.tsv"))).lines().collect(Collectors.toList());

        lines.remove(0);
        for (String line : lines) {
            String[] values = line.split("\t");
            opcodes.put(values[1], new OpcodeInfo((byte)Integer.parseInt(values[0].replace("0x",""), 16), (byte)Integer.parseInt(values[2]), (byte)Integer.parseInt(values[3])));
        }
    }

    public static byte getOpcode(String name){
        OpcodeInfo info = opcodes.get(name);
        if(info == null)
            throw new IllegalArgumentException("invalid instruction: " + name);
        return info.opcode;
    }

    public static byte getCodeLength(String name){
        OpcodeInfo info = opcodes.get(name);
        if(info == null)
            throw new IllegalArgumentException("invalid instruction");
        return info.codeLength;
    }

    public static byte getClockCycles(String name){
        OpcodeInfo info = opcodes.get(name);
        if(info == null)
            throw new IllegalArgumentException("invalid instruction");
        return info.clockCycles;
    }

    public static boolean isValid(String name){
        return name != null && opcodes.containsKey(name);
    }

    private static class OpcodeInfo {
        private final byte opcode;
        private final byte codeLength;
        private final byte clockCycles;

        private OpcodeInfo(byte opcode, byte codeLength, byte clockCycles) {
            this.opcode = opcode;
            this.codeLength = codeLength;
            this.clockCycles = clockCycles;
        }

        public byte getOpcode() {
            return opcode;
        }

        public byte getCodeLength() {
            return codeLength;
        }

        public byte getClockCycles() {
            return clockCycles;
        }
    }
}
