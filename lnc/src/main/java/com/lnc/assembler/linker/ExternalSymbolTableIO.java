package com.lnc.assembler.linker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Set;

/**
 * The ExternalSymbolTableIO class provides static methods for reading and writing
 * a symbol table represented as a map of {@code String} keys to {@code LabelMapEntry} values.
 * The symbol table is serialized to and deserialized from files using Java's object
 * serialization mechanism.
 */
public class ExternalSymbolTableIO {

    public static void write(String symOut, Map<String, LabelMapEntry> map) {
        // serialize the symbol table to a file
        try{
            var oos = new ObjectOutputStream(new java.io.FileOutputStream(symOut));
            oos.writeObject(map);
            oos.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("could not open file for writing: " + symOut, e);
        } catch (IOException e) {
            throw new RuntimeException("could not write to file: " + symOut, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, LabelMapEntry> read(String symIn) {
        try{
            var ois = new java.io.ObjectInputStream(new java.io.FileInputStream(symIn));
            var map = ois.readObject();
            ois.close();

            if(!(map instanceof Map<?, ?>)) {
                throw new RuntimeException("invalid symbol table file: " + symIn);
            }

            return (Map<String, LabelMapEntry>) map;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("could not open file for reading: " + symIn, e);
        } catch (IOException e) {
            throw new RuntimeException("could not read from file: " + symIn, e);
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new RuntimeException("invalid symbol table file: " + symIn, e);
        }
    }
}
