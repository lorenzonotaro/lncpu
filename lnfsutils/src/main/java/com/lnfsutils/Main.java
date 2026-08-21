package com.lnfsutils;

import java.io.IOException;
import java.nio.file.Path;

import com.lnfsutils.lnfs.LNFS;
import com.lnfsutils.packer.LNFSFileWriter;
import com.lnfsutils.packer.LNFSBinaryWriter;
import com.lnfsutils.packer.LNFSDirectoryBuilder;
import com.lnfsutils.unpacker.LNFSUnpackWriter;
import com.lnfsutils.unpacker.LNFSUnpacker;

public class Main {

    public static final String DEFAULT_SETTINGS_FILE = "defaultSettings.json";
    public static final String PROGRAM_NAME = "lnfsutils";
	private static final String VERSION = "1.1.0";
    public static ProgramSettings programSettings;

    public static void main(String[] args) {
        if(!parseArgs(args))
            System.exit(0);

        RunMode mode = validateArgs();

        if(mode == RunMode.PACK) {
            runPack();
        } else {
            runUnpack();
        }
    }

    private static void runPack() {
        try {
            LNFSDirectoryBuilder fs = new LNFSDirectoryBuilder(programSettings);

            LNFS lnfs = fs.build(Path.of(programSettings.getSourceFiles().get(0)));

            if (programSettings.get("--stdout", Boolean.class)){
                LNFSBinaryWriter writer = new LNFSBinaryWriter(System.out);
                writer.write(lnfs);
            }else{
                String outputFile = programSettings.get("--out", String.class);

                try(var packer = new LNFSFileWriter(Path.of(outputFile))){
                    packer.write(lnfs);
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

    private static void runUnpack(){
        try {
            ILNFSBuilder unpacker = new LNFSUnpacker();
            LNFS lnfs = unpacker.build(Path.of(programSettings.getSourceFiles().get(0)));

            LNFSUnpackWriter writer = new LNFSUnpackWriter(Path.of(programSettings.get("--out", String.class)));
            writer.write(lnfs);
        }catch(LNFSException | IOException e){
            Logger.error("Error unpacking LNFS filesystem: " + e.getMessage());
            System.exit(1);
        }
    }

    private static RunMode validateArgs() {

        RunMode mode = null;

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
        if(sourceFiles.isEmpty()){
            Logger.error("No source path specified.");
            System.exit(1);
        }else if(sourceFiles.size() > 1){
            Logger.error("Multiple source paths specified. Only one source path is allowed.");
            System.exit(1);
        }

        if(programSettings.get("--pack", Boolean.class)){
            if(programSettings.get("--unpack", Boolean.class)){
                Logger.error("Cannot specify both --pack and --unpack options.");
                System.exit(1);
            }
            mode = RunMode.PACK;
        }else if(programSettings.get("--unpack", Boolean.class)){
            mode = RunMode.UNPACK;
        }else{
            mode = RunMode.PACK;
        }

        if (programSettings.get("--out", String.class).isEmpty()) {
            if(mode == RunMode.UNPACK){
                Logger.error("Must specify --out option when unpacking.");
                System.exit(1);
            }else if(!programSettings.get("--stdout", Boolean.class)){
                programSettings.set("--out", "out.lnfs.bin");
            }
        }
        return mode;
    }

    private static boolean parseArgs(String[] args) {
        programSettings = new ProgramSettings(Main.class.getClassLoader().getResourceAsStream(DEFAULT_SETTINGS_FILE));
        return programSettings.parse(args);
    }

    private enum RunMode{
        PACK,
        UNPACK
    }
}
