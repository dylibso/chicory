package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.exceptions.InvalidException;
import com.dylibso.chicory.wasm.types.*;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;

public class Module {

    private static final System.Logger LOGGER = System.getLogger(Module.class.getName());
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
        return this.instantiate(new HostImports());
    }

    public Instance instantiate(HostImports hostImports) {
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

        Memory memory = null;
        if (module.getMemorySection() != null) {
            var memories = module.getMemorySection().getMemories();
            if (memories.length > 1) {
                throw new ChicoryException("We don't support multiple memories");
            }
            memory = new Memory(memories[0].getMemoryLimits(), dataSegments);
        } else {
            boolean importFound = false;
            if (module.getImportSection() != null) {
                for (int i = 0; i < module.getImportSection().getImports().length; i++) {
                    var imprt = module.getImportSection().getImports()[i];
                    if (imprt.getDesc().getType() != ImportDescType.MemIdx) {
                        continue;
                    }

                    if (importFound) {
                        throw new ChicoryException("We don't support multiple memories");
                    }
                    memory = new Memory(MemoryLimits.defaultLimits(), dataSegments, false);
                    importFound = true;
                }
            }

            if (!importFound) {
                memory = new Memory(MemoryLimits.defaultLimits(), dataSegments);
            }
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
        }
        if (module.getImportSection() != null) {
            numFuncTypes +=
                    Arrays.stream(module.getImportSection().getImports())
                            .filter(is -> is.getDesc().getType() == ImportDescType.FuncIdx)
                            .count();
        }

        FunctionBody[] functions = new FunctionBody[0];
        var codeSection = module.getCodeSection();
        if (codeSection != null) {
            functions = module.getCodeSection().getFunctionBodies();
        }

        int importId = 0;
        int importFuncType = 0;
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
                            functionTypes[importFuncType++] = type;
                            // The global function id increases on this table
                            // function ids are assigned on imports first
                            imports[importId++] = imprt;
                            break;
                        }
                    case TableIdx:
                    case MemIdx:
                    case GlobalIdx:
                        imports[importId++] = imprt;
                        break;
                }
            }
        }

        var mappedHostImports = mapHostImports(imports, hostImports);

        if (module.getStartSection() != null) {
            startFuncId = (int) module.getStartSection().getStartIndex();
        }

        if (module.getFunctionSection() != null) {
            if (startFuncId == null) startFuncId = importId;
            for (var ft : module.getFunctionSection().getTypeIndices()) {
                if (importId >= functionTypes.length) {
                    break;
                }
                functionTypes[importId++] = ft;
            }
        }

        if (startFuncId != null) {
            // if we got a start func, let's add it to the exports
            var desc = new ExportDesc(startFuncId, ExportDescType.FuncIdx);
            var export = new Export("_start", desc);
            exports.put("_start", export);
        }

        Table[] tables = new Table[0];
        if (module.getTableSection() != null) {
            var tableLength = module.getTableSection().getTables().length;
            tables = new Table[tableLength];
            for (int i = 0; i < tableLength; i++) {
                tables[i] = module.getTableSection().getTables()[i];
                if (module.getElementSection() != null) {
                    for (var el : module.getElementSection().getElements()) {
                        var idx = (int) el.getTableIndex();
                        for (var fi : el.getFuncIndices()) {
                            tables[idx].addFuncRef((int) fi);
                        }
                    }
                }
            }
        }

        var globalImportsOffset = 0;
        var functionImportsOffset = 0;
        for (int i = 0; i < imports.length; i++) {
            switch (imports[i].getDesc().getType()) {
                case GlobalIdx:
                    globalImportsOffset++;
                    break;
                case FuncIdx:
                    functionImportsOffset++;
                    break;
                default:
                    break;
            }
        }

        return new Instance(
                this,
                globalInitializers,
                globals,
                globalImportsOffset,
                functionImportsOffset,
                memory,
                functions,
                types,
                functionTypes,
                mappedHostImports,
                tables);
    }

    private HostImports mapHostImports(Import[] imports, HostImports hostImports) {
        int hostFuncNum = 0;
        int hostGlobalNum = 0;
        int hostMemNum = 0;
        int hostTableNum = 0;
        for (var imprt : imports) {
            switch (imprt.getDesc().getType()) {
                case FuncIdx:
                    hostFuncNum++;
                case GlobalIdx:
                    hostGlobalNum++;
                case MemIdx:
                    hostMemNum++;
                case TableIdx:
                    hostTableNum++;
            }
        }

        // TODO: this can probably be refactored ...
        var hostFuncs = new HostFunction[hostFuncNum];
        var hostFuncIdx = 0;
        var hostGlobals = new HostGlobal[hostGlobalNum];
        var hostGlobalIdx = 0;
        var hostMems = new HostMemory[hostMemNum];
        var hostMemIdx = 0;
        var hostTables = new HostTable[hostTableNum];
        var hostTableIdx = 0;
        for (var impIdx = 0; impIdx < imports.length; impIdx++) {
            var i = imports[impIdx];
            var name = i.getModuleName() + "." + i.getFieldName();
            var found = false;
            switch (i.getDesc().getType()) {
                case FuncIdx:
                    for (var f : hostImports.getFunctions()) {
                        if (i.getModuleName().equals(f.getModuleName())
                                && i.getFieldName().equals(f.getFieldName())) {
                            hostFuncs[hostFuncIdx] = f;
                            found = true;
                            break;
                        }
                    }
                    hostFuncIdx++;
                case GlobalIdx:
                    for (var g : hostImports.getGlobals()) {
                        if (i.getModuleName().equals(g.getModuleName())
                                && i.getFieldName().equals(g.getFieldName())) {
                            hostGlobals[hostGlobalIdx] = g;
                            found = true;
                            break;
                        }
                    }
                    hostGlobalIdx++;
                case MemIdx:
                    for (var m : hostImports.getMemories()) {
                        if (i.getModuleName().equals(m.getModuleName())
                                && i.getFieldName().equals(m.getFieldName())) {
                            hostMems[hostMemIdx] = m;
                            found = true;
                            break;
                        }
                    }
                    hostMemIdx++;
                case TableIdx:
                    for (var t : hostImports.getTables()) {
                        if (i.getModuleName().equals(t.getModuleName())
                                && i.getFieldName().equals(t.getFieldName())) {
                            hostTables[hostTableIdx] = t;
                            found = true;
                            break;
                        }
                    }
                    hostTableIdx++;
            }
            if (!found) {
                LOGGER.log(
                        System.Logger.Level.WARNING,
                        "Could not find host function for import number: "
                                + impIdx
                                + " named: "
                                + name);
            }
        }

        return new HostImports(hostFuncs, hostGlobals, hostMems, hostTables);
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
