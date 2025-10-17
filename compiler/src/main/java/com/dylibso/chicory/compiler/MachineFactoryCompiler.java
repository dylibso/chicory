package com.dylibso.chicory.compiler;

import com.dylibso.chicory.compiler.internal.ClassLoadingCollector;
import com.dylibso.chicory.compiler.internal.MachineFactory;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.WasmModule;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

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
            try {

                // Can we load the byte codes from the cache?
                var useCache = cache != null && module.digest() != null;
                if (useCache) {
                    byte[] cachedData = cache.get(module.digest());
                    if (cachedData != null) {
                        var collector = loadCollector(cachedData);
                        return new MachineFactory(module, collector.machineFactory());
                    }
                }

                // Compile the byte codes...
                var result =
                        compilerBuilder
                                .withClassCollectorFactory(ClassLoadingCollector::new)
                                .build()
                                .compile();
                var collector = (ClassLoadingCollector) result.collector();

                if (useCache) {
                    // store results in the cache to speed the next time.
                    cache.putIfAbsent(module.digest(), storeClassLoadingCollector(collector));
                }

                return new MachineFactory(module, collector.machineFactory());
            } catch (IOException e) {
                throw new ChicoryException(e);
            }
        }
    }

    private static byte[] storeClassLoadingCollector(ClassLoadingCollector collector) {
        try {
            // Create JAR in memory
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (JarOutputStream jos = new JarOutputStream(baos)) {
                // Store the properties file
                var properties = new Properties();
                properties.put("mainClass", collector.mainClassName());
                ByteArrayOutputStream propsBaos = new ByteArrayOutputStream();
                properties.store(propsBaos, "");

                JarEntry propsEntry = new JarEntry("wasm-module.properties");
                jos.putNextEntry(propsEntry);
                jos.write(propsBaos.toByteArray());
                jos.closeEntry();

                // Store all class files
                for (var entry : collector.classBytes().entrySet()) {
                    var className = entry.getKey().replace('.', '/') + ".class";
                    JarEntry classEntry = new JarEntry(className);
                    jos.putNextEntry(classEntry);
                    jos.write(entry.getValue());
                    jos.closeEntry();
                }
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ClassLoadingCollector loadCollector(byte[] jarData) throws IOException {
        var collector = new ClassLoadingCollector();

        // It was previously compiled, just load it from JAR.
        try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(jarData))) {
            var properties = new Properties();
            String mainClass = null;
            Map<String, byte[]> classes = new HashMap<>();

            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.getName().equals("wasm-module.properties")) {
                    // Load properties
                    properties.load(jis);
                    mainClass = properties.getProperty("mainClass");
                } else if (entry.getName().endsWith(".class")) {
                    // Load class file
                    String className = entry.getName().replace('/', '.').replace(".class", "");

                    // Read class file bytes
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = jis.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                    }
                    classes.put(className, baos.toByteArray());
                }
                jis.closeEntry();
            }

            // Add classes to collector
            for (var classEntry : classes.entrySet()) {
                if (classEntry.getKey().equals(mainClass)) {
                    collector.putMainClass(mainClass, classEntry.getValue());
                } else {
                    collector.put(classEntry.getKey(), classEntry.getValue());
                }
            }
        }
        return collector;
    }
}
