package com.dylibso.chicory.wasm;

import com.dylibso.chicory.wasm.types.*;
import java.util.ArrayList;
import java.util.List;

public class Module {
    private List<CustomSection> customSections;
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

    public Module() {
        this.customSections = new ArrayList<>();
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

    public TypeSection getTypeSection() {
        return typeSection;
    }

    public FunctionSection getFunctionSection() {
        return functionSection;
    }

    public ExportSection getExportSection() {
        return exportSection;
    }

    public StartSection getStartSection() {
        return startSection;
    }

    public ImportSection getImportSection() {
        return importSection;
    }

    public void setStartSection(StartSection startSection) {
        this.startSection = startSection;
    }

    public void setImportSection(ImportSection importSection) {
        this.importSection = importSection;
    }

    public CodeSection getCodeSection() {
        return codeSection;
    }

    public void setCodeSection(CodeSection codeSection) {
        this.codeSection = codeSection;
    }

    public void setDataSection(DataSection dataSection) {
        this.dataSection = dataSection;
    }

    public DataSection getDataSection() {
        return dataSection;
    }

    public MemorySection getMemorySection() {
        return memorySection;
    }

    public void setMemorySection(MemorySection memorySection) {
        this.memorySection = memorySection;
    }

    public GlobalSection getGlobalSection() {
        return globalSection;
    }

    public void setGlobalSection(GlobalSection globalSection) {
        this.globalSection = globalSection;
    }

    public TableSection getTableSection() {
        return tableSection;
    }

    public void setTableSection(TableSection tableSection) {
        this.tableSection = tableSection;
    }

    public void addCustomSection(CustomSection customSection) {
        this.customSections.add(customSection);
    }

    public List<CustomSection> getCustomSections() {
        return customSections;
    }

    public ElementSection getElementSection() {
        return elementSection;
    }

    public void setElementSection(ElementSection elementSection) {
        this.elementSection = elementSection;
    }
}
