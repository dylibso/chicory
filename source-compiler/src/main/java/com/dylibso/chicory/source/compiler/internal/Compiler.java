package com.dylibso.chicory.source.compiler.internal;

import static java.util.Objects.requireNonNull;

import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.FunctionType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public static final String DEFAULT_CLASS_NAME = "com.dylibso.chicory.$gen.CompiledMachine";

    private final String className;
    private final WasmModule module;
    private final WasmAnalyzer analyzer;
    private final int functionImports;
    private final List<FunctionType> functionTypes;
    private final HashSet<Integer> interpretedFunctions;

    private Compiler(WasmModule module, String className, Set<Integer> interpretedFunctions) {
        this.className = requireNonNull(className, "className");
        this.module = requireNonNull(module, "module");
        this.analyzer = new WasmAnalyzer(module);
        this.functionImports = module.importSection().count(ExternalType.FUNCTION);

        if (interpretedFunctions == null || interpretedFunctions.isEmpty()) {
            this.interpretedFunctions = new HashSet<>();
        } else {
            this.interpretedFunctions = new HashSet<>(interpretedFunctions);
        }

        this.functionTypes = analyzer.functionTypes();
    }

    public static Builder builder(WasmModule module) {
        return new Builder(module);
    }

    public static final class Builder {
        private final WasmModule module;
        private String className;
        private Set<Integer> interpretedFunctions;

        private Builder(WasmModule module) {
            this.module = module;
        }

        public Builder withClassName(String className) {
            this.className = className;
            return this;
        }

        public Builder withInterpretedFunctions(Set<Integer> interpretedFunctions) {
            this.interpretedFunctions = interpretedFunctions;
            return this;
        }

        public Compiler build() {
            var className = this.className;
            if (className == null) {
                className = DEFAULT_CLASS_NAME;
            }

            return new Compiler(module, className, interpretedFunctions);
        }
    }

    /**
     * Entry point used by the test: generate Java source and print it.
     */
    public CompilerResult compile() {
        String source = compileToSource();
        System.out.println(source);
        return new CompilerResult(Set.copyOf(interpretedFunctions));
    }

    /**
     * Generate Java source code for the module and return as string.
     *
     * <p>For now:
     * <ul>
     *   <li>only the first non-imported function (funcId = functionImports) is handled,</li>
     *   <li>only simple opcodes like I32_CONST / LOCAL_GET / I32_ADD / RETURN are supported.</li>
     * </ul>
     */
    private String compileToSource() {
        int lastDot = className.lastIndexOf('.');
        String packageName = lastDot > 0 ? className.substring(0, lastDot) : "";
        String simpleClassName = lastDot > 0 ? className.substring(lastDot + 1) : className;

        int funcId = functionImports;
        if (funcId >= functionTypes.size()) {
            return "// No functions to compile";
        }

        var instructions = analyzer.analyze(funcId);
        var functionType = functionTypes.get(funcId);

        return SourceCodeEmitter.generateSource(
                packageName, simpleClassName, funcId, functionType, instructions, module, analyzer);
    }
}
