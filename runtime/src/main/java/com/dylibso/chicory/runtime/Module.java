package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.exceptions.InvalidException;
import com.dylibso.chicory.wasm.types.*;
import java.util.HashMap;

public class Module {
    private com.dylibso.chicory.wasm.Module module;
    private HashMap<String, Export> exports;

    public static Module build(String wasmFile) {
        var parser = new Parser(wasmFile);
        return new Module(parser.parseModule());
    }

    public static Module build(String wasmFile, ModuleType type) {
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
                throw new RuntimeException("We don't support this global initializer");
            var instr = g.getInit()[0];
            // TODO we're assuming this is a const value, do we need to eval it?
            if (instr.getOpcode() != OpCode.I32_CONST && instr.getOpcode() != OpCode.I64_CONST) {
                throw new RuntimeException(
                        "We only support I32_CONST and I64_CONST on global initializers right now");
            }
            if (instr.getOpcode() == OpCode.I32_CONST) {
                globals[i] = Value.i32(instr.getOperands()[0]);
            } else {
                globals[i] = Value.i64(instr.getOperands()[0]);
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
            // TODO fix default
            memory = new Memory(new MemoryLimits(1, 10), dataSegments);
        }

        var types = new FunctionType[0];
        // TODO i guess we should explode if this is the case, is this possible?
        if (module.getTypeSection() != null) {
            types = module.getTypeSection().getTypes();
        }

        var numFuncTypes = 0;
        var funcSecton = module.getFunctionSection();
        if (funcSecton != null) {
            numFuncTypes = funcSecton.getTypeIndices().length;
            if (module.getImportSection() != null) {
                numFuncTypes += module.getImportSection().getImports().length;
            }
        }

        FunctionBody[] functions = new FunctionBody[0];
        var codeSection = module.getCodeSection();
        if (codeSection != null) {
            functions = module.getCodeSection().getFunctionBodies();
        }

        int funcId = 0;
        Integer startFuncId = null;
        var functionTypes = new int[numFuncTypes];
        var imports = new Import[0];

        if (module.getImportSection() != null) {
            imports = new Import[module.getImportSection().getImports().length];
            for (var imprt : module.getImportSection().getImports()) {
                if (imprt.getDesc().getType() != ImportDescType.FuncIdx)
                    throw new ChicoryException("We don't support non-function imports yet");
                var type = (int) imprt.getDesc().getIndex();
                functionTypes[funcId] = type;
                // The global function id increases on this table
                // function ids are assigned on imports first
                imports[funcId++] = imprt;
            }
        }

        var hostFuncs = mapHostFunctions(imports, hostFunctions);

        if (module.getStartSection() != null) {
            startFuncId = (int) module.getStartSection().getStartIndex();
        }

        if (module.getFunctionSection() != null) {
            if (startFuncId == null) startFuncId = funcId;
            for (var ft : module.getFunctionSection().getTypeIndices()) {
                functionTypes[funcId++] = ft;
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

        return new Instance(
                this,
                globalInitializers,
                globals,
                memory,
                functions,
                types,
                functionTypes,
                hostFuncs,
                table);
    }

    private HostFunction[] mapHostFunctions(Import[] imports, HostFunction[] hostFunctions) {
        var hostImports = new HostFunction[hostFunctions.length];
        for (var f : hostFunctions) {
            Integer foundId = null;
            for (var i : imports) {
                if (i.getModuleName().equals(f.getModuleName())
                        && i.getFieldName().equals(f.getFieldName())) {
                    foundId = (int) i.getDesc().getIndex();
                    break;
                }
            }
            if (foundId == null) throw new RuntimeException("Couldn't map import to function");
            hostImports[foundId] = f;
        }
        return hostImports;
    }

    public Export getExport(String name) {
        var e = this.exports.get(name);
        if (e == null) throw new ChicoryException("Unknown export with name " + name);
        return e;
    }
}
