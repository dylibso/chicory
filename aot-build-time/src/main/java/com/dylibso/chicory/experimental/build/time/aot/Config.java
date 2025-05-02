package com.dylibso.chicory.experimental.build.time.aot;

import java.nio.file.Path;
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

    private Config(
            Path wasmFile,
            String name,
            Path targetClassFolder,
            Path targetSourceFolder,
            Path targetWasmFolder) {
        this.wasmFile = wasmFile;
        this.name = name;
        this.targetClassFolder = targetClassFolder;
        this.targetSourceFolder = targetSourceFolder;
        this.targetWasmFolder = targetWasmFolder;
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

        private Builder() {}

        public Builder withWasmFile(Path wasmFile) {
            this.wasmFile = wasmFile;
            return this;
        }

        /**
         * @deprecated use {@link #withModuleClass(String)} instead
         */
        @Deprecated
        @SuppressWarnings("InlineMeSuggester")
        public Builder withName(String name) {
            return withModuleClass(name + "Module");
        }

        public Builder withModuleClass(String name) {
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

        public Config build() {
            return new Config(
                    wasmFile, name, targetClassFolder, targetSourceFolder, targetWasmFolder);
        }
    }
}
