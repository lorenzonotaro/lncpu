package com.lnc.common;

import com.lnc.LNC;
import com.lnc.common.frontend.Token;

import java.io.PrintStream;

public class Logger {

    private static String programState = "";

    private static PrintStream err = System.err, out = System.out;

    public static void error(String str){
        err.printf("%s(%s): error: %s\n", LNC.PROGRAM_NAME, programState, str);
    }

    public static void warning(String str){
        out.printf("%s(%s): warning: %s\n", LNC.PROGRAM_NAME, programState, str);
    }

    public static void compileWarning(String str, Token loc){
        warning(String.format("in file %s: %s", loc.formatLocation(), str));
    }

    public static void out(String str){
        out.printf("%s(%s): %s\n", LNC.PROGRAM_NAME, programState, str);
    }

    public static void setErr(PrintStream err) {
        Logger.err = err;
    }

    public static void setOut(PrintStream out) {
        Logger.out = out;
    }

    public static String getProgramState() {
        return programState;
    }

    public static void setProgramState(String programState) {
        Logger.programState = programState;
    }
}
