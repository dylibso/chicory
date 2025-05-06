package com.dylibso.chicory.experimental.build.time.aot;

import com.dylibso.chicory.compiler.internal.InterpreterFallback;
import java.nio.file.Path;
import java.util.Set;
import java.util.StringJoiner;

public final class Config {
    /**
     * the wasm module to be used
     */
    private final Path wasmFile;

    /**
     * the base name to be used for the generated classes
     */
    private final String name;

    /**
     * the target folder to generate classes
     */
    private final Path targetClassFolder;

    /**
     * the target source folder to generate the java source code Machine implementation
     */
    private final Path targetSourceFolder;

    /**
     * the target wasm folder to generate the meta, stripped, wasm module
     */
    private final Path targetWasmFolder;

    /**
     * the interpreter fallback to be used
     */
    public final InterpreterFallback interpreterFallback;

    /**
     * the set of interpreted functions
     */
    private final Set<Integer> interpretedFunctions;

    private Config(
            Path wasmFile,
            String name,
            Path targetClassFolder,
            Path targetSourceFolder,
            Path targetWasmFolder,
            InterpreterFallback interpreterFallback,
            Set<Integer> interpretedFunctions) {
        this.wasmFile = wasmFile;
        this.name = name;
        this.targetClassFolder = targetClassFolder;
        this.targetSourceFolder = targetSourceFolder;
        this.targetWasmFolder = targetWasmFolder;
        this.interpreterFallback = interpreterFallback;
        this.interpretedFunctions = interpretedFunctions;
    }

    public Path wasmFile() {
        return wasmFile;
    }

    public String name() {
        return name;
    }

    public Path targetClassFolder() {
        return targetClassFolder;
    }

    public Path targetSourceFolder() {
        return targetSourceFolder;
    }

    public Path targetWasmFolder() {
        return targetWasmFolder;
    }

    public InterpreterFallback interpreterFallback() {
        return interpreterFallback;
    }

    public Set<Integer> interpretedFunctions() {
        return interpretedFunctions;
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("StringSplitter")
    public String getPackageName() {
        var split = name.split("\\.");
        StringJoiner packageName = new StringJoiner(".");
        for (int i = 0; i < split.length - 1; i++) {
            packageName.add(split[i]);
        }
        return packageName.toString();
    }

    @SuppressWarnings("StringSplitter")
    public String getBaseName() {
        var split = name.split("\\.");
        return split[split.length - 1];
    }

    public static final class Builder {
        private Path wasmFile;
        private String name;
        private Path targetClassFolder;
        private Path targetSourceFolder;
        private Path targetWasmFolder;
        private InterpreterFallback interpreterFallback = InterpreterFallback.FAIL;
        private Set<Integer> interpretedFunctions;

        private Builder() {}

        public Builder withWasmFile(Path wasmFile) {
            this.wasmFile = wasmFile;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withTargetClassFolder(Path targetClassFolder) {
            this.targetClassFolder = targetClassFolder;
            return this;
        }

        public Builder withTargetSourceFolder(Path targetSourceFolder) {
            this.targetSourceFolder = targetSourceFolder;
            return this;
        }

        public Builder withTargetWasmFolder(Path targetWasmFolder) {
            this.targetWasmFolder = targetWasmFolder;
            return this;
        }

        public Builder withInterpreterFallback(InterpreterFallback interpreterFallback) {
            this.interpreterFallback = interpreterFallback;
            return this;
        }

        public Builder withInterpretedFunctions(Set<Integer> interpretedFunctions) {
            this.interpretedFunctions = interpretedFunctions;
            return this;
        }

        public Config build() {
            return new Config(
                    wasmFile,
                    name,
                    targetClassFolder,
                    targetSourceFolder,
                    targetWasmFolder,
                    interpreterFallback,
                    interpretedFunctions);
        }
    }
}
