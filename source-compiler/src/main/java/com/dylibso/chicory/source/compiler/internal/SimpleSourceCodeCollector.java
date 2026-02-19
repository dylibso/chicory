package com.dylibso.chicory.source.compiler.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple implementation of SourceCodeCollector that stores source files in memory.
 */
public class SimpleSourceCodeCollector implements SourceCodeCollector {
    private final LinkedHashMap<String, String> sourceFiles = new LinkedHashMap<>();
    private String mainClass;

    @Override
    public String mainClassName() {
        return mainClass;
    }

    @Override
    public void putMainClass(String className, String source) {
        this.mainClass = className;
        // Ensure the main class comes first in order
        var newSourceFiles = new LinkedHashMap<String, String>();
        newSourceFiles.put(className, source);
        newSourceFiles.putAll(this.sourceFiles);
        this.sourceFiles.clear();
        this.sourceFiles.putAll(newSourceFiles);
    }

    @Override
    public void put(String className, String source) {
        sourceFiles.put(className, source);
    }

    @Override
    public void putAll(SourceCodeCollector collector) {
        sourceFiles.putAll(collector.sourceFiles());
    }

    @Override
    public Map<String, String> sourceFiles() {
        return Collections.unmodifiableMap(sourceFiles);
    }
}
