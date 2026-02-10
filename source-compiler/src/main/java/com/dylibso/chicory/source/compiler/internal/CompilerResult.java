package com.dylibso.chicory.source.compiler.internal;

import java.util.Set;

/**
 * Minimal compiler result for the source compiler.
 *
 * <p>The original bytecode compiler returned collected class bytes; for the Java source compiler we
 * currently only track the set of interpreted functions. Class bytes and collectors can be added
 * back later if needed.
 */
public final class CompilerResult {

    private final Set<Integer> interpretedFunctions;

    public CompilerResult(Set<Integer> interpretedFunctions) {
        this.interpretedFunctions = interpretedFunctions;
    }

    public Set<Integer> interpretedFunctions() {
        return interpretedFunctions;
    }
}
