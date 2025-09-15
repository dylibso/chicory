module com.dylibso.chicory.compiler {
    requires transitive com.dylibso.chicory.runtime;
    requires transitive com.dylibso.chicory.wasm;
    requires org.objectweb.asm;
    requires org.objectweb.asm.commons;

    exports com.dylibso.chicory.compiler;
    exports com.dylibso.chicory.compiler.internal to
            com.dylibso.chicory.build.time.compiler;
    exports com.dylibso.chicory.experimental.aot;
}
