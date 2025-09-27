package com.lnc.assembler.common;

import com.lnc.LNC;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A utility class that maps opcode names to their respective byte values, instruction lengths,
 * clock cycles, and provides mechanisms for translation and validation of opcode-related data.
 * The mapping is initialized from a Tab-Separated Values (TSV) file containing opcode information.
 */
public class OpcodeMap {
    /**
     *
     */
    private static final Map<String, OpcodeInfo> byImmediateName = new HashMap<>();

    /**
     *
     */
    private static final Map<Byte, OpcodeInfo>  byOpcode = new HashMap<>();

    static{
        List<String> lines = new BufferedReader(new InputStreamReader(LNC.class.getClassLoader().getResourceAsStream("opcodes.tsv"))).lines().collect(Collectors.toList());

        lines.remove(0);
        for (String line : lines) {
            String[] values = line.split("\t");
            String immediateName = values[1];
            byte opcode = (byte)Integer.parseInt(values[0].replace("0x",""), 16);
            OpcodeInfo opcodeInfo = new OpcodeInfo(immediateName, opcode, (byte)Integer.parseInt(values[2]), (byte)Integer.parseInt(values[3]));
            byImmediateName.put(immediateName, opcodeInfo);
            byOpcode.put(opcode, opcodeInfo);
        }
    }

    /**
     * Retrieves the opcode byte value associated with the given instruction name.
     * The method performs a case-insensitive lookup for the instruction name
     * and returns the corresponding opcode. If the instruction name is invalid,
     * an IllegalArgumentException is thrown.
     *
     * @param name the name of the instruction for which to retrieve the opcode
     * @return the opcode byte value corresponding to the given instruction name
     * @throws IllegalArgumentException if the provided instruction name is invalid
     */
    public static byte getOpcode(String name){
        OpcodeInfo info = byImmediateName.get(name.toLowerCase());
        if(info == null)
            throw new IllegalArgumentException("invalid instruction: " + name);
        return info.opcode;
    }

    /**
     * Retrieves the code length of a specified instruction by its name.
     *
     * The method takes the name of an instruction, converts it to lowercase,
     * and retrieves its corresponding OpcodeInfo object. If no matching instruction
     * is found, an IllegalArgumentException is thrown.
     *
     * @param name the name of the instruction whose code length is to be retrieved
     * @return the code length of the specified instruction as a byte
     * @throws IllegalArgumentException if the instruction name is invalid or not found
     */
    public static byte getCodeLength(String name){
        OpcodeInfo info = byImmediateName.get(name.toLowerCase());
        if(info == null)
            throw new IllegalArgumentException("invalid instruction");
        return info.codeLength;
    }

    /**
     * Retrieves the clock cycle count associated with the provided instruction name.
     *
     * This method performs a case-insensitive lookup for the instruction name
     * in a predefined map and returns the corresponding clock cycle count.
     * If the instruction name is invalid or not found, an IllegalArgumentException is thrown.
     *
     * @param name the name of the instruction whose clock cycle count is to be retrieved
     * @return the clock cycle count for the specified instruction as a byte
     * @throws IllegalArgumentException if the instruction name is invalid or not found
     */
    public static byte getClockCycles(String name){
        OpcodeInfo info = byImmediateName.get(name.toLowerCase());
        if(info == null)
            throw new IllegalArgumentException("invalid instruction");
        return info.clockCycles;
    }

    /**
     * Determines whether the given name is valid by checking if it is non-null
     * and exists as a key in the `byImmediateName` map (case-insensitive).
     *
     * @param name the name to be validated; expected to be a non-null string
     *             representing a possible key in the `byImmediateName` map
     * @return true if the name is non-null and a valid key in `byImmediateName`;
     *         false otherwise
     */
    public static boolean isValid(String name){
        return name != null && byImmediateName.containsKey(name.toLowerCase());
    }

    /**
     * Retrieves the immediate name associated with the provided opcode.
     *
     * @param opcode the opcode byte for which the corresponding immediate name is to be retrieved
     * @return the immediate name associated with the given opcode
     * @throws IllegalArgumentException if the provided opcode does not map to a valid entry
     */
    public static String getImmediateName(byte opcode) {
        var opcodeInfo = byOpcode.get(opcode);

        if(opcodeInfo == null)
            throw new IllegalArgumentException("invalid opcode: " + opcode);

        return opcodeInfo.immediateName;
    }

    /**
     * Converts an immediate parameter representation into its corresponding Lnasm syntax.
     *
     * This method processes the input string representing an immediate parameter and returns
     * its equivalent Lnasm format. If no match is found for predefined cases, the input string
     * is returned in uppercase. The method handles various formats, such as bytes, words,
     * addresses, registers, and offsets.
     *
     * @param immediateParam the immediate parameter string to be converted to Lnasm syntax
     * @return a string representing the Lnasm syntax for the given immediate parameter
     */
    private static String immediateParamToLnasm(String immediateParam){
        return switch(immediateParam){
            case "cst" -> "<byte>";
            case "dcst" -> "<word>";
            case "datap" -> "[<data page address>]";
            case "abs" -> "[<full address>]";
            case "ird" -> "[RD]";
            case "ircrd" -> "[RC:RD]";
            case "rcrd" -> "RC:RD";
            case "bpoffset" -> "BP +/ <offset>";
            case "ibpoffset" -> "[BP +/ <offset>]";
            default -> immediateParam.toUpperCase();
        };
    }

    /**
     * Converts an immediate instruction string into its corresponding Lnasm pseudocode representation.
     *
     * The method splits the input instruction string into an opcode and any associated immediate parameters.
     * The opcode is extracted as the first part, while the remaining parts (if any) are treated as immediate parameters.
     * These parameters are then mapped to their Lnasm equivalents using the `immediateParamToLnasm` method.
     * If the input string is invalid (e.g., empty), the method returns "invalid instruction".
     *
     * @param immediateInstruction the immediate instruction string to be converted to Lnasm pseudocode.
     *                             The format is expected to be "opcode_parameter1_parameter2_..." where the
     *                             opcode is the instruction name and the parameters represent associated
     *                             immediate values.
     * @return a string representing the Lnasm pseudocode of the provided instruction, or "invalid instruction"
     *         if the input string format is invalid.
     */
    public static String toLnasmPseudocode(String immediateInstruction) {
        String[] splitted = immediateInstruction.split("_");

        if(splitted.length < 1)
            return "invalid instruction";
        else{
            String opcode = splitted[0];
            String[] immediateParams = new String[splitted.length - 1];
            System.arraycopy(splitted, 1, immediateParams, 0, immediateParams.length);
            return opcode + " " + Stream.of(immediateParams).map(OpcodeMap::immediateParamToLnasm).collect(Collectors.joining(", "));
        }
    }

    /**
     * Retrieves an array of instruction names similar to the provided immediate instruction.
     *
     * The method analyzes the structure of the input instruction and searches for other instruction names
     * in the `byImmediateName` map that share a similar prefix or suffix structure. The rules for similarity
     * depend on the number of segments (split by underscore) in the input instruction:
     * - If the instruction is a single segment and exists in the map, it returns the instruction itself.
     * - If the instruction has two segments, it returns instructions with matching prefixes that are not the same as the input.
     * - If the instruction has three segments, it returns instructions with similar prefixes or suffixes.
     *
     * @param immediateInstruction the immediate instruction string for which to find similar instructions
     * @return an array of similar instruction names; if no similar instructions are found, an empty array is returned
     */
    public static String[] getSimilarInstructions(String immediateInstruction) {

        String[] splitted = immediateInstruction.split("_");
        if (splitted.length == 1 && byImmediateName.containsKey(splitted[0])) {
            return new String[] { splitted[0] };
        } else if (splitted.length == 2) {
            return new ArrayList<>(byImmediateName.keySet().stream()
                    .filter(name -> name.startsWith(splitted[0] + "_") && !name.equals(immediateInstruction))
                    .toList()).toArray(new String[0]);
        } else if (splitted.length == 3) {
            return new ArrayList<>(byImmediateName.keySet().stream()
                    .filter(
                            name -> name.startsWith(splitted[0] + "_" + splitted[1]) ||
                                    (name.startsWith(splitted[0] + "_") && name.endsWith(splitted[2])))
                    .toList()).toArray(new String[0]);
        }

        return new String[0];
    }

    private record OpcodeInfo(String immediateName, byte opcode, byte codeLength, byte clockCycles) {
    }
}
