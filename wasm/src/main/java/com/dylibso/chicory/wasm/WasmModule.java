package com.dylibso.chicory.wasm;

import static java.util.Objects.requireNonNull;

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
import com.dylibso.chicory.wasm.types.TagSection;
import com.dylibso.chicory.wasm.types.TypeSection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a parsed WebAssembly module, containing all its sections.
 * This class provides access to the different parts of a Wasm module,
 * such as types, functions, tables, memories, globals, exports, imports,
 * element segments, data segments, custom sections, and metadata.
 * Instances are created via the {@link #builder()}.
 */
public final class WasmModule {
    private final Map<String, CustomSection> customSections;

    private final TypeSection typeSection;
    private final ImportSection importSection;
    private final FunctionSection functionSection;
    private final TableSection tableSection;
    private final Optional<MemorySection> memorySection;
    private final GlobalSection globalSection;
    private final ExportSection exportSection;
    private final Optional<StartSection> startSection;
    private final ElementSection elementSection;
    private final CodeSection codeSection;
    private final DataSection dataSection;
    private final Optional<DataCountSection> dataCountSection;
    private final Optional<TagSection> tagSection;
    private final List<Integer> ignoredSections;

    private WasmModule(
            TypeSection typeSection,
            ImportSection importSection,
            FunctionSection functionSection,
            TableSection tableSection,
            Optional<MemorySection> memorySection,
            GlobalSection globalSection,
            ExportSection exportSection,
            Optional<StartSection> startSection,
            ElementSection elementSection,
            CodeSection codeSection,
            DataSection dataSection,
            Optional<DataCountSection> dataCountSection,
            Optional<TagSection> tagSection,
            Map<String, CustomSection> customSections,
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
        this.tagSection = tagSection;
        this.customSections = Map.copyOf(customSections);
        this.ignoredSections = List.copyOf(ignoredSections);
    }

    /**
     * Returns the Type Section of the module, containing function type definitions.
     *
     * @return the {@link TypeSection}.
     */
    public TypeSection typeSection() {
        return typeSection;
    }

    /**
     * Returns the Function Section of the module, mapping internal function indices to type indices.
     *
     * @return the {@link FunctionSection}.
     */
    public FunctionSection functionSection() {
        return functionSection;
    }

    /**
     * Returns the Export Section of the module, listing exported items.
     *
     * @return the {@link ExportSection}.
     */
    public ExportSection exportSection() {
        return exportSection;
    }

    /**
     * Returns the Start Section of the module, specifying the start function index, if present.
     *
     * @return an {@link Optional} containing the {@link StartSection}, or empty if not present.
     */
    public Optional<StartSection> startSection() {
        return startSection;
    }

    /**
     * Returns the Import Section of the module, describing imported items.
     *
     * @return the {@link ImportSection}.
     */
    public ImportSection importSection() {
        return importSection;
    }

    /**
     * Returns the Code Section of the module, containing function bodies.
     *
     * @return the {@link CodeSection}.
     */
    public CodeSection codeSection() {
        return codeSection;
    }

    /**
     * Returns the Data Section of the module, initializing linear memory segments.
     *
     * @return the {@link DataSection}.
     */
    public DataSection dataSection() {
        return dataSection;
    }

    /**
     * Returns the Data Count Section, declaring the number of data segments, if present.
     * Required if the Data Section is present and the passive data segment proposal is used.
     *
     * @return an {@link Optional} containing the {@link DataCountSection}, or empty if not present.
     */
    public Optional<DataCountSection> dataCountSection() {
        return dataCountSection;
    }

    /**
     * Returns the Memory Section of the module, defining linear memories, if present.
     *
     * @return an {@link Optional} containing the {@link MemorySection}, or empty if not present.
     */
    public Optional<MemorySection> memorySection() {
        return memorySection;
    }

    /**
     * Returns the Global Section of the module, defining global variables.
     *
     * @return the {@link GlobalSection}.
     */
    public GlobalSection globalSection() {
        return globalSection;
    }

    /**
     * Returns the Table Section of the module, defining tables.
     *
     * @return the {@link TableSection}.
     */
    public TableSection tableSection() {
        return tableSection;
    }

    /**
     * Returns a list of all custom sections found in the module.
     * Note: If multiple sections share the same name, only the last one parsed with that name might be present
     * due to the underlying map storage used by the builder. Use {@link #customSection(String)} for specific access.
     *
     * @return an unmodifiable {@link List} of {@link CustomSection} values.
     */
    public List<CustomSection> customSections() {
        return new ArrayList<>(customSections.values());
    }

    /**
     * Retrieves a specific custom section by its name.
     *
     * @param name The name of the custom section (e.g., "name", "dylink.0").
     * @return The {@link CustomSection} if found, otherwise null.
     */
    public CustomSection customSection(String name) {
        return customSections.get(name);
    }

    /**
     * Convenience method to get the "name" custom section, cast to {@link NameCustomSection}.
     * Returns null if the "name" section is not present or not a {@link NameCustomSection}.
     *
     * @return The {@link NameCustomSection} or null.
     */
    public NameCustomSection nameSection() {
        return (NameCustomSection) customSections.get("name");
    }

    /**
     * Returns the Element Section of the module, initializing table elements.
     *
     * @return the {@link ElementSection}.
     */
    public ElementSection elementSection() {
        return elementSection;
    }

    /**
     * Returns the Tag Section (part of Exception Handling proposal), if present.
     *
     * @return an {@link Optional} containing the {@link TagSection}, or empty if not present.
     */
    public Optional<TagSection> tagSection() {
        return tagSection;
    }

    /**
     * Returns a list of section IDs that were present in the Wasm binary but ignored during parsing
     * (e.g., due to unknown section IDs or configuration).
     *
     * @return an unmodifiable {@link List} of ignored section IDs.
     */
    public List<Integer> ignoredSections() {
        return ignoredSections;
    }

    /**
     * Creates a new builder for constructing a {@link WasmModule}.
     *
     * @return A new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for programmatically constructing or modifying a {@link WasmModule}.
     * Allows setting each section individually and controlling validation.
     */
    public static final class Builder {
        private TypeSection typeSection = TypeSection.builder().build();
        private ImportSection importSection = ImportSection.builder().build();
        private FunctionSection functionSection = FunctionSection.builder().build();
        private TableSection tableSection = TableSection.builder().build();
        private Optional<MemorySection> memorySection = Optional.empty();
        private GlobalSection globalSection = GlobalSection.builder().build();
        private ExportSection exportSection = ExportSection.builder().build();
        private Optional<StartSection> startSection = Optional.empty();
        private ElementSection elementSection = ElementSection.builder().build();
        private CodeSection codeSection = CodeSection.builder().build();
        private DataSection dataSection = DataSection.builder().build();
        private Optional<DataCountSection> dataCountSection = Optional.empty();
        private Optional<TagSection> tagSection = Optional.empty();
        private final Map<String, CustomSection> customSections = new HashMap<>();
        private final List<Integer> ignoredSections = new ArrayList<>();
        private boolean validate = true;

        private Builder() {}

        /**
         * Sets the Type Section for the module being built.
         *
         * @param ts the {@link TypeSection}
         * @return this builder instance
         */
        public Builder setTypeSection(TypeSection ts) {
            this.typeSection = requireNonNull(ts);
            return this;
        }

        /**
         * Sets the Import Section for the module being built.
         *
         * @param is the {@link ImportSection}
         * @return this builder instance
         */
        public Builder setImportSection(ImportSection is) {
            this.importSection = requireNonNull(is);
            return this;
        }

        /**
         * Sets the Function Section for the module being built.
         *
         * @param fs the {@link FunctionSection}
         * @return this builder instance
         */
        public Builder setFunctionSection(FunctionSection fs) {
            this.functionSection = requireNonNull(fs);
            return this;
        }

        /**
         * Sets the Table Section for the module being built.
         *
         * @param ts the {@link TableSection}
         * @return this builder instance
         */
        public Builder setTableSection(TableSection ts) {
            this.tableSection = requireNonNull(ts);
            return this;
        }

        /**
         * Sets the Memory Section for the module being built. Use null to indicate no memory section.
         *
         * @param ms the {@link MemorySection} or null
         * @return this builder instance
         */
        public Builder setMemorySection(MemorySection ms) {
            this.memorySection = Optional.ofNullable(ms);
            return this;
        }

        /**
         * Sets the Global Section for the module being built.
         *
         * @param gs the {@link GlobalSection}
         * @return this builder instance
         */
        public Builder setGlobalSection(GlobalSection gs) {
            this.globalSection = requireNonNull(gs);
            return this;
        }

        /**
         * Sets the Export Section for the module being built.
         *
         * @param es the {@link ExportSection}
         * @return this builder instance
         */
        public Builder setExportSection(ExportSection es) {
            this.exportSection = requireNonNull(es);
            return this;
        }

        /**
         * Sets the Start Section for the module being built. Use null to indicate no start section.
         *
         * @param ss the {@link StartSection} or null
         * @return this builder instance
         */
        public Builder setStartSection(StartSection ss) {
            this.startSection = Optional.ofNullable(ss);
            return this;
        }

        /**
         * Sets the Element Section for the module being built.
         *
         * @param es the {@link ElementSection}
         * @return this builder instance
         */
        public Builder setElementSection(ElementSection es) {
            this.elementSection = requireNonNull(es);
            return this;
        }

        /**
         * Sets the Code Section for the module being built.
         *
         * @param cs the {@link CodeSection}
         * @return this builder instance
         */
        public Builder setCodeSection(CodeSection cs) {
            this.codeSection = requireNonNull(cs);
            return this;
        }

        /**
         * Sets the Data Section for the module being built.
         *
         * @param ds the {@link DataSection}
         * @return this builder instance
         */
        public Builder setDataSection(DataSection ds) {
            this.dataSection = requireNonNull(ds);
            return this;
        }

        /**
         * Sets the Data Count Section for the module being built. Use null to indicate no data count section.
         *
         * @param dcs the {@link DataCountSection} or null
         * @return this builder instance
         */
        public Builder setDataCountSection(DataCountSection dcs) {
            this.dataCountSection = Optional.ofNullable(dcs);
            return this;
        }

        /**
         * Sets the Tag Section for the module being built. Use null to indicate no tag section.
         *
         * @param ts the {@link TagSection} or null
         * @return this builder instance
         */
        public Builder setTagSection(TagSection ts) {
            this.tagSection = Optional.ofNullable(ts);
            return this;
        }

        /**
         * Adds a custom section to the module being built. If a section with the same name
         * already exists, it will be replaced.
         *
         * @param name the name of the custom section
         * @param cs the {@link CustomSection}
         * @return this builder instance
         */
        public Builder addCustomSection(String name, CustomSection cs) {
            requireNonNull(name);
            requireNonNull(cs);
            this.customSections.put(name, cs);
            return this;
        }

        /**
         * Adds an ID to the list of ignored sections.
         *
         * @param id the section ID that was ignored
         * @return this builder instance
         */
        public Builder addIgnoredSection(int id) {
            this.ignoredSections.add(id);
            return this;
        }

        /**
         * Sets whether validation should be performed when {@link #build()} is called.
         * Validation is enabled by default. Disabling validation might be useful for tools
         * that process potentially invalid modules, but can lead to runtime errors if the
         * resulting module is used in an engine that expects valid modules.
         *
         * @param validate true to enable validation (default), false to disable.
         * @return this builder instance
         */
        public Builder withValidation(boolean validate) {
            this.validate = validate;
            return this;
        }

        /**
         * Constructs the {@link WasmModule} instance from the configured sections.
         * Performs validation checks if validation is enabled (default).
         *
         * @return the built {@link WasmModule}
         * @throws InvalidException if validation is enabled and the module is found to be invalid.
         */
        public WasmModule build() {
            var module =
                    new WasmModule(
                            typeSection,
                            importSection,
                            functionSection,
                            tableSection,
                            memorySection,
                            globalSection,
                            exportSection,
                            startSection,
                            elementSection,
                            codeSection,
                            dataSection,
                            dataCountSection,
                            tagSection,
                            customSections,
                            ignoredSections);

            var validator = new Validator(module);
            validator.validateModule();
            if (validate) {
                validator.validateFunctions();
                validator.validateGlobals();
                validator.validateElements();
                validator.validateData();
                validator.validateTags();
            }

            return module;
        }
    }

    // Comparison uses everything but the custom section
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof WasmModule)) {
            return false;
        }
        WasmModule that = (WasmModule) o;
        return Objects.equals(typeSection, that.typeSection)
                && Objects.equals(importSection, that.importSection)
                && Objects.equals(functionSection, that.functionSection)
                && Objects.equals(tableSection, that.tableSection)
                && Objects.equals(memorySection, that.memorySection)
                && Objects.equals(globalSection, that.globalSection)
                && Objects.equals(exportSection, that.exportSection)
                && Objects.equals(startSection, that.startSection)
                && Objects.equals(elementSection, that.elementSection)
                && Objects.equals(codeSection, that.codeSection)
                && Objects.equals(dataSection, that.dataSection)
                && Objects.equals(dataCountSection, that.dataCountSection)
                && Objects.equals(tagSection, that.tagSection)
                && Objects.equals(ignoredSections, that.ignoredSections);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                typeSection,
                importSection,
                functionSection,
                tableSection,
                memorySection,
                globalSection,
                exportSection,
                startSection,
                elementSection,
                codeSection,
                dataSection,
                dataCountSection,
                tagSection);
    }
}
