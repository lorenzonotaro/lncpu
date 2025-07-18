package com.lnc.common;

import com.lnc.cc.types.*;

public class IntUtils {
    public static boolean inByteRange(int i) {
        return i >= -128 && i < 256;
    }

    public static boolean inUI8Range(int i) {
        return i >= 0 && i < 256;
    }

    public static boolean inShortRange(int i) {
        return i >= -32768 && i < 65536;
    }

    public static boolean inI8Range(int i) {
        return i >= -128 && i < 128;
    }

    public static boolean inUI16Range(int i) {
        return i >= 0 && i < 65536;
    }

    public static boolean inI16Range(int i) {
        return i >= -32768 && i < 32768;
    }

    public static TypeSpecifier getTypeFor(int value) {
        if (inUI8Range(value)) {
            return new UI8Type();
        }else if(inI8Range(value)){
            return new I8Type();
        }else if(inUI16Range(value)){
            return new UI16Type();
        }else if(inI16Range(value)){
            return new I16Type();
        }
        return null;
    }
}
