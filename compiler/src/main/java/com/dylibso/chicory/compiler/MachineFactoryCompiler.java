package com.dylibso.chicory.compiler;

import com.dylibso.chicory.compiler.internal.MachineFactory;
import com.dylibso.chicory.runtime.DebugParser;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.WasmModule;
import java.util.Set;
import java.util.function.Function;

/**
 * Compiles WASM function bodies to JVM byte code that can be used as a machine factory for {@link Instance}'s.
 */
public final class MachineFactoryCompiler {

    private MachineFactoryCompiler() {
        // no instances
    }

    /**
     * The compile method reference can be used as machine factory in instance builders.
     * <pre>
     * var instance = Instance.builder(Parser.parse(is))
     *         .withMachineFactory(MachineFactoryCompiler::compile)
     *         .build();
     * </pre>
     * <p>
     * Every instance created by the builder will pay the cost of compiling the module.
     *
     * @see #compile(WasmModule) If you want to compile the module only once for multiple instances.
     */
    public static Machine compile(Instance instance) {
        return builder(instance.module())
                .withDebugParser(instance.debugParser())
                .compile()
                .apply(instance);
    }

    /**
     * Compiles a machine factory that can used in instance builders.
     * The module is only compiled once and the machine factory is reused for every
     * instance created by the builder.
     * <pre>
     * var module  = Parser.parse(is);
     * var builder = Instance.builder(module)
     *         .withMachineFactory(MachineFactoryCompiler.compile(module));
     * var instance1 = builder.build();
     * var instance2 = builder.build();
     * </pre>
     */
    public static Function<Instance, Machine> compile(WasmModule module) {
        return new MachineFactory(module);
    }

    /**
     * Configures a compiler that can compile a machine factory that can used in instance builders.
     * The builder allows you to configure the compiler options used to compile the module to
     * byte code.
     * This should be used when you want to create multiple instances of the same module.
     * <pre>
     * var module  = Parser.parse(is);
     * var builder = Instance.builder(module)
     *         .withMachineFactory(
     *             MachineFactoryCompiler.builder(module)
     *                 .withInterpreterFallback(InterpreterFallback.FAIL)
     *                 .compile()
     *         );
     * var instance1 = builder.build();
     * var instance2 = builder.build();
     * </pre>
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

        public Builder withDebugParser(DebugParser debugParser) {
            compilerBuilder.withDebugParser(debugParser);
            return this;
        }

        public Function<Instance, Machine> compile() {
            var result = compilerBuilder.build().compile();
            return new MachineFactory(module, result.machineFactory());
        }
    }
}
