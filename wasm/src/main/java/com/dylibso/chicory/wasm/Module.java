package com.dylibso.chicory.wasm;

import static java.util.Objects.requireNonNull;

import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.exceptions.InvalidException;
import com.dylibso.chicory.wasm.types.CodeSection;
import com.dylibso.chicory.wasm.types.CustomSection;
import com.dylibso.chicory.wasm.types.DataCountSection;
import com.dylibso.chicory.wasm.types.DataSection;
import com.dylibso.chicory.wasm.types.ElementSection;
import com.dylibso.chicory.wasm.types.ExportSection;
import com.dylibso.chicory.wasm.types.FunctionSection;
import com.dylibso.chicory.wasm.types.GlobalSection;
import com.dylibso.chicory.wasm.types.ImportSection;
import com.dylibso.chicory.wasm.types.MemorySection;
import com.dylibso.chicory.wasm.types.NameCustomSection;
import com.dylibso.chicory.wasm.types.StartSection;
import com.dylibso.chicory.wasm.types.TableSection;
import com.dylibso.chicory.wasm.types.TypeSection;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class Module {
    private final HashMap<String, CustomSection> customSections;

    private final TypeSection typeSection;
    private final ImportSection importSection;
    private final FunctionSection functionSection;
    private final TableSection tableSection;
    private final MemorySection memorySection;
    private final GlobalSection globalSection;
    private final ExportSection exportSection;
    private final StartSection startSection;
    private final ElementSection elementSection;
    private final CodeSection codeSection;
    private final DataSection dataSection;
    private final DataCountSection dataCountSection;
    private final List<Integer> ignoredSections;

    Module(
            TypeSection typeSection,
            ImportSection importSection,
            FunctionSection functionSection,
            TableSection tableSection,
            MemorySection memorySection,
            GlobalSection globalSection,
            ExportSection exportSection,
            StartSection startSection,
            ElementSection elementSection,
            CodeSection codeSection,
            DataSection dataSection,
            DataCountSection dataCountSection,
            HashMap<String, CustomSection> customSections,
            List<Integer> ignoredSections) {
        this.typeSection = requireNonNull(typeSection);
        this.importSection = requireNonNull(importSection);
        this.functionSection = requireNonNull(functionSection);
        this.tableSection = requireNonNull(tableSection);
        this.memorySection = memorySection;
        this.globalSection = requireNonNull(globalSection);
        this.exportSection = requireNonNull(exportSection);
        this.startSection = startSection;
        this.elementSection = requireNonNull(elementSection);
        this.codeSection = requireNonNull(codeSection);
        this.dataSection = requireNonNull(dataSection);
        this.dataCountSection = dataCountSection;
        this.customSections = requireNonNull(customSections);
        this.ignoredSections = requireNonNull(ignoredSections);
    }

    public TypeSection typeSection() {
        return typeSection;
    }

    public FunctionSection functionSection() {
        return functionSection;
    }

    public ExportSection exportSection() {
        return exportSection;
    }

    public StartSection startSection() {
        return startSection;
    }

    public ImportSection importSection() {
        return importSection;
    }

    public CodeSection codeSection() {
        return codeSection;
    }

    public DataSection dataSection() {
        return dataSection;
    }

    public DataCountSection dataCountSection() {
        return dataCountSection;
    }

    public MemorySection memorySection() {
        return memorySection;
    }

    public GlobalSection globalSection() {
        return globalSection;
    }

    public TableSection tableSection() {
        return tableSection;
    }

    public List<CustomSection> customSections() {
        return new ArrayList<>(customSections.values());
    }

    public CustomSection customSection(String name) {
        return customSections.get(name);
    }

    public NameCustomSection nameSection() {
        return (NameCustomSection) customSections.get("name");
    }

    public ElementSection elementSection() {
        return elementSection;
    }

    /**
     * Creates a {@link Builder} for the specified {@link InputStream}
     *
     * @param input the input stream
     * @return a {@link Builder} for reading the module definition from the specified input stream
     */
    public static Builder builder(InputStream input) {
        return new Builder(() -> input);
    }

    /**
     * Creates a {@link Builder} for the specified {@link ByteBuffer}
     *
     * @param buffer the buffer
     * @return a {@link Builder} for reading the module definition from the specified buffer
     */
    public static Builder builder(ByteBuffer buffer) {
        return builder(buffer.array());
    }

    /**
     * Creates a {@link Builder} for the specified byte array
     *
     * @param buffer the buffer
     * @return a {@link Builder} for reading the module definition from the specified buffer
     */
    public static Builder builder(byte[] buffer) {
        return new Builder(() -> new ByteArrayInputStream(buffer));
    }

    /**
     * Creates a {@link Builder} for the specified {@link File} resource
     *
     * @param file the path of the resource
     * @return a {@link Builder} for reading the module definition from the specified file
     */
    public static Builder builder(File file) {
        return builder(file.toPath());
    }

    /**
     * Creates a {@link Builder} for the specified {@link Path} resource
     *
     * @param path the path of the resource
     * @return a {@link Builder} for reading the module definition from the specified path
     */
    public static Builder builder(Path path) {
        return new Builder(
                () -> {
                    try {
                        return Files.newInputStream(path);
                    } catch (IOException e) {
                        throw new IllegalArgumentException("Error opening file: " + path, e);
                    }
                });
    }

    public static class Builder {
        private final Supplier<InputStream> inputStreamSupplier;
        private ModuleType moduleType = ModuleType.BINARY;

        private Builder(Supplier<InputStream> inputStreamSupplier) {
            this.inputStreamSupplier = Objects.requireNonNull(inputStreamSupplier);
        }

        public Builder withType(ModuleType type) {
            this.moduleType = type;
            return this;
        }

        public Module build() {
            final Parser parser = new Parser();
            switch (this.moduleType) {
                case BINARY:
                    try (final InputStream is = inputStreamSupplier.get()) {
                        return parser.parseModule(is);
                    } catch (IOException e) {
                        throw new ChicoryException(e);
                    }
                default:
                    throw new InvalidException(
                            "Text format parsing is not implemented, but you can use wat2wasm"
                                    + " through Chicory.");
            }
        }
    }
}
