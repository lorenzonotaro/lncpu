package com.lnc.common;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;

public class ProgramSettings {
    private static final GsonBuilder gsonBuilder = new GsonBuilder();

    private final Map<String, Entry> entries;
    private final List<String> lnasmFiles, lncFiles;

    public ProgramSettings(InputStream jsonFile){
        Type emptyMapType = new TypeToken<Map<String, Entry>>(){}.getType();
        this.lnasmFiles = new ArrayList<>();
        this.lncFiles = new ArrayList<>();
        this.entries = gsonBuilder.create().fromJson(new InputStreamReader(jsonFile), emptyMapType);
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
        if(filename.endsWith(".lnasm")){
            lnasmFiles.add(filename);
        }else if(filename.endsWith(".lnc") || filename.endsWith(".lnh")){
            lncFiles.add(filename);
        }else{
            throw new IllegalArgumentException("invalid source file type: " + filename);
        }
    }

    public List<String> getLnasmFiles() {
        return lnasmFiles;
    }

    public List<String> getLncFiles(){
        return lncFiles;
    }

    public void help() {
        System.out.println("Usage: lnc [options] <source files>\n" +
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

    private static class Entry{
        private final String help;
        private Object value;

        private Entry(String help, Object value) {
            this.help = help;
            this.value = value;
        }
    }
}
