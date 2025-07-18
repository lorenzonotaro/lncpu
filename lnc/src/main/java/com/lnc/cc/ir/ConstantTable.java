package com.lnc.cc.ir;

import com.lnc.assembler.parser.EncodedData;

import java.util.Map;

public class ConstantTable {
    private final Map<Object, String> constants;

    private final String STRING_PREFIX = "str_";

    private int stringCounter = 0;

    public ConstantTable() {
        constants = new java.util.HashMap<>();
    }

    public String putString(String value){
        if (constants.containsKey(value)) {
            return constants.get(value);
        }

        String key = STRING_PREFIX + stringCounter++;
        constants.put(value, key);
        return key;
    }
}
