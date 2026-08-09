package com.lncpu.lnfs;

public class Main {

    public static final String DEFAULT_SETTINGS_FILE = "defaultSettings.json";
    public static final String PROGRAM_NAME = "lnfsutils";

    public static ProgramSettings programSettings;

    public static void main(String[] args) {
        if(!parseArgs(args))
            System.exit(0);
    }

    private static boolean parseArgs(String[] args) {
        programSettings = new ProgramSettings(Main.class.getClassLoader().getResourceAsStream(DEFAULT_SETTINGS_FILE));
        return programSettings.parse(args);
    }
}
