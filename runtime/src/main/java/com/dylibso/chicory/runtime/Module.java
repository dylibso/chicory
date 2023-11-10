package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.exceptions.InvalidException;
import com.dylibso.chicory.wasm.types.*;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class Module {
    private com.dylibso.chicory.wasm.Module module;
    private NameSection nameSec;

    private HashMap<String, Export> exports;

    public static Module build(File wasmFile) {
        var parser = new Parser(wasmFile);
        return new Module(parser.parseModule());
    }

    public static Module build(InputStream inputWasmFile) {
        var parser = new Parser(inputWasmFile);
        return new Module(parser.parseModule());
    }

    public static Module build(ByteBuffer buffer) {
        var parser = new Parser(buffer);
        return new Module(parser.parseModule());
    }

    public static Module build(File wasmFile, ModuleType type) {
        switch (type) {
            case TEXT:
                return build(wasmFile);
            default:
                // TODO: implement me
                throw new InvalidException("type mismatch");
        }
    }

    protected Module(com.dylibso.chicory.wasm.Module module) {
        this.module = module;
        this.exports = new HashMap<>();
        if (module.getExportSection() != null) {
            for (var e : module.getExportSection().getExports()) {
                exports.put(e.getName(), e);
            }
        }
    }

    public Instance instantiate() {
        return this.instantiate(new HostFunction[0]);
    }

    public Instance instantiate(HostFunction[] hostFunctions) {
        var globalInitializers = new Global[] {};
        if (this.module.getGlobalSection() != null) {
            globalInitializers = this.module.getGlobalSection().getGlobals();
        }
        var globals = new Value[globalInitializers.length];
        for (var i = 0; i < globalInitializers.length; i++) {
            var g = globalInitializers[i];
            if (g.getInit().length > 2)
                throw new RuntimeException(
                        "We don't a global initializer with multiple instructions");
            var instr = g.getInit()[0];
            switch (instr.getOpcode()) {
                case I32_CONST:
                    globals[i] = Value.i32(instr.getOperands()[0]);
                    break;
                case I64_CONST:
                    globals[i] = Value.i64(instr.getOperands()[0]);
                    break;
                case F32_CONST:
                    globals[i] = Value.f32(instr.getOperands()[0]);
                    break;
                case F64_CONST:
                    globals[i] = Value.f64(instr.getOperands()[0]);
                    break;
                case GLOBAL_GET:
                    // TODO this assumes that these are already initialized declared in order
                    // should we make this more resilient? Should initialization happen later?
                    globals[i] = globals[(int) instr.getOperands()[0]];
                    break;
                case REF_NULL:
                    globals[i] = Value.REF_NULL;
                    break;
                default:
                    throw new RuntimeException(
                            "We only support i32.const, i64.const, f32.const, f64.const,"
                                + " global.get, and ref.null opcodes on global initializers right"
                                + " now. We failed to initialize opcode: "
                                    + instr.getOpcode());
            }
        }

        var dataSegments = new DataSegment[0];
        if (module.getDataSection() != null) {
            dataSegments = module.getDataSection().getDataSegments();
        }

        Memory memory;
        if (module.getMemorySection() != null) {
            var memories = module.getMemorySection().getMemories();
            if (memories.length > 1) {
                throw new ChicoryException("We don't support multiple memories");
            }
            memory = new Memory(memories[0].getMemoryLimits(), dataSegments);
        } else {
            memory = new Memory(MemoryLimits.defaultLimits(), dataSegments);
        }

        var types = new FunctionType[0];
        // TODO i guess we should explode if this is the case, is this possible?
        if (module.getTypeSection() != null) {
            types = module.getTypeSection().getTypes();
        }

        var numFuncTypes = 0;
        var funcSection = module.getFunctionSection();
        if (funcSection != null) {
            numFuncTypes = funcSection.getTypeIndices().length;
            if (module.getImportSection() != null) {
                numFuncTypes += module.getImportSection().getImports().length;
            }
        }

        FunctionBody[] functions = new FunctionBody[0];
        var codeSection = module.getCodeSection();
        if (codeSection != null) {
            functions = module.getCodeSection().getFunctionBodies();
        }

        int importId = 0;
        Integer startFuncId = null;
        var functionTypes = new int[numFuncTypes];
        var imports = new Import[0];

        if (module.getImportSection() != null) {
            imports = new Import[module.getImportSection().getImports().length];
            for (var imprt : module.getImportSection().getImports()) {
                switch (imprt.getDesc().getType()) {
                    case FuncIdx:
                        {
                            var type = (int) imprt.getDesc().getIndex();
                            functionTypes[importId] = type;
                            // The global function id increases on this table
                            // function ids are assigned on imports first
                            imports[importId++] = imprt;
                            break;
                        }
                    case TableIdx:
                        throw new ChicoryException("Don't support table type globals yet");
                    case MemIdx:
                        throw new ChicoryException("Don't support mem type globals yet");
                    case GlobalIdx:
                        imports[importId++] = imprt;
                }
            }
        }

        var hostFuncs = mapHostFunctions(imports, hostFunctions);

        if (module.getStartSection() != null) {
            startFuncId = (int) module.getStartSection().getStartIndex();
        }

        if (module.getFunctionSection() != null) {
            if (startFuncId == null) startFuncId = importId;
            for (var ft : module.getFunctionSection().getTypeIndices()) {
                functionTypes[importId++] = ft;
            }
        }

        if (startFuncId != null) {
            // if we got a start func, let's add it to the exports
            var desc = new ExportDesc(startFuncId, ExportDescType.FuncIdx);
            var export = new Export("_start", desc);
            exports.put("_start", export);
        }

        Table table = null;
        if (module.getTableSection() != null) {
            if (module.getTableSection().getTables().length > 1) {
                throw new ChicoryException("We don't currently support more than 1 table");
            }
            table = module.getTableSection().getTables()[0];
            if (module.getElementSection() != null) {
                for (var el : module.getElementSection().getElements()) {
                    var idx = el.getTableIndex();
                    if (idx != 0)
                        throw new ChicoryException("We don't currently support more than 1 table");
                    for (var fi : el.getFuncIndices()) {
                        table.addFuncRef((int) fi);
                    }
                }
            }
        }

        var globalImportsOffset = 0;
        for (int i = 0; i < imports.length; i++) {
            if (imports[i].getDesc().getType() == ImportDescType.GlobalIdx) {
                globalImportsOffset++;
            }
        }

        return new Instance(
                this,
                globalInitializers,
                globals,
                globalImportsOffset,
                memory,
                functions,
                types,
                functionTypes,
                hostFuncs,
                table);
    }

    private HostFunction[] mapHostFunctions(Import[] imports, HostFunction[] hostFunctions) {
        var hostImports = new HostFunction[imports.length];
        for (var impIdx = 0; impIdx < imports.length; impIdx++) {
            var i = imports[impIdx];
            var name = i.getModuleName() + "." + i.getFieldName();
            var found = false;
            for (var f : hostFunctions) {
                if (i.getModuleName().equals(f.getModuleName())
                        && i.getFieldName().equals(f.getFieldName())) {
                    hostImports[impIdx] = f;
                    found = true;
                    break;
                }
            }
            if (!found)
                throw new ChicoryException("Could not find host function for import " + name);
        }
        return hostImports;
    }

    public Export getExport(String name) {
        var e = this.exports.get(name);
        if (e == null) throw new ChicoryException("Unknown export with name " + name);
        return e;
    }

    public NameSection getNameSection() {
        if (nameSec != null) return nameSec;
        nameSec = this.module.getNameSection();
        return nameSec;
    }
}
