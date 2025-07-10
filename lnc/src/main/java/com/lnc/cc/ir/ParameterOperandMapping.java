package com.lnc.cc.ir;

import com.lnc.cc.ir.operands.IROperand;

import java.util.Map;

public record ParameterOperandMapping(
        Map<String, IROperand> entryParameterMapping,
        Map<String, IROperand> registerCopyParameterMapping
) {

    public IROperand get(String name) {
        var registerCopy = registerCopyParameterMapping.get(name);

        if (registerCopy != null) {
            return registerCopy;
        }

        var entryParam = entryParameterMapping.get(name);
        if (entryParam != null) {
            return entryParam;
        }

        throw new IllegalArgumentException("Parameter not found: " + name);
    }
}
