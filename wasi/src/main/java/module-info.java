module com.dylibso.chicory.wasi {
    requires static com.dylibso.chicory.annotations;
    requires com.dylibso.chicory.log;
    requires transitive com.dylibso.chicory.runtime;
    requires static java.compiler;

    exports com.dylibso.chicory.wasi;
}
