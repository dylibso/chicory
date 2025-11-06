module com.dylibso.chicory.runtime {
    requires transitive com.dylibso.chicory.wasm;

    exports com.dylibso.chicory.runtime;
    exports com.dylibso.chicory.runtime.alloc;
    exports com.dylibso.chicory.runtime.internal;
}
