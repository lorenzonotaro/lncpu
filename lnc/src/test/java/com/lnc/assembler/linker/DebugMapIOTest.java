package com.lnc.assembler.linker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lnc.LNC;
import com.lnc.assembler.Assembler;
import com.lnc.assembler.common.LabelInfo;
import com.lnc.assembler.common.LinkMode;
import com.lnc.assembler.common.SectionInfo;
import com.lnc.assembler.parser.Instruction;
import com.lnc.assembler.parser.LnasmParsedBlock;
import com.lnc.cc.Compiler;
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DebugMapIOTest {
    @Test
    public void compilerInstructionsRetainLncSourceLocations() throws Exception {
        Path directory = Path.of("target", "test-temp", "compiler-debug-map");
        Files.createDirectories(directory);
        Path source = directory.resolve("program.lnc").toAbsolutePath().normalize();
        Path debugMap = directory.resolve("program.debug.json");
        Files.writeString(source, String.join("\n",
                "int adjust(int value) {",
                "    value = value + 1;",
                "    return value;",
                "}",
                ""));

        LNC.settings.set("-S", "");
        Compiler compiler = new Compiler(List.of(source));
        assertTrue(compiler.compile());

        Assembler assembler = new Assembler(
                List.of(),
                "SECTIONS[DUMMY: mode = fixed, start = 0x1fff;]",
                compiler.getOutput(),
                new LinkTarget[]{LinkTarget.ROM});
        assertTrue(assembler.assemble());
        assembler.writeDebugMap(debugMap.toString());

        JsonObject root = JsonParser.parseString(Files.readString(debugMap)).getAsJsonObject();
        assertEquals(1, root.get("version").getAsInt());
        assertEquals(source.toString(), root.getAsJsonArray("files").get(0).getAsString());

        Set<Integer> mappedLines = root.getAsJsonArray("lines").asList().stream()
                .map(element -> element.getAsJsonObject())
                .filter(line -> line.get("f").getAsInt() == 0)
                .map(line -> line.get("l").getAsInt())
                .filter(line -> line > 0)
                .collect(Collectors.toSet());
        assertTrue(mappedLines.contains(2));
        assertTrue(mappedLines.contains(3));
    }

    @Test
    public void callLineStartsAtItsStackArgumentPush() throws Exception {
        Path directory = Path.of("target", "test-temp", "compiler-call-debug-map");
        Files.createDirectories(directory);
        Path source = directory.resolve("program.lnc").toAbsolutePath().normalize();
        Path debugMap = directory.resolve("program.debug.json");
        Files.writeString(source, String.join("\n",
                "int callee(...) {",
                "    return 1;",
                "}",
                "int invoke() {",
                "    return callee(5);",
                "}",
                ""));

        LNC.settings.set("-S", "");
        Compiler compiler = new Compiler(List.of(source));
        assertTrue(compiler.compile());

        Assembler assembler = new Assembler(
                List.of(),
                "SECTIONS[DUMMY: mode = fixed, start = 0x1fff;]",
                compiler.getOutput(),
                new LinkTarget[]{LinkTarget.ROM});
        assertTrue(assembler.assemble());
        assembler.writeDebugMap(debugMap.toString());

        JsonObject root = JsonParser.parseString(Files.readString(debugMap)).getAsJsonObject();
        List<JsonObject> invokeEntries = root.getAsJsonArray("lines").asList().stream()
                .map(element -> element.getAsJsonObject())
                .filter(line -> "LNCCODE#invoke".equals(line.get("sec").getAsString()))
                .toList();
        int callLineStart = invokeEntries.stream()
                .filter(line -> line.get("l").getAsInt() == 5)
                .mapToInt(line -> line.get("a").getAsInt())
                .min()
                .orElseThrow();

        assertEquals(invokeEntries.get(0).get("a").getAsInt(), callLineStart);
    }

    @Test
    public void assemblerWritesFinalAddressesAndOriginalInstructionLocations() throws Exception {
        Path directory = Files.createTempDirectory("lnc-debug-map-test");
        Path source = directory.resolve("program.lnasm");
        Path debugMap = directory.resolve("program.debug.json");
        Files.writeString(source, String.join("\n",
                ".section CODE",
                "start:",
                "    mov 1, RA",
                "    hlt",
                ""));

        LNC.settings.set("-S", "");
        Assembler assembler = new Assembler(
                List.of(source),
                "SECTIONS[CODE: mode = fixed, start = 0x1234;]",
                List.of(),
                new LinkTarget[]{LinkTarget.ROM});

        assertTrue(assembler.assemble());
        assembler.writeDebugMap(debugMap.toString());

        JsonObject root = JsonParser.parseString(Files.readString(debugMap)).getAsJsonObject();
        assertEquals(1, root.get("version").getAsInt());
        assertEquals(source.toAbsolutePath().normalize().toString(),
                root.getAsJsonArray("files").get(0).getAsString());
        assertEquals(2, root.getAsJsonArray("lines").size());

        JsonObject move = root.getAsJsonArray("lines").get(0).getAsJsonObject();
        JsonObject halt = root.getAsJsonArray("lines").get(1).getAsJsonObject();
        assertEquals(0x1234, move.get("a").getAsInt());
        assertEquals(3, move.get("l").getAsInt());
        assertEquals(5, move.get("c").getAsInt());
        assertEquals(0x1234 + move.get("s").getAsInt(), halt.get("a").getAsInt());
        assertEquals(4, halt.get("l").getAsInt());
        assertEquals(5, halt.get("c").getAsInt());
        assertEquals("CODE", halt.get("sec").getAsString());

        JsonObject label = root.getAsJsonArray("labels").get(0).getAsJsonObject();
        assertEquals("start", label.get("name").getAsString());
        assertEquals(0x1234, label.get("a").getAsInt());
        assertEquals("CODE", label.get("sec").getAsString());
    }

    @Test
    public void mapIncludesAllPhysicalTargetsIndependentlyOfRequestedOutputs() throws Exception {
        Path directory = Files.createTempDirectory("lnc-physical-debug-map-test");
        Path source = directory.resolve("program.lnasm");
        Path debugMap = directory.resolve("program.debug.json");
        Files.writeString(source, String.join("\n",
                ".section ROM_CODE",
                "rom_label:",
                "    hlt",
                ".section RAM_CODE",
                "ram_label:",
                "    hlt",
                ".section D0_CODE",
                "d0_label:",
                "    hlt",
                ".section VIRTUAL_DATA",
                "virtual_label:",
                "    hlt",
                ""));

        LNC.settings.set("-S", "");
        Assembler assembler = new Assembler(
                List.of(source),
                String.join("\n",
                        "SECTIONS[",
                        "ROM_CODE: mode = fixed, start = 0x0100;",
                        "RAM_CODE: mode = fixed, start = 0x2100;",
                        "D0_CODE: mode = fixed, start = 0x4100;",
                        "EMPTY_D0: mode = fixed, start = 0x4200;",
                        "VIRTUAL_DATA: datapage, virtual;",
                        "]"),
                List.of(),
                new LinkTarget[]{LinkTarget.ROM});

        assertTrue(assembler.assemble());
        assembler.writeDebugMap(debugMap.toString());

        JsonObject root = JsonParser.parseString(Files.readString(debugMap)).getAsJsonObject();
        assertEquals(1, root.get("version").getAsInt());
        assertEquals(4, root.getAsJsonArray("sections").size());

        JsonObject rom = root.getAsJsonArray("sections").get(0).getAsJsonObject();
        JsonObject ram = root.getAsJsonArray("sections").get(1).getAsJsonObject();
        JsonObject d0 = root.getAsJsonArray("sections").get(2).getAsJsonObject();
        JsonObject emptyD0 = root.getAsJsonArray("sections").get(3).getAsJsonObject();
        assertSection(rom, "ROM_CODE", "ROM", 0x0100, 1);
        assertSection(ram, "RAM_CODE", "RAM", 0x2100, 1);
        assertSection(d0, "D0_CODE", "D0", 0x4100, 1);
        assertSection(emptyD0, "EMPTY_D0", "D0", 0x4200, 0);
        assertTrue(root.getAsJsonArray("sections").asList().stream()
                .map(element -> element.getAsJsonObject())
                .noneMatch(section -> "VIRTUAL_DATA".equals(section.get("name").getAsString())));

        JsonObject ramLabel = root.getAsJsonArray("labels").asList().stream()
                .map(element -> element.getAsJsonObject())
                .filter(label -> "ram_label".equals(label.get("name").getAsString()))
                .findFirst()
                .orElseThrow();
        JsonObject d0Label = root.getAsJsonArray("labels").asList().stream()
                .map(element -> element.getAsJsonObject())
                .filter(label -> "d0_label".equals(label.get("name").getAsString()))
                .findFirst()
                .orElseThrow();
        assertEquals(0x2100, ramLabel.get("a").getAsInt());
        assertEquals("RAM_CODE", ramLabel.get("sec").getAsString());
        assertEquals(0x4100, d0Label.get("a").getAsInt());
        assertEquals("D0_CODE", d0Label.get("sec").getAsString());
    }

    @Test
    public void mapSortsByFinalAddressAndMarksInternalInstructionsSynthetic() {
        SectionBuilder later = sectionWithInternalInstruction("LATER", 0x30, "later");
        SectionBuilder earlier = sectionWithInternalInstruction("EARLIER", 0x10, "earlier");

        DebugMapIO.DebugMap map = DebugMapIO.create(List.of(later, earlier));

        assertTrue(map.files().isEmpty());
        assertEquals(0x10, map.lines().get(0).a());
        assertEquals(0x30, map.lines().get(1).a());
        assertEquals(-1, map.lines().get(0).f());
        assertEquals(0, map.lines().get(0).l());
        assertEquals(0, map.lines().get(0).c());
        assertEquals("earlier", map.labels().get(0).name());
        assertEquals("later", map.labels().get(1).name());
    }

    private static SectionBuilder sectionWithInternalInstruction(String name, int start, String labelName) {
        SectionInfo sectionInfo = new SectionInfo(name, start, null, LinkMode.FIXED,
                false, false, false);
        SectionBuilder builder = new SectionBuilder(sectionInfo, ignored -> null);
        Token opcode = Token.__internal(TokenType.HLT, "hlt");
        Instruction instruction = new Instruction(opcode, new com.lnc.assembler.parser.argument.Argument[0]);
        instruction.setLabels(List.of(new LabelInfo(opcode, labelName)));
        LinkedList<com.lnc.assembler.parser.CodeElement> instructions = new LinkedList<>();
        instructions.add(instruction);
        builder.append(new LnasmParsedBlock(Token.__internal(TokenType.IDENTIFIER, name), instructions));
        builder.setSectionStart(start);
        return builder;
    }

    private static void assertSection(JsonObject section, String name, String target, int address, int size) {
        assertEquals(name, section.get("name").getAsString());
        assertEquals(target, section.get("target").getAsString());
        assertEquals(address, section.get("a").getAsInt());
        assertEquals(size, section.get("s").getAsInt());
    }
}
