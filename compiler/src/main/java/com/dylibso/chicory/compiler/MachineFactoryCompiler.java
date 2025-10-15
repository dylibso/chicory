package com.dylibso.chicory.compiler;

import com.dylibso.chicory.compiler.internal.ClassLoadingCollector;
import com.dylibso.chicory.compiler.internal.MachineFactory;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.WasmModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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
        private Cache cache;

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

        public Builder withCache(Cache cache) {
            this.cache = cache;
            return this;
        }

        public Function<Instance, Machine> compile() {

            var useCache = cache != null && module.messageDigest() != null;
            if (!useCache) {
                // we can't cache so compile the classes...
                var result =
                        compilerBuilder
                                .withClassCollectorFactory(ClassLoadingCollector::new)
                                .build()
                                .compile();
                var collector = (ClassLoadingCollector) result.collector();
                return new MachineFactory(module, collector.machineFactory());
            }

            // We can cache, so see if it's previously compiled.
            try {
                Path cachePath = cache.get(module.messageDigest());
                if (cachePath == null) {

                    // Not previously compiled, so compile it now...
                    var result =
                            compilerBuilder
                                    .withClassCollectorFactory(ClassLoadingCollector::new)
                                    .build()
                                    .compile();

                    var collector = (ClassLoadingCollector) result.collector();

                    // Store the compiled results in the cache
                    try (var tempPath = cache.createTempDir()) {
                        var properties = new Properties();
                        properties.put("mainClass", collector.mainClassName());
                        var wasmModuleProperties =
                                tempPath.path().resolve("wasm-module.properties");
                        try (var f = Files.newOutputStream(wasmModuleProperties)) {
                            properties.store(f, "");
                        }
                        writeClassesToCache(tempPath.path(), collector.classBytes());
                        cache.put(module.messageDigest(), tempPath);
                    } catch (IOException e) {
                        throw new ChicoryException(e);
                    }

                    return new MachineFactory(module, collector.machineFactory());
                }

                // It was previously compiled, just load it.
                var wasmModuleProperties = cachePath.resolve("wasm-module.properties");
                try (var is = Files.newInputStream(wasmModuleProperties)) {

                    var properties = new Properties();
                    properties.load(is);
                    String mainClass = properties.getProperty("mainClass");

                    var collector = new ClassLoadingCollector();
                    var classes = loadClassesFromCache(cachePath);
                    for (var entry : classes.entrySet()) {
                        if (entry.getKey().equals(mainClass)) {
                            collector.putMainClass(mainClass, classes.get(mainClass));
                        } else {
                            collector.put(entry.getKey(), entry.getValue());
                        }
                    }

                    return new MachineFactory(module, collector.machineFactory());
                }

            } catch (IOException e) {
                throw new ChicoryException(e);
            }
        }
    }

    private static void writeClassesToCache(Path cachePath, Map<String, byte[]> classBytes)
            throws IOException {
        for (var entry : classBytes.entrySet()) {
            var className = entry.getKey().replace('.', '/') + ".class";
            var classFile = cachePath.resolve(className);
            Files.createDirectories(classFile.getParent());
            Files.write(classFile, entry.getValue());
        }
    }

    private static Map<String, byte[]> loadClassesFromCache(Path cachePath) throws IOException {
        Map<String, byte[]> classBytes = new HashMap<>();

        // Walk through all files in the cache directory
        try (var stream = Files.walk(cachePath)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".class"))
                    .forEach(
                            classFile -> {
                                try {
                                    // Convert file path back to class name
                                    // e.g., cachePath/foo/bar/Example.class -> foo.bar.Example
                                    String relativePath =
                                            cachePath.relativize(classFile).toString();
                                    String className =
                                            relativePath.replace('/', '.').replace(".class", "");

                                    // Read the class file bytes
                                    byte[] bytes = Files.readAllBytes(classFile);
                                    classBytes.put(className, bytes);
                                } catch (IOException e) {
                                    throw new RuntimeException(
                                            "Failed to read class file: " + classFile, e);
                                }
                            });
        }

        return classBytes;
    }
}
