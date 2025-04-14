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

    public TypeSection typeSection() {
        return typeSection;
    }

    public FunctionSection functionSection() {
        return functionSection;
    }

    public ExportSection exportSection() {
        return exportSection;
    }

    public Optional<StartSection> startSection() {
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

    public Optional<DataCountSection> dataCountSection() {
        return dataCountSection;
    }

    public Optional<MemorySection> memorySection() {
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

    public Optional<TagSection> tagSection() {
        return tagSection;
    }

    public List<Integer> ignoredSections() {
        return ignoredSections;
    }

    public static Builder builder() {
        return new Builder();
    }

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

        public Builder setTypeSection(TypeSection ts) {
            this.typeSection = requireNonNull(ts);
            return this;
        }

        public Builder setImportSection(ImportSection is) {
            this.importSection = requireNonNull(is);
            return this;
        }

        public Builder setFunctionSection(FunctionSection fs) {
            this.functionSection = requireNonNull(fs);
            return this;
        }

        public Builder setTableSection(TableSection ts) {
            this.tableSection = requireNonNull(ts);
            return this;
        }

        public Builder setMemorySection(MemorySection ms) {
            this.memorySection = Optional.ofNullable(ms);
            return this;
        }

        public Builder setGlobalSection(GlobalSection gs) {
            this.globalSection = requireNonNull(gs);
            return this;
        }

        public Builder setExportSection(ExportSection es) {
            this.exportSection = requireNonNull(es);
            return this;
        }

        public Builder setStartSection(StartSection ss) {
            this.startSection = Optional.ofNullable(ss);
            return this;
        }

        public Builder setElementSection(ElementSection es) {
            this.elementSection = requireNonNull(es);
            return this;
        }

        public Builder setCodeSection(CodeSection cs) {
            this.codeSection = requireNonNull(cs);
            return this;
        }

        public Builder setDataSection(DataSection ds) {
            this.dataSection = requireNonNull(ds);
            return this;
        }

        public Builder setDataCountSection(DataCountSection dcs) {
            this.dataCountSection = Optional.ofNullable(dcs);
            return this;
        }

        public Builder setTagSection(TagSection ts) {
            this.tagSection = Optional.ofNullable(ts);
            return this;
        }

        public Builder addCustomSection(String name, CustomSection cs) {
            requireNonNull(name);
            requireNonNull(cs);
            this.customSections.put(name, cs);
            return this;
        }

        public Builder addIgnoredSection(int id) {
            this.ignoredSections.add(id);
            return this;
        }

        public Builder withValidation(boolean validate) {
            this.validate = validate;
            return this;
        }

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
                validator.validateTypes();
                validator.validateFunctions();
                validator.validateGlobals();
                validator.validateElements();
                validator.validateData();
                validator.validateTags();
                validator.validateTables();
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
