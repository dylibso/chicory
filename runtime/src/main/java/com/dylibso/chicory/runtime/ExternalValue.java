package com.dylibso.chicory.runtime;

/**
 * An <i>external value</i> is the runtime representation of an entity that can be imported or exported.
 * It is an address denoting either a function instance, table instance, memory instance,
 * or global instances in the shared store.
 *
 * See <a href="https://webassembly.github.io/spec/core/exec/runtime.html#syntax-externval">External Values</a>.
 */
public interface ExternalValue {
    enum Type {
        FUNCTION,
        GLOBAL,
        MEMORY,
        TABLE
    }

    String moduleName();

    String symbolName();

    ExternalValue.Type type();
}
