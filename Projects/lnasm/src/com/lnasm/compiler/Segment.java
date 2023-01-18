package com.lnasm.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Segment {
    final byte csIndex;
    final int startAddress;
    final List<Encodeable> encodeables;
    private final Map<String, Byte> labels;
    int codeSize;

    public Segment(byte segmentIndex) {
        this.csIndex = segmentIndex;
        this.startAddress = segmentIndex << 8;
        this.encodeables = new ArrayList<>();
        this.labels = new HashMap<>();
    }

    public byte resolveLabel(String labelName, Token token) {
        Byte labelVal = this.labels.get(labelName);
        if(labelVal == null)
            throw new CompileException("unresolved label '" + labelName + "' in code segment " + csIndex, token);
        return labelVal;
    }

    boolean addInstruction(Encodeable instr) {
        if(codeSize + instr.size() >= 256)
            return false;
        encodeables.add(instr);
        codeSize += instr.size();
        return true;
    }

    public void addLabel(Token label) {
        if(labels.containsKey(label.lexeme))
            throw new CompileException("Duplicate label '" + label.lexeme + "' in code segment " + csIndex, label);
        labels.put(label.lexeme, (byte) codeSize);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Segment segment = (Segment) o;

        return csIndex == segment.csIndex;
    }

    @Override
    public int hashCode() {
        return csIndex;
    }
}
