module com.dylibso.chicory.compiler {
    requires transitive com.dylibso.chicory.runtime;
    requires transitive com.dylibso.chicory.wasm;
    requires org.objectweb.asm;
    requires org.objectweb.asm.commons;
    requires org.objectweb.asm.util;

    exports com.dylibso.chicory.compiler;
    exports com.dylibso.chicory.compiler.internal;
    exports com.dylibso.chicory.experimental.aot;
}
