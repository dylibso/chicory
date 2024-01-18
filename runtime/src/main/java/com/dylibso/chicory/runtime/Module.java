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
                    {
                        // TODO this assumes that these are already initialized declared in order
                        // should we make this more resilient? Should initialization happen later?
                        var globalImports = hostImports.getGlobals();
                        var idx = (int) instr.getOperands()[0];
                        globals[i] =
                                idx < globalImports.length
                                        ? globalImports[idx].getValue()
                                        : globals[idx];
                        break;
                    }
                case REF_NULL:
                    globals[i] = Value.EXTREF_NULL;
                    break;
                case REF_FUNC:
                    globals[i] = Value.funcRef(instr.getOperands()[0]);
                    break;
                default:
                    throw new RuntimeException(
                            "We only support i32.const, i64.const, f32.const, f64.const,"
                                    + " global.get, ref.func and ref.null opcodes on global"
                                    + " initializers right now. We failed to initialize opcode: "
                                    + instr.getOpcode());
            }
        }

        var dataSegments = new DataSegment[0];
        if (module.getDataSection() != null) {
            dataSegments = module.getDataSection().getDataSegments();
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
        Integer startFuncId = null;
        var functionTypes = new int[numFuncTypes];
        var imports = new Import[0];
        var funcIdx = 0;

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
                            funcIdx++;
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
            var desc = new ExportDesc(startFuncId, ExportDescType.FuncIdx);
            var export = new Export("_start", desc);
            exports.put("_start", export);
        }

        if (module.getFunctionSection() != null) {
            for (var ft : module.getFunctionSection().getTypeIndices()) {
                functionTypes[funcIdx++] = ft;
            }
        }

        Table[] tables = new Table[0];
        Element[] elements = new Element[0];
        if (module.getTableSection() != null) {
            var tableLength = module.getTableSection().getTables().length;
            tables = new Table[tableLength];
            for (int i = 0; i < tableLength; i++) {
                tables[i] = module.getTableSection().getTables()[i];
            }
            if (module.getElementSection() != null) {
                elements = module.getElementSection().getElements();
                for (var el : module.getElementSection().getElements()) {
                    switch (el.getElemType()) {
                        case Type:
                            {
                                var typeElem = (ElemType) el;
                                var expr = typeElem.getExpr();
                                var addr = getConstantValue(expr);
                                for (var fi : typeElem.getFuncIndices()) {
                                    tables[0].setRef(addr++, (int) fi);
                                }
                                break;
                            }
                        case Table:
                            {
                                var tableElem = (ElemTable) el;
                                var idx = (int) tableElem.getTableIndex();
                                var expr = tableElem.getExpr();
                                var addr = getConstantValue(expr);
                                for (var fi : tableElem.getFuncIndices()) {
                                    tables[idx].setRef(addr++, (int) fi);
                                }
                                break;
                            }
                        case Func:
                            {
                                var funcElem = (ElemFunc) el;
                                // TODO: what? only runtime?
                                break;
                            }
                        case Elem:
                            {
                                var elemElem = (ElemElem) el;
                                // TODO: what? only runtime?
                                break;
                            }
                        case Mem:
                            {
                                var memElem = (ElemMem) el;
                                // TODO: what? only runtime?
                                break;
                            }
                        default:
                            throw new ChicoryException(
                                    "Elment type: " + el.getElemType() + " not yet supported");
                    }
                }
            }
        }

        Memory memory = null;
        if (module.getMemorySection() != null) {
            assert (mappedHostImports.getMemories().length == 0);

            var memories = module.getMemorySection().getMemories();
            if (memories.length > 1) {
                throw new ChicoryException("Multiple memories are not supported");
            }
            memory = new Memory(memories[0].getMemoryLimits(), dataSegments);
        } else {
            if (mappedHostImports.getMemories().length > 0) {
                assert (mappedHostImports.getMemories().length == 1);
                if (mappedHostImports.getMemories()[0] == null
                        || mappedHostImports.getMemories()[0].getMemory() == null) {
                    throw new ChicoryException(
                            "Imported memory not defined, cannot run the program");
                }
                memory = mappedHostImports.getMemories()[0].getMemory();
            } else {
                // No memory defined
            }
        }

        var globalImportsOffset = 0;
        var functionImportsOffset = 0;
        var tablesImportsOffset = 0;
        for (int i = 0; i < imports.length; i++) {
            switch (imports[i].getDesc().getType()) {
                case GlobalIdx:
                    globalImportsOffset++;
                    break;
                case FuncIdx:
                    functionImportsOffset++;
                    break;
                case TableIdx:
                    tablesImportsOffset++;
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
                tablesImportsOffset,
                memory,
                functions,
                types,
                functionTypes,
                mappedHostImports,
                tables,
                elements);
    }

    public static int getConstantValue(Instruction[] expr) {
        assert (expr.length == 1);
        if (expr[0].getOpcode() != OpCode.I32_CONST) {
            throw new RuntimeException(
                    "Don't support data segment expressions other than"
                            + " i32.const yet, found: "
                            + expr[0].getOpcode());
        }
        return (int) expr[0].getOperands()[0];
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
                    break;
                case GlobalIdx:
                    hostGlobalNum++;
                    break;
                case MemIdx:
                    hostMemNum++;
                    break;
                case TableIdx:
                    hostTableNum++;
                    break;
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
        var hostIndex = new FromHost[hostFuncNum + hostGlobalNum + hostMemNum + hostTableNum];
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
                            hostIndex[impIdx] = f;
                            found = true;
                            break;
                        }
                    }
                    hostFuncIdx++;
                    break;
                case GlobalIdx:
                    for (var g : hostImports.getGlobals()) {
                        if (i.getModuleName().equals(g.getModuleName())
                                && i.getFieldName().equals(g.getFieldName())) {
                            hostGlobals[hostGlobalIdx] = g;
                            hostIndex[impIdx] = g;
                            found = true;
                            break;
                        }
                    }
                    hostGlobalIdx++;
                    break;
                case MemIdx:
                    for (var m : hostImports.getMemories()) {
                        if (i.getModuleName().equals(m.getModuleName())
                                && i.getFieldName().equals(m.getFieldName())) {
                            hostMems[hostMemIdx] = m;
                            hostIndex[impIdx] = m;
                            found = true;
                            break;
                        }
                    }
                    hostMemIdx++;
                    break;
                case TableIdx:
                    for (var t : hostImports.getTables()) {
                        if (i.getModuleName().equals(t.getModuleName())
                                && i.getFieldName().equals(t.getFieldName())) {
                            hostTables[hostTableIdx] = t;
                            hostIndex[impIdx] = t;
                            found = true;
                            break;
                        }
                    }
                    hostTableIdx++;
                    break;
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

        var result = new HostImports(hostFuncs, hostGlobals, hostMems, hostTables);
        result.setIndex(hostIndex);
        return result;
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
