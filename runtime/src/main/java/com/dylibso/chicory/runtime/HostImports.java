package com.dylibso.chicory.runtime;

import java.util.Arrays;
import java.util.stream.Collectors;

public class HostImports {

    private String name;
    private final HostFunction[] functions;
    private final HostGlobal[] globals;
    private final HostMemory[] memories;
    private final HostTable[] tables;
    private FromHost[] index;
    private HostImports[] delegates;

    public HostImports(String name) {
        this.name = name;
        this.functions = new HostFunction[] {};
        this.globals = new HostGlobal[] {};
        this.memories = new HostMemory[] {};
        this.tables = new HostTable[] {};
    }

    public HostImports(String name, HostImports... imports) {
        this.name = name;
        this.delegates = imports;
        this.functions = new HostFunction[] {};
        this.globals = new HostGlobal[] {};
        this.memories = new HostMemory[] {};
        this.tables = new HostTable[] {};
    }

    public HostImports() {
        this.name = null;
        this.functions = new HostFunction[] {};
        this.globals = new HostGlobal[] {};
        this.memories = new HostMemory[] {};
        this.tables = new HostTable[] {};
    }

    public HostImports(HostFunction[] functions) {
        this.name = null;
        this.functions = functions;
        this.globals = new HostGlobal[] {};
        this.memories = new HostMemory[] {};
        this.tables = new HostTable[] {};
    }

    public HostImports(HostGlobal[] globals) {
        this.name = null;
        this.functions = new HostFunction[] {};
        this.globals = globals;
        this.memories = new HostMemory[] {};
        this.tables = new HostTable[] {};
    }

    public HostImports(HostMemory[] memories) {
        this.name = null;
        this.functions = new HostFunction[] {};
        this.globals = new HostGlobal[] {};
        this.memories = memories;
        this.tables = new HostTable[] {};
    }

    public HostImports(HostMemory memory) {
        this.name = null;
        this.functions = new HostFunction[] {};
        this.globals = new HostGlobal[] {};
        this.memories = new HostMemory[] {memory};
        this.tables = new HostTable[] {};
    }

    public HostImports(HostTable[] tables) {
        this.name = null;
        this.functions = new HostFunction[] {};
        this.globals = new HostGlobal[] {};
        this.memories = new HostMemory[] {};
        this.tables = tables;
    }

    public HostImports(
            HostFunction[] functions, HostGlobal[] globals, HostMemory memory, HostTable[] tables) {
        this.name = null;
        this.functions = functions;
        this.globals = globals;
        this.memories = new HostMemory[] {memory};
        this.tables = tables;
    }

    public HostImports(
            HostFunction[] functions,
            HostGlobal[] globals,
            HostMemory[] memories,
            HostTable[] tables) {
        this.name = null;
        this.functions = functions;
        this.globals = globals;
        this.memories = memories;
        this.tables = tables;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDelegates(HostImports[] delegates) {
        this.delegates = delegates;
    }

    public HostFunction[] getFunctions() {
        return functions;
    }

    public HostGlobal[] getGlobals() {
        return globals;
    }

    public HostMemory[] getMemories() {
        return memories;
    }

    public HostTable[] getTables() {
        return tables;
    }

    public FromHost[] getIndex() {
        return index;
    }

    public void setIndex(FromHost[] index) {
        this.index = index;
    }

    public HostImports resolve() {
        var finalFunctions = Arrays.stream(functions).collect(Collectors.toSet());
        var finalGlobals = Arrays.stream(globals).collect(Collectors.toSet());
        var finalMemories = Arrays.stream(memories).collect(Collectors.toSet());
        var finalTables = Arrays.stream(tables).collect(Collectors.toSet());

        if (delegates != null) {
            for (var delegate : delegates) {
                var resolved = delegate.resolve();

                for (var f : resolved.getFunctions()) {
                    var matching =
                            finalFunctions.stream()
                                    .filter(
                                            fun ->
                                                    fun.getModuleName().equals(f.getModuleName())
                                                            && fun.getFieldName()
                                                                    .equals(f.getFieldName()))
                                    .findFirst();
                    if (matching.isPresent()) {
                        if (!matching.get().override()) {
                            finalFunctions.remove(matching.get());
                            finalFunctions.add(f);
                        }
                    } else {
                        finalFunctions.add(f);
                    }
                }
                // TODO extend the logic to Globals, Memories, Tables ...
            }
        }
        return new HostImports(
                finalFunctions.toArray(new HostFunction[finalFunctions.size()]),
                finalGlobals.toArray(new HostGlobal[finalGlobals.size()]),
                finalMemories.toArray(new HostMemory[finalMemories.size()]),
                finalTables.toArray(new HostTable[finalTables.size()]));
    }
}
