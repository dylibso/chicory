package com.dylibso.chicory.experimental.aot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public final class SourceMap {

    public enum Language {
        UNKNOWN("Unknown"),
        C89("C89"),
        C("C"),
        Ada83("Ada83"),
        CPP("C++"),
        Cobol74("Cobol74"),
        Cobol85("Cobol85"),
        Fortran77("Fortran77"),
        Fortran90("Fortran90"),
        Pascal83("Pascal83"),
        Modula2("Modula2"),
        Java("Java"),
        C99("C99"),
        Ada95("Ada95"),
        Fortran95("Fortran95"),
        PLI("PLI"),
        ObjC("Objective-C"),
        ObjCPP("Objective-C++"),
        UPC("UPC"),
        D("D"),
        Python("Python"),
        OpenCL("OpenCL"),
        Go("Go"),
        Modula3("Modula3"),
        Haskell("Haskell"),
        CPP03("C++03"),
        CPP11("C++11"),
        OCaml("OCaml"),
        Rust("Rust"),
        C11("C11"),
        Swift("Swift"),
        Julia("Julia"),
        Dylan("Dylan"),
        CPP14("C++14"),
        Fortran03("Fortran03"),
        Fortran08("Fortran08"),
        RenderScript("RenderScript"),
        BLISS("BLISS"),
        MipsAssembler("Mips Assembler"),
        GoogleRenderScript("Google RenderScript"),
        SunAssembler("SUN Assembler"),
        AltiumAssembler("Altium Assembler"),
        BorlandDelphi("Borland Delphi");

        private final String value;

        Language(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public static Language fromName(String name) {
            for (Language lang : values()) {
                if (lang.value.equals(name)) {
                    return lang;
                }
            }
            throw new IllegalArgumentException("Unknown language: " + name);
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public static final class Entry {
        private final long address;
        private final String file;
        private final String path;
        private final int line;
        private final Language language;

        public Entry(String path, String file, Language language, long address, int line) {
            this.path = Objects.requireNonNull(path);
            this.file = Objects.requireNonNullElse(file, path);
            this.language = language;
            this.address = address;
            this.line = line;
        }

        public String path() {
            return path;
        }

        public String file() {
            return file;
        }

        public Language language() {
            return language;
        }

        public long address() {
            return address;
        }

        public int line() {
            return line;
        }
    }

    static class MappedLine {
        final int line;
        final SourceMap.Entry entry;

        MappedLine(int line, SourceMap.Entry entry) {
            this.line = line;
            this.entry = entry;
        }
    }

    private final ArrayList<Entry> entries;

    private static int compareEntries(Entry a, Entry b) {
        return Long.compare(a.address, b.address);
    }

    public SourceMap(List<Entry> entries) {
        this.entries = new ArrayList<>(entries);
        this.entries.sort(SourceMap::compareEntries);
    }

    public List<Entry> entries() {
        return entries;
    }

    public int entryIndex(long address) {
        Entry key = new Entry("", "", Language.UNKNOWN, address, 0);
        var index = Collections.binarySearch(this.entries, key, SourceMap::compareEntries);
        if (index < 0) {
            return -1;
        } else {
            return index;
        }
    }

    public Entry entryAt(int index) {
        return this.entries.get(index);
    }

    String generateSmap(String outputFileName, List<MappedLine> mappedLines) {

        var linesByPath = new LinkedHashMap<String, ArrayList<MappedLine>>();
        for (MappedLine line : mappedLines) {
            String path = line.entry.path();
            linesByPath.computeIfAbsent(path, k -> new ArrayList<>()).add(line);
        }

        SmapGenerator g = new SmapGenerator();
        g.setOutputFileName(outputFileName);
        SmapStratum stratum = null;
        for (var entry : linesByPath.entrySet()) {

            ArrayList<MappedLine> lines = entry.getValue();

            // allocate a new stratum if the language changes
            Entry mappedEntry = lines.get(0).entry;
            String language = mappedEntry.language.value();
            if (stratum == null || !language.equals(stratum.getStratumName())) {
                if (stratum != null) {
                    g.addStratum(stratum, true);
                }
                stratum = new SmapStratum(language);
            }

            stratum.addFile(mappedEntry.file(), mappedEntry.path());
            for (MappedLine line : lines) {
                stratum.addLineData(line.entry.line, mappedEntry.path(), 1, line.line, 1);
            }
        }
        g.addStratum(stratum, true);

        return g.toString();
    }
}
