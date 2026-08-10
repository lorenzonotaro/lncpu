package com.lnfsutils;

import java.io.FileOutputStream;
import java.io.IOException;

import com.lnfsutils.lnfs.LNFSBuilder;

public class Main {

    public static final String DEFAULT_SETTINGS_FILE = "defaultSettings.json";
    public static final String PROGRAM_NAME = "lnfsutils";
	private static final String VERSION = "1.0.0";

    public static ProgramSettings programSettings;

    public static void main(String[] args) {
        if(!parseArgs(args))
            System.exit(0);

        validateArgs();


        try {
            LNFSBuilder fs = new LNFSBuilder(programSettings);

            byte[] fsData = fs.build(programSettings.getSourceFiles().get(0));
            if (programSettings.get("--stdout", Boolean.class)){
                System.out.write(fsData);
            }
            String outFile = programSettings.get("--out", String.class);

            if (outFile != null) {
                String outputFile = programSettings.get("--out", String.class);
                try(FileOutputStream fos = new FileOutputStream(outputFile)){
                    fos.write(fsData);
                }
            }

        } catch (LNFSException e) {
            Logger.error("Error building LNFS filesystem: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            Logger.error("Error writing LNFS filesystem to output file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void validateArgs() {
                if (programSettings.get("--version", Boolean.class)) {
            System.out.println(PROGRAM_NAME + " version " + VERSION);
            System.exit(0);
            
        }

        if(programSettings.get("--help", Boolean.class)){
            System.out.println("lnfsutils - a utility for working with LNFS filesystems");
            programSettings.help();
            System.exit(0);
        }

        var sourceFiles = programSettings.getSourceFiles();
        if(sourceFiles.size() == 0){
            Logger.error("No source directory specified.");
            System.exit(1);
        }else if(sourceFiles.size() > 1){
            Logger.error("Multiple source directories specified. Only one source directory is allowed.");
            System.exit(1);
        }

        if (programSettings.get("--out", String.class).isEmpty() && !programSettings.get("--stdout", Boolean.class)) {
            programSettings.set("--out", "lnfs.bin");
        }
    }

    private static boolean parseArgs(String[] args) {
        programSettings = new ProgramSettings(Main.class.getClassLoader().getResourceAsStream(DEFAULT_SETTINGS_FILE));
        return programSettings.parse(args);
    }
}
