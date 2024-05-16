package com.lnasm;

import com.lnasm.compiler.Compiler;
import com.lnasm.compiler.common.Line;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LNASM {

    public static final String PROGRAM_NAME = "lnasm";
    public static final String PROGRAM_VERSION = "1.1.0";

    public static ProgramSettings settings = new ProgramSettings(LNASM.class.getClassLoader().getResourceAsStream("default-settings.json"));
    public static String[] includeDirs;

    public static void main(String[] args){
        Logger.setProgramState("init");
        init();
        if(!parseArgs(args)){
            System.exit(1);
        }
        System.exit(run());
    }

    private static int run() {
        if(settings.get("--version", Boolean.class)){
            System.out.printf("%s v%s\n", PROGRAM_NAME, PROGRAM_VERSION);
            return 0;
        }else if(settings.get("--help", Boolean.class)){
            settings.help();
            return 0;
        } else{
            return runFromSourceFiles();
        }
    }

    private static int runFromSourceFiles() {
        List<Line> lines, linkerConfigLines;
        try {
            includeDirs = settings.get("-I", String.class).split(";");

            if(settings.getSourceFiles().isEmpty()){
                Logger.error("no source files.");
                return 1;
            }
            lines = getLinesFromSourceFiles();
            linkerConfigLines = getLinkerConfig();
            Compiler compiler = new Compiler(lines, linkerConfigLines, settings.get("-f", String.class));
            if(!compiler.compile())
                System.exit(1);
            else if(!settings.get("-s", Boolean.class)){
                Files.write(Path.of(settings.get("-o", String.class)), compiler.getOutput());
            }
        } catch (FileNotFoundException | IllegalStateException e) {
            Logger.error(e.getMessage());
            return 1;
        } catch (IOException e) {
            Logger.error("unable to write output file (" + e.getMessage() + ")");
            return 1;
        }
        return 0;
    }

    private static List<Line> getLinesFromSourceFiles() throws FileNotFoundException {
        List<Line> lines = new ArrayList<>();
        for (String file : settings.getSourceFiles()) {
            lines.addAll(getLinesFromFile(file));
        }
        return lines;
    }

    private static List<Line> getLinkerConfig() throws FileNotFoundException {
        var configFile = settings.get("-lC", String.class);
        var configScript = settings.get("-lc", String.class);

        if("".equals(configScript) && "".equals(configFile)) {
            throw new IllegalStateException("no linker config specified");
        } else if(!"".equals(configScript) && !"".equals(configFile)){
            throw new IllegalStateException("cannot specify both linker config file and script");
        } else if(!"".equals(configFile)){
            return getLinesFromFile(configFile);
        }else{
            return new ArrayList<>(List.of(new Line("<cmdline>", "<cmdline>", configScript, 1)));
        }
    }

    public static List<Line> getLinesFromFile(String file) throws FileNotFoundException{
        List<String> strLines;
        List<Line> lines = new ArrayList<>();
        Path path = Path.of(file);
        try {
            strLines = Files.readAllLines(path);
        } catch (IOException e) {
            throw new FileNotFoundException("Unable to open source file '" + file + "'");
        }
        for (int i = 0; i < strLines.size(); i++) {
            String code = strLines.get(i);
            lines.add(new Line(path.toAbsolutePath(), code, i + 1));
        }
        return lines;
    }

    private static void init() {

    }

    private static boolean parseArgs(String[] args) {
        try {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("-")) {
                    if (arg.contains("=")) {
                        int index = arg.indexOf('=');
                        settings.parseAndSet(arg.substring(0, index), arg.substring(index + 1));
                    } else if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        settings.parseAndSet(arg, args[++i]);
                    } else settings.set(arg, true);
                } else {
                    settings.addSourceFile(arg);
                }
            }
        }catch(IllegalArgumentException e){
            Logger.error(e.getMessage());
            return false;
        }
        return true;
    }
}
