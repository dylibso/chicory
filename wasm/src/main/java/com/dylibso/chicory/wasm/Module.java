package com.dylibso.chicory.wasm;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Module {
    private final HashMap<String, CustomSection> customSections;

    private TypeSection typeSection;
    private ImportSection importSection;
    private FunctionSection functionSection;
    private TableSection tableSection;
    private MemorySection memorySection;
    private GlobalSection globalSection;
    private ExportSection exportSection;
    private StartSection startSection;
    private ElementSection elementSection;
    private CodeSection codeSection;
    private DataSection dataSection;
    private DataCountSection dataCountSection;

    public Module() {
        this.customSections = new HashMap<>();
    }

    public void setTypeSection(TypeSection typeSection) {
        this.typeSection = typeSection;
    }

    public void setFunctionSection(FunctionSection functionSection) {
        this.functionSection = functionSection;
    }

    public void setExportSection(ExportSection exportSection) {
        this.exportSection = exportSection;
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
        this.importSection = importSection;
    }

    public CodeSection codeSection() {
        return codeSection;
    }

    public void setCodeSection(CodeSection codeSection) {
        this.codeSection = codeSection;
    }

    public void setDataSection(DataSection dataSection) {
        this.dataSection = dataSection;
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
        this.globalSection = globalSection;
    }

    public TableSection tableSection() {
        return tableSection;
    }

    public void setTableSection(TableSection tableSection) {
        this.tableSection = tableSection;
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
        this.elementSection = elementSection;
    }
}
