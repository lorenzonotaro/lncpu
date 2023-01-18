package com.computer8bit.eeprom.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileIO {
    private static final int ROW_WIDTH = 16;

    public static EEPROMDataByte[] importRawData(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        EEPROMDataByte[] newData = new EEPROMDataByte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            newData[i] = new EEPROMDataByte(b, null, i);
        }
        return newData;
    }

    public static void exportRawData(String filename, EEPROMDataByte[] data) throws IOException {
        try (FileOutputStream fw = new FileOutputStream(filename)) {
            for (EEPROMDataByte dataByte : data) {
                fw.write(dataByte.getValue());
            }
        }
    }

    public static void exportHexDump(String filename, EEPROMDataByte[] data) throws IOException {
        try (FileWriter fw = new FileWriter(filename)) {
            for (int i = 0; i < data.length; i++) {
                EEPROMDataByte dataByte = data[i];
                fw.write(String.format("0x%02x ", dataByte.getValue()));
                if((i + 1) % ROW_WIDTH == 0)
                    fw.write('\n');
            }
        }
    }

    public static void saveFile(String filename, EEPROMDataByte[] data) throws IOException {
        try (FileWriter fw = new FileWriter(filename)) {
            Gson gs = new GsonBuilder().create();
            String json = gs.toJson(data, EEPROMDataByte[].class);
            fw.write(json);
        }
    }

    public static EEPROMDataByte[] loadFile(Path path) throws IOException {
        String str = new String(Files.readAllBytes(path));
        Gson gs = new GsonBuilder().create();
        return gs.fromJson(str, EEPROMDataByte[].class);
    }
}
