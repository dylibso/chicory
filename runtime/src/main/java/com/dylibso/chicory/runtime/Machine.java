package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.ChicoryException;
import java.util.function.IntConsumer;

public interface Machine {

    long[] call(int funcId, long[] args) throws ChicoryException;

    /**
     * Visits all GC ref IDs currently reachable from the machine's stack and locals.
     * Used by the sweep to find roots that are not in globals/tables.
     * Default implementation does nothing (e.g., for compiled machines where
     * GC refs live on the JVM stack and can't be enumerated).
     */
    default void visitGcRoots(IntConsumer visitor) {}
}
