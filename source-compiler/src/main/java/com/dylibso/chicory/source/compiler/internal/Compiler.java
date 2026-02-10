package com.dylibso.chicory.source.compiler.internal;

import static java.util.Objects.requireNonNull;

import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.FunctionType;
import java.util.List;

/**
 * Minimal Java-source compiler that mirrors the high-level structure of the ASM-based compiler.
 *
 * <p>For now this:
 * <ul>
 *   <li>analyzes the first non-imported function,</li>
 *   <li>generates a single Java class using {@link SourceCodeEmitter},</li>
 *   <li>prints the generated source to stdout.</li>
 * </ul>
 *
 * All ASM bytecode generation and class collector machinery from the original compiler has been
 * intentionally removed to keep things simple while we iterate on the source generator.
 */
public final class Compiler {

    public static final String DEFAULT_CLASS_NAME = "com.dylibso.chicory.gen.CompiledMachine";

    private final String className;
    private final WasmModule module;
    private final WasmAnalyzer analyzer;
    private final int functionImports;
    private final List<FunctionType> functionTypes;
    private final SourceCodeCollector collector;

    private Compiler(WasmModule module, String className, SourceCodeCollector collector) {
        this.className = requireNonNull(className, "className");
        this.module = requireNonNull(module, "module");
        this.analyzer = new WasmAnalyzer(module);
        this.functionImports = module.importSection().count(ExternalType.FUNCTION);
        this.collector = collector != null ? collector : new SimpleSourceCodeCollector();
        this.functionTypes = analyzer.functionTypes();
    }

    public static Builder builder(WasmModule module) {
        return new Builder(module);
    }

    public static final class Builder {
        private final WasmModule module;
        private String className;
        private SourceCodeCollector collector;

        private Builder(WasmModule module) {
            this.module = module;
        }

        public Builder withClassName(String className) {
            this.className = className;
            return this;
        }

        public Builder withSourceCodeCollector(SourceCodeCollector collector) {
            this.collector = collector;
            return this;
        }

        public Compiler build() {
            var className = this.className;
            if (className == null) {
                className = DEFAULT_CLASS_NAME;
            }

            return new Compiler(module, className, collector);
        }
    }

    /**
     * Compile the module to Java source files.
     */
    public CompilerResult compile() {
        String source = compileToSource();
        collector.putMainClass(className, source);
        return new CompilerResult(collector);
    }

    /**
     * Generate Java source code for the module and return as string.
     *
     * <p>Generates code for ALL functions in the module, not just the first one.
     */
    private String compileToSource() {
        int lastDot = className.lastIndexOf('.');
        String packageName = lastDot > 0 ? className.substring(0, lastDot) : "";
        String simpleClassName = lastDot > 0 ? className.substring(lastDot + 1) : className;

        if (functionTypes.isEmpty()) {
            return "// No functions to compile";
        }

        return SourceCodeEmitter.generateSource(
                packageName, simpleClassName, module, analyzer, functionTypes, functionImports);
    }
}
