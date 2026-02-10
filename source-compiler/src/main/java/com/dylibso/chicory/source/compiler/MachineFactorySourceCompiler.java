package com.dylibso.chicory.source.compiler;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.source.compiler.internal.Compiler;
import com.dylibso.chicory.source.compiler.internal.JavaSourceCompiler;
import com.dylibso.chicory.source.compiler.internal.SimpleSourceCodeCollector;
import com.dylibso.chicory.source.compiler.internal.SourceCodeCollector;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.WasmModule;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

/**
 * Compiles WASM function bodies to Java source code that can be compiled and used as a machine
 * factory for {@link Instance}'s.
 */
public final class MachineFactorySourceCompiler {

    private MachineFactorySourceCompiler() {}

    /**
     * The compile method reference can be used as machine factory in instance builders.
     *
     * <pre>
     * var instance = Instance.builder(Parser.parse(is))
     *         .withMachineFactory(MachineFactorySourceCompiler::compile)
     *         .build();
     * </pre>
     *
     * <p>Every instance created by the builder will pay the cost of compiling the module.
     *
     * @see #compile(WasmModule) If you want to compile the module only once for multiple instances.
     */
    public static Machine compile(Instance instance) {
        return compile(instance.module()).apply(instance);
    }

    /**
     * Compiles a machine factory that can be used in instance builders. The module is only compiled
     * once and the machine factory is reused for every instance created by the builder.
     *
     * <pre>
     * var module  = Parser.parse(is);
     * var builder = Instance.builder(module)
     *         .withMachineFactory(MachineFactorySourceCompiler.compile(module));
     * var instance1 = builder.build();
     * var instance2 = builder.build();
     * </pre>
     */
    public static Function<Instance, Machine> compile(WasmModule module) {
        return new MachineFactory(module);
    }

    /**
     * Configures a compiler that can compile a machine factory. The builder allows you to
     * configure the compiler options used to compile the module to Java source.
     */
    public static Builder builder(WasmModule module) {
        return new Builder(module);
    }

    public static final class Builder {
        private final WasmModule module;
        private String className;
        private Path compileTargetDir;

        private String moduleName;
        private boolean dumpSources;

        private Builder(WasmModule module) {
            this.module = module;
        }

        public Builder withClassName(String className) {
            this.className = className;
            return this;
        }

        public Builder withCompileTargetDir(Path compileTargetDir) {
            this.compileTargetDir = compileTargetDir;
            return this;
        }

        public Builder withModuleName(String moduleName) {
            this.moduleName = moduleName;
            return this;
        }

        public Builder withDumpSources(boolean dumpSources) {
            this.dumpSources = dumpSources;
            return this;
        }

        public Function<Instance, Machine> compile() {
            SourceCodeCollector collector = new SimpleSourceCodeCollector();
            try {
                var compilerBuilder = Compiler.builder(module).withSourceCodeCollector(collector);
                if (className != null) {
                    compilerBuilder.withClassName(className);
                }
                compilerBuilder.build().compile();
            } finally {
                if (dumpSources && moduleName != null) {
                    dumpSourcesToTarget(collector, moduleName);
                }
            }

            try {
                Path targetDir = compileTargetDir;
                if (targetDir == null) {
                    targetDir = Files.createTempDirectory("chicory-compiled-");
                }

                String classpath = buildClasspath();
                JavaSourceCompiler.compile(collector.sourceFiles(), targetDir, classpath);

                String mainClassName = collector.mainClassName();
                Class<?> machineClass = loadClass(mainClassName, targetDir);

                try {
                    Constructor<?> constructor = machineClass.getConstructor(Instance.class);
                    return instance -> {
                        try {
                            return (Machine) constructor.newInstance(instance);
                        } catch (ReflectiveOperationException e) {
                            throw new ChicoryException("Failed to instantiate machine", e);
                        }
                    };
                } catch (NoSuchMethodException e) {
                    throw new ChicoryException(
                            "Failed to find constructor for " + mainClassName + "(Instance)", e);
                }
            } catch (IOException e) {
                throw new ChicoryException("Failed to compile Java sources", e);
            }
        }

        private String buildClasspath() {
            String fullClasspath = System.getProperty("java.class.path");

            // Prepend runtime.jar if found (for library distribution compatibility)
            Class<?> instanceClass = Instance.class;
            String resourcePath = instanceClass.getName().replace('.', '/') + ".class";
            URL resource = instanceClass.getClassLoader().getResource(resourcePath);
            if (resource != null) {
                String path = resource.getPath();
                if (path.contains("!")) {
                    String jarPath = path.substring(0, path.indexOf("!"));
                    if (jarPath.startsWith("file:")) {
                        jarPath = jarPath.substring(5);
                    }
                    if (!fullClasspath.contains(jarPath)) {
                        return jarPath + File.pathSeparator + fullClasspath;
                    }
                }
            }
            return fullClasspath;
        }

        private Class<?> loadClass(String className, Path classDir) throws IOException {
            try {
                URL url = classDir.toUri().toURL();
                URLClassLoader classLoader =
                        new URLClassLoader(new URL[] {url}, getClass().getClassLoader());
                return classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new ChicoryException("Failed to load compiled class: " + className, e);
            }
        }

        private void dumpSourcesToTarget(SourceCodeCollector collector, String moduleName) {
            try {
                String classPath =
                        getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                Path targetDir = Path.of(classPath).getParent();

                Path dumpDir = targetDir.resolve("source-dump").resolve(moduleName);
                Files.createDirectories(dumpDir);

                for (Map.Entry<String, String> entry : collector.sourceFiles().entrySet()) {
                    String className = entry.getKey();
                    String source = entry.getValue();

                    String packagePath = className.substring(0, className.lastIndexOf('.'));
                    Path packageDir = dumpDir.resolve(packagePath.replace('.', File.separatorChar));
                    Files.createDirectories(packageDir);

                    String simpleName = className.substring(className.lastIndexOf('.') + 1);
                    Path sourceFile = packageDir.resolve(simpleName + ".java");
                    Files.writeString(sourceFile, source);
                }
            } catch (IOException | RuntimeException e) {
                // Ignore - test-only debugging feature
            }
        }
    }

    private static final class MachineFactory implements Function<Instance, Machine> {
        private final WasmModule module;
        private final Function<Instance, Machine> factory;

        MachineFactory(WasmModule module) {
            this.module = module;
            this.factory = builder(module).compile();
        }

        @Override
        public Machine apply(Instance instance) {
            if (instance.module() != module) {
                throw new IllegalArgumentException("Instance module does not match factory module");
            }
            return factory.apply(instance);
        }
    }
}
