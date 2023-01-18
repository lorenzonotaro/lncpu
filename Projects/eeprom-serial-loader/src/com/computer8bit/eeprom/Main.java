package com.computer8bit.eeprom;

import com.computer8bit.eeprom.data.EEPROMDataByte;
import com.computer8bit.eeprom.data.FileIO;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;

public class Main {

    public static final String WINDOW_TITLE = "EEPROM Serial Loader";
    private static boolean launchGUI = true;

    private static boolean exportRaw = false;
    private static String exportRawDest = null;

    private static boolean exportHex = false;
    private static String exportHexDest = null;

    private static String file = null;

    public static void main(String[] args) {
        if(parseArgs(args) == 1){
            System.err.println("Exiting...");
        }else {

            export();

            if(launchGUI)
                launchGUI();
        }
    }

    private static void export() {
        EEPROMDataByte[] data = null;
        try {
            if(exportRaw || exportHex)
                data = FileIO.loadFile(Path.of(file));
        } catch (IOException e) {
            System.err.println("Unable to open file '" + file + "':" + e.getMessage());
            return;
        }

        if(exportRaw){
            try {
                FileIO.exportRawData(exportRawDest, data);
                System.out.println("Exported as raw data to file '" + exportRawDest + "'.");
            } catch (IOException e) {
                System.err.println("Unable to export to file '" + exportRawDest + "': " + e.getMessage());
            }
        }

        if(exportHex){
            try {
                FileIO.exportHexDump(exportHexDest, data);
                System.out.println("Exported as hex dump to file '" + exportHexDest + "'.");
            } catch (IOException e) {
                System.err.println("Unable to export to file '" + exportHexDest + "': " + e.getMessage());
            }
        }
    }

    private static void launchGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e){ e.printStackTrace();}
        JFrame frame = new JFrame(WINDOW_TITLE);
        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        Window window = new Window(frame);
        frame.setContentPane(window.contentPane);
        frame.setJMenuBar(window.getMenuBar());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        if(file != null)
            window.openFile(Path.of(file));
    }

    private static int parseArgs(String[] args) {
        if(args.length > 0){
            if("--help".equalsIgnoreCase(args[0])){
                help();
            }else{
                file = args[0];
            }
        }
        for (int i = 1; i < args.length; i++) {
            switch(args[i]){
                case "--no-gui":
                    launchGUI = false;
                    break;
                case "--export-raw":
                    if(i + 1 >= args.length) return requiresDestFile("--export-raw");
                    exportRaw = true;
                    exportRawDest = args[++i];
                    break;
                case "--export-hex":
                    if(i + 1 >= args.length) return requiresDestFile("--export-hex");
                    exportHex = true;
                    exportHexDest = args[++i];
                    break;
            }
        }

        return 0;
    }

    private static void help() {
        System.out.println("eeprom-serial-loader. Usage: \n" +
                "       java -jar eeprom-serial-loader.jar [file] [options...]\n\n" +
                "Options: \n" +
                "   --no-gui : command-line only, doesn't launch the user interface\n" +
                "   --export-raw <dest> : exports the given file to the given destination as raw binary\n" +
                "   --export-hex <dest> : exports the given file to the given destination as a hex dump\n");
    }

    private static int requiresDestFile(String arg) {
        System.err.println("Error: " + arg + " requires a destination file. Use " + arg + " <file>");
        return 1;
    }
}
