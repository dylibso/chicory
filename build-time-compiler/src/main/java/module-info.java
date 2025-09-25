module com.dylibso.chicory.build.time.compiler {
    requires transitive com.dylibso.chicory.compiler;
    requires com.dylibso.chicory.runtime;
    requires com.dylibso.chicory.wasm;
    requires com.github.javaparser.core;

    exports com.dylibso.chicory.build.time.compiler;
}
