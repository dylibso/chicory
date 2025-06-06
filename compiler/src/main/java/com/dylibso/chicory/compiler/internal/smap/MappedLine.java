package com.dylibso.chicory.compiler.internal.smap;

import com.dylibso.chicory.dwarf.DebugInfo;

public class MappedLine {
    final int line;
    final DebugInfo.Entry entry;

    public MappedLine(int line, DebugInfo.Entry entry) {
        this.line = line;
        this.entry = entry;
    }

    public int line() {
        return line;
    }

    public DebugInfo.Entry entry() {
        return entry;
    }
}
