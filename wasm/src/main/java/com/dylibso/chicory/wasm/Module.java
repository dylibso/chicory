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

    private TypeSection typeSection = new TypeSection();
    private ImportSection importSection = new ImportSection();
    private FunctionSection functionSection = new FunctionSection();
    private TableSection tableSection = new TableSection();
    private MemorySection memorySection;
    private GlobalSection globalSection = new GlobalSection();
    private ExportSection exportSection = new ExportSection();
    private StartSection startSection;
    private ElementSection elementSection = new ElementSection();
    private CodeSection codeSection = new CodeSection();
    private DataSection dataSection = new DataSection();
    private DataCountSection dataCountSection;

    private final List<Integer> ignoredSections = new ArrayList();

    public Module() {
        this.customSections = new HashMap<>();
    }

    public void setTypeSection(TypeSection typeSection) {
        this.typeSection = requireNonNull(typeSection);
    }

    public void setFunctionSection(FunctionSection functionSection) {
        this.functionSection = requireNonNull(functionSection);
    }

    public void setExportSection(ExportSection exportSection) {
        this.exportSection = requireNonNull(exportSection);
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

    public void setStartSection(StartSection startSection) {
        this.startSection = startSection;
    }

    public void setImportSection(ImportSection importSection) {
        this.importSection = requireNonNull(importSection);
    }

    public CodeSection codeSection() {
        return codeSection;
    }

    public void setCodeSection(CodeSection codeSection) {
        this.codeSection = requireNonNull(codeSection);
    }

    public void setDataSection(DataSection dataSection) {
        this.dataSection = requireNonNull(dataSection);
    }

    public void setDataCountSection(DataCountSection dataCountSection) {
        this.dataCountSection = dataCountSection;
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

    public void setMemorySection(MemorySection memorySection) {
        this.memorySection = memorySection;
    }

    public GlobalSection globalSection() {
        return globalSection;
    }

    public void setGlobalSection(GlobalSection globalSection) {
        this.globalSection = requireNonNull(globalSection);
    }

    public TableSection tableSection() {
        return tableSection;
    }

    public void setTableSection(TableSection tableSection) {
        this.tableSection = requireNonNull(tableSection);
    }

    public void addCustomSection(CustomSection customSection) {
        this.customSections.put(customSection.name(), customSection);
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

    public void setElementSection(ElementSection elementSection) {
        this.elementSection = requireNonNull(elementSection);
    }

    public void addIgnoredSection(int id) {
        ignoredSections.add(id);
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
