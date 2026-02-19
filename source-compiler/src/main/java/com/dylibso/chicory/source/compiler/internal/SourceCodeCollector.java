package com.dylibso.chicory.source.compiler.internal;

import java.util.Map;

/**
 * A collector that stores generated Java source files.
 *
 * <p>Similar to ClassCollector but for Java source code instead of bytecode.
 */
public interface SourceCodeCollector {
    /**
     * Main entry point - the fully qualified class name of the main generated class.
     */
    String mainClassName();

    void putMainClass(String className, String source);

    void put(String className, String source);

    void putAll(SourceCodeCollector collector);

    Map<String, String> sourceFiles();
}
