package com.lnasm;

import com.lnasm.compiler.CompileException;

import java.io.PrintStream;

public class Logger {

    private static String programState = "";

    private static PrintStream err = System.err, out = System.out;

    public static void error(String str){
        err.printf("%s(%s): %s\n", LNASM.PROGRAM_NAME, programState, str);
    }

    public static void out(String str){
        out.printf("%s(%s): %s\n", LNASM.PROGRAM_NAME, programState, str);
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
