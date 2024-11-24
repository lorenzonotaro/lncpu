package com.lnc.common;

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
}
