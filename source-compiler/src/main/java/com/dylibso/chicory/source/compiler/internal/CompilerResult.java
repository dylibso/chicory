package com.dylibso.chicory.source.compiler.internal;

/**
 * Compiler result for the source compiler.
 *
 * <p>Contains the source code collector with generated Java files.
 */
public final class CompilerResult {

    private final SourceCodeCollector collector;

    public CompilerResult(SourceCodeCollector collector) {
        this.collector = collector;
    }

    public SourceCodeCollector collector() {
        return collector;
    }
}
