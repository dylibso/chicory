package com.dylibso.chicory.runtime;

/**
 * An <i>external value</i> is the runtime representation of an entity that can be imported.
 * It is an address denoting either a function instance, table instance, memory instance,
 * or global instances in the shared store.
 *
 * See also <a href="https://webassembly.github.io/spec/core/exec/runtime.html#syntax-externval">External Values</a>.
 *
 * @see ExportFunction
 */
public interface ImportValue {
    enum Type {
        FUNCTION,
        GLOBAL,
        MEMORY,
        TABLE
    }

    String module();

    String name();

    ImportValue.Type type();
}
