package com.dylibso.chicory.compiler;

import com.dylibso.chicory.compiler.internal.MachineFactory;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.WasmModule;
import java.util.Set;
import java.util.function.Function;

/**
 * Compiles WASM function bodies to JVM byte code to create Machine implementations.
 */
public final class RuntimeCompiler {

    private RuntimeCompiler() {
        // no instances
    }

    /**
     * The compile method reference can be used as machine factory in instance builders.
     * <code>
     * var instance = Instance.builder(Parser.parse(is)).
     *         withMachineFactory(RuntimeCompiler::compile).
     *         build();
     * </code>
     * <p>
     * Every instance created by the builder will pay the cost of compiling the module.
     * </p>
     *
     * @see #compile(WasmModule) If you want to compile the module only once for multiple instances.
     */
    public static Machine compile(Instance instance) {
        return compile(instance.module()).apply(instance);
    }

    /**
     * Compiles a machine factory that can used in instance builders.
     * The module is only compiled once and the machine factory is reused for every
     * instance created by the builder.
     * <p>
     * <code>
     * var module  = Parser.parse(is);
     * var builder = Instance.builder(module).withMachineFactory(RuntimeCompiler.compile(module));
     * var instance1 = builder.build();
     * var instance2 = builder.build();
     * </code>
     */
    public static Function<Instance, Machine> compile(WasmModule module) {
        return new MachineFactory(module);
    }

    /**
     * Configures a compiler that can compile a machine factory that can used in instance builders.
     * The builder allows you to configure the compiler options used to compile the module to
     * byte code.
     * This should be used when you want to create multiple instances of the same module.
     * <p>
     * <code>
     * var module  = Parser.parse(is);
     * var builder = Instance.builder(module).withMachineFactory(
     *      RuntimeCompiler.builder(module).withInterpreterFallback(InterpreterFallback.FAIL).compile()
     * );
     * var instance1 = builder.build();
     * var instance2 = builder.build();
     * </code>
     */
    public static Builder builder(WasmModule module) {
        return new Builder(module);
    }

    public static final class Builder {
        private final WasmModule module;
        private final com.dylibso.chicory.compiler.internal.Compiler.Builder compilerBuilder;

        private Builder(WasmModule module) {
            this.module = module;
            this.compilerBuilder = com.dylibso.chicory.compiler.internal.Compiler.builder(module);
        }

        public Builder withClassName(String className) {
            compilerBuilder.withClassName(className);
            return this;
        }

        public Builder withMaxFunctionsPerClass(int maxFunctionsPerClass) {
            compilerBuilder.withMaxFunctionsPerClass(maxFunctionsPerClass);
            return this;
        }

        public Builder withInterpreterFallback(InterpreterFallback interpreterFallback) {
            compilerBuilder.withInterpreterFallback(interpreterFallback);
            return this;
        }

        public Builder withInterpretedFunctions(Set<Integer> interpretedFunctions) {
            compilerBuilder.withInterpretedFunctions(interpretedFunctions);
            return this;
        }

        public Function<Instance, Machine> compile() {
            var result = compilerBuilder.build().compile();
            return new MachineFactory(module, result.machineFactory());
        }
    }
}
