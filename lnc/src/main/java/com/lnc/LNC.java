package com.lnc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.lnc.assembler.Assembler;
import com.lnc.assembler.linker.LinkTarget;
import com.lnc.cc.Compiler;
import com.lnc.cc.codegen.CompilerOutput;
import com.lnc.common.Logger;
import com.lnc.common.ProgramSettings;
import com.lnc.common.frontend.Line;

public class LNC {

    public static final String PROGRAM_NAME = "lnc";
    public static final String PROGRAM_VERSION = "2.6.1";
    private static final String DEFAULT_LINKER_CFG_FILENAME = "linker.cfg";

    public static ProgramSettings settings = new ProgramSettings(LNC.class.getClassLoader().getResourceAsStream("default-settings.json"));
    public static String[] includeDirs;
    private static LinkTarget[] requestedOutputs;

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

        try {

            parseIncludeDirs();

            parseRequestedOutputs();

            boolean noLncFiles = settings.getLncFiles().isEmpty();
            if(settings.getLnasmFiles().isEmpty() && noLncFiles){
                Logger.error("no source files.");
                return 1;
            }

            List<CompilerOutput> output = new ArrayList<>();

            if(!noLncFiles){
                Compiler compiler = new Compiler(settings.getLncFiles().stream().map(Path::of).toList());
                if(!compiler.compile())
                    return 1;

                output = compiler.getOutput();

                var assemblyOut = settings.get("-oA", String.class);

                if(!assemblyOut.isEmpty()) {
                    Files.writeString(Path.of(assemblyOut), outputsToString(output));
                }
            }

            Assembler assembler = new Assembler(settings.getLnasmFiles().stream().map(Path::of).toList(), getLinkerConfig(noLncFiles), output, requestedOutputs);

            if(!assembler.assemble())
                System.exit(1);
            else if(!settings.get("-s", Boolean.class)){
                assembler.writeOutputFiles();
            }



        } catch (IllegalStateException | IOException e) {
            Logger.error(e.getMessage());
            return 1;
        }
        return 0;
    }

    private static void parseRequestedOutputs() {
        String[] reqOutputs = settings.get("-oD", String.class).split(",");
        List<LinkTarget> targets = new ArrayList<>();
        for (String out : reqOutputs) {
            String trim = out.trim();
            if (!trim.isEmpty()) {
                try {
                    targets.add(LinkTarget.valueOf(trim.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    Logger.error("unknown output target '%s'".formatted(out));
                    throw e;
                }
            }
        }
        LNC.requestedOutputs = targets.toArray(new LinkTarget[0]);
    }

    private static void parseIncludeDirs() {
        List<String> includeDirsList = new ArrayList<>();

        String[] fromCmdLine = settings.get("-I", String.class).split(";");

        for (String dir : fromCmdLine) {
            if (!dir.isEmpty()) {
                includeDirsList.add(dir);
            }
        }


        // current jar directory
        var codeSource = LNC.class.getProtectionDomain().getCodeSource();
        URL url;
        if (codeSource != null && (url = codeSource.getLocation()) != null) {
            try {
                String path = Path.of(url.toURI()).getParent().resolve("lib").toString();
                includeDirsList.add(path);
            } catch (URISyntaxException ex) {}
        }

        LNC.includeDirs = includeDirsList.toArray(new String[0]);
    }

    private static String outputsToString(List<CompilerOutput> outputs) {
        return outputs.stream().map(CompilerOutput::toString).collect(Collectors.joining("\n\n"));
    }

    private static List<Line> getLinesFromSourceFiles() throws FileNotFoundException {
        List<Line> lines = new ArrayList<>();
        for (String file : settings.getLnasmFiles()) {
            lines.addAll(getLinesFromFile(file));
        }
        return lines;
    }

    private static String getLinkerConfig(boolean required) throws IOException {
        var configFile = settings.get("-lf", String.class);
        var configScript = settings.get("-lc", String.class);

        if("".equals(configScript) && "".equals(configFile)) {
            if (Files.exists(Path.of(DEFAULT_LINKER_CFG_FILENAME))) {
                configFile = DEFAULT_LINKER_CFG_FILENAME;
            } else if(required){
                throw new IllegalStateException("no linker config provided and no '%s' could be found".formatted(DEFAULT_LINKER_CFG_FILENAME));
            }
        }
        if(!"".equals(configScript) && !"".equals(configFile)){
            throw new IllegalStateException("cannot specify both linker config file and script");
        }else if(!"".equals(configFile)){
            return Files.readString(Path.of(configFile));
        }else{
            return configScript;
        }
    }

    public static List<Line> getLinesFromFile(String file) throws FileNotFoundException{
        List<String> strLines;
        List<Line> lines = new ArrayList<>();
        Path path = Path.of(file);
        try {
            strLines = Files.readAllLines(path);
        } catch (IOException e) {
            throw new FileNotFoundException("unable to open file '" + file + "'");
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

            if(settings.get("-oI", String.class).isEmpty() && settings.get("-oB", String.class).isEmpty()){
                settings.set("-oB", "a.out");
            }

        }catch(IllegalArgumentException e){
            Logger.error(e.getMessage());
            return false;
        }
        return true;
    }
}
