package com.lnfsutils;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;

/**
 * A utility class for managing program settings and options.
 * This class provides functionalities to load, retrieve, modify, and handle
 * configuration options from a JSON file, as well as handle associated source files.
 */
public class ProgramSettings {
    private static final GsonBuilder gsonBuilder = new GsonBuilder();
    private final Map<String, Entry> entries;
    private final List<String> sourceFiles;

    public ProgramSettings(InputStream jsonFile){
        Type emptyMapType = new TypeToken<Map<String, Entry>>(){}.getType();
        this.entries = gsonBuilder.create().fromJson(new InputStreamReader(jsonFile), emptyMapType);
        sourceFiles = new ArrayList<>();
    }

    public <T> T get(String name, Class<T> tClass){
        try{
            return tClass.cast(entries.get(name).value);
        }catch(NullPointerException e){
            throw  new IllegalArgumentException("invalid option.");
        }catch(ClassCastException e){
            throw new IllegalArgumentException("Wrong option type");
        }
    }

    public Object set(String name, Object value){
        Entry entry = entries.get(name);

        if(entry == null)
            throw  new IllegalArgumentException("invalid option '" + name + "'");

        Object prev = entry.value;

        if(!prev.getClass().isInstance(value))
            throw  new IllegalArgumentException("invalid option '" + name + "'");

        entry.value = value;

        return prev;
    }

    public void parseAndSet(String name, String stringValue){
        Entry entry = entries.get(name);

        if(entry == null)
            throw  new IllegalArgumentException("invalid option '" + name + "'");

        Object prev = entry.value;

        if(prev instanceof String){
            set(name, stringValue);
        }else if(prev instanceof Integer){
            try{
                set(name, Integer.parseInt(stringValue));
            }catch(NumberFormatException e){
                throw new IllegalArgumentException("invalid format for option '" + name + "': expected integer");
            }
        }else if(prev instanceof Double){
            try{
                set(name, Double.parseDouble(stringValue));
            }catch(NumberFormatException e){
                throw new IllegalArgumentException("invalid format for option '" + name + "': expected number");
            }
        }else if(prev instanceof Boolean){
            set(name, Boolean.parseBoolean(stringValue));
        }
    }

    public String getHelp(String name){
        Entry entry = entries.get(name);

        if(entry == null)
            throw  new IllegalArgumentException("invalid option '" + name + "'");

        return entry.help;
    }

    public void addSourceFile(String filename){
        sourceFiles.add(filename);
    }

    public void help() {
        System.out.println("Usage: lnfsutils [<--option>|<source directory>...]\n" +
                "Options:");

        for (Map.Entry<String, Entry> entry : entries.entrySet()) {
            String name = entry.getKey();
            Entry value = entry.getValue();
            System.out.printf("  %s: %s", name, value.help);
            if (value.value != null && !value.value.toString().isEmpty()) {
                System.out.printf(" (default: %s)", value.value);
            }
            System.out.println();
        }
    }
    public boolean parse(String[] args){
        try {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("-")) {
                    if (arg.contains("=")) {
                        int index = arg.indexOf('=');
                        parseAndSet(arg.substring(0, index), arg.substring(index + 1));
                    } else if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        parseAndSet(arg, args[++i]);
                    } else set(arg, true);
                } else if (!arg.isBlank()) {
                    addSourceFile(arg);
                }else {
                    throw new IllegalArgumentException("Invalid argument: " + arg);
                }
            }

        }catch(IllegalArgumentException e){
            Logger.error(e.getMessage());
            return false;
        }
        return true;
    }

    public List<String> getSourceFiles() {
        return sourceFiles;
    }


    private static class Entry{
        private String help;
        private Object value;

        private Entry(String help, Object value) {
            this.help = help;
            this.value = value;
        }
    }
}
