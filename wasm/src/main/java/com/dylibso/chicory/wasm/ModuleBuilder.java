package com.dylibso.chicory.wasm;

import com.dylibso.chicory.wasm.types.*;

class ModuleBuilder implements ParserListener {
    private Module module;

    public ModuleBuilder() {
        this.module = new Module();
    }

    public Module getModule() { return module; }

    @Override
    public void onSection(Section s) {
        switch (s.getSectionId()) {
            case SectionId.CUSTOM -> module.addCustomSection((CustomSection) s);
            case SectionId.TYPE -> module.setTypeSection((TypeSection) s);
            case SectionId.IMPORT -> module.setImportSection((ImportSection) s);
            case SectionId.FUNCTION -> module.setFunctionSection((FunctionSection) s);
            case SectionId.TABLE -> module.setTableSection((TableSection) s);
            case SectionId.MEMORY -> module.setMemorySection((MemorySection) s);
            case SectionId.GLOBAL -> module.setGlobalSection((GlobalSection) s);
            case SectionId.EXPORT -> module.setExportSection((ExportSection) s);
            case SectionId.START -> module.setStartSection((StartSection) s);
            case SectionId.ELEMENT -> module.setElementSection((ElementSection) s);
            case SectionId.CODE -> module.setCodeSection((CodeSection) s);
            case SectionId.DATA -> module.setDataSection((DataSection) s);
            default -> System.out.println("Ignoring section with id: " + s.getSectionId());
        }
    }
}
