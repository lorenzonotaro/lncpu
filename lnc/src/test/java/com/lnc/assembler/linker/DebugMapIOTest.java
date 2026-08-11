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
import com.lnc.common.frontend.Token;
import com.lnc.common.frontend.TokenType;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DebugMapIOTest {
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
}
