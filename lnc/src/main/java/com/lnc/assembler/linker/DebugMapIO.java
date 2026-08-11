package com.lnc.assembler.linker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lnc.assembler.common.LabelInfo;
import com.lnc.common.frontend.Location;
import com.lnc.common.frontend.Token;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DebugMapIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private DebugMapIO() {
    }

    public static DebugMap create(Collection<SectionBuilder> sections) {
        List<SectionBuilder.DebugEntry> entries = sections.stream()
                .flatMap(section -> section.getDebugEntries().stream())
                .sorted(Comparator.comparingInt(SectionBuilder.DebugEntry::address)
                        .thenComparing(SectionBuilder.DebugEntry::section))
                .toList();

        Map<String, Integer> fileIndexes = new LinkedHashMap<>();
        List<LineEntry> lines = new ArrayList<>(entries.size());
        List<LabelEntry> labels = new ArrayList<>();

        for (SectionBuilder.DebugEntry entry : entries) {
            Token token = entry.sourceToken();
            Location location = token == null ? null : token.location;
            String sourcePath = normalizedSourcePath(location);
            int fileIndex = sourcePath == null
                    ? -1
                    : fileIndexes.computeIfAbsent(sourcePath, ignored -> fileIndexes.size());

            lines.add(new LineEntry(
                    entry.address(),
                    entry.size(),
                    fileIndex,
                    location == null ? 0 : location.lineNumber,
                    location == null ? 0 : location.colNumber,
                    entry.section()));

            for (LabelInfo label : entry.labels()) {
                labels.add(new LabelEntry(label.name(), entry.address(), entry.section()));
            }
        }

        labels.sort(Comparator.comparingInt(LabelEntry::a)
                .thenComparing(LabelEntry::sec)
                .thenComparing(LabelEntry::name));

        return new DebugMap(1, List.copyOf(fileIndexes.keySet()), lines, labels);
    }

    public static void write(Path output, DebugMap debugMap) throws IOException {
        Files.writeString(output, GSON.toJson(debugMap));
    }

    private static String normalizedSourcePath(Location location) {
        if (location == null || location.filepath == null || location.filepath.isBlank()
                || "<internal>".equals(location.filepath) || "<internal>".equals(location.filename)) {
            return null;
        }
        try {
            return Path.of(location.filepath).toAbsolutePath().normalize().toString();
        } catch (InvalidPathException exception) {
            return location.filepath;
        }
    }

    public record DebugMap(int version, List<String> files, List<LineEntry> lines,
                           List<LabelEntry> labels) {
    }

    public record LineEntry(int a, int s, int f, int l, int c, String sec) {
    }

    public record LabelEntry(String name, int a, String sec) {
    }
}
