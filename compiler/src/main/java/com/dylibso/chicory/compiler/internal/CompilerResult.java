package com.dylibso.chicory.compiler.internal;

import java.util.Map;
import java.util.Set;

public final class CompilerResult {

    private final ClassCollector collector;
    private final Set<Integer> interpretedFunctions;

    public CompilerResult(ClassCollector collector, Set<Integer> interpretedFunctions) {
        this.collector = collector;
        this.interpretedFunctions = interpretedFunctions;
    }

    public Map<String, byte[]> classBytes() {
        return collector.classBytes();
    }

    public ClassCollector collector() {
        return this.collector;
    }

    public Set<Integer> interpretedFunctions() {
        return interpretedFunctions;
    }
}
