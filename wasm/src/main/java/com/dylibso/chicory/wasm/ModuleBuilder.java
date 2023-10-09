package com.dylibso.chicory.wasm;

import com.dylibso.chicory.wasm.types.*;

class ModuleBuilder implements ParserListener {
    private Module module;

    public ModuleBuilder() {
        this.module = new Module();
    }

    public Module getModule() {
        return module;
    }

    @Override
    public void onSection(Section s) {
        switch (s.getSectionId()) {
            case SectionId.CUSTOM:
                module.addCustomSection((CustomSection) s);
                break;
            case SectionId.TYPE:
                module.setTypeSection((TypeSection) s);
                break;
            case SectionId.IMPORT:
                module.setImportSection((ImportSection) s);
                break;
            case SectionId.FUNCTION:
                module.setFunctionSection((FunctionSection) s);
                break;
            case SectionId.TABLE:
                module.setTableSection((TableSection) s);
                break;
            case SectionId.MEMORY:
                module.setMemorySection((MemorySection) s);
                break;
            case SectionId.GLOBAL:
                module.setGlobalSection((GlobalSection) s);
                break;
            case SectionId.EXPORT:
                module.setExportSection((ExportSection) s);
                break;
            case SectionId.START:
                module.setStartSection((StartSection) s);
                break;
            case SectionId.ELEMENT:
                module.setElementSection((ElementSection) s);
                break;
            case SectionId.CODE:
                module.setCodeSection((CodeSection) s);
                break;
            case SectionId.DATA:
                module.setDataSection((DataSection) s);
                break;
            default:
                System.out.println("Ignoring section with id: " + s.getSectionId());
                break;
        }
    }
}
