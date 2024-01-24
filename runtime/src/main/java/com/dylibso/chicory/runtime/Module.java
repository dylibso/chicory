package com.dylibso.chicory.runtime;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.exceptions.InvalidException;
import com.dylibso.chicory.wasm.types.DataSegment;
import com.dylibso.chicory.wasm.types.ElemElem;
import com.dylibso.chicory.wasm.types.ElemFunc;
import com.dylibso.chicory.wasm.types.ElemMem;
import com.dylibso.chicory.wasm.types.ElemTable;
import com.dylibso.chicory.wasm.types.ElemType;
import com.dylibso.chicory.wasm.types.Element;
import com.dylibso.chicory.wasm.types.Export;
import com.dylibso.chicory.wasm.types.ExportDesc;
import com.dylibso.chicory.wasm.types.ExportDescType;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Global;
import com.dylibso.chicory.wasm.types.Import;
import com.dylibso.chicory.wasm.types.ImportDescType;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.NameSection;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.Value;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Supplier;

public class Module {
    private final com.dylibso.chicory.wasm.Module module;
    private NameSection nameSec;

    private final HashMap<String, Export> exports;
    private final Logger logger;

    protected Module(com.dylibso.chicory.wasm.Module module, Logger logger) {
        this.logger = logger;
        this.module = module;
        this.exports = new HashMap<>();
        if (module.getExportSection() != null) {
            for (var e : module.getExportSection().getExports()) {
                exports.put(e.getName(), e);
            }
        }
    }

    public Logger logger() {
        return logger;
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
                        var idx = (int) instr.getOperands()[0];
                        globals[i] =
                                idx < hostImports.getGlobalCount()
                                        ? hostImports.getGlobal(idx).getValue()
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
            assert (mappedHostImports.getMemoryCount() == 0);

            var memories = module.getMemorySection().getMemories();
            if (memories.length > 1) {
                throw new ChicoryException("Multiple memories are not supported");
            }
            if (memories.length > 0) {
                memory = new Memory(memories[0].getMemoryLimits(), dataSegments);
            }
        } else {
            if (mappedHostImports.getMemoryCount() > 0) {
                assert (mappedHostImports.getMemoryCount() == 1);
                if (mappedHostImports.getMemory(0) == null
                        || mappedHostImports.getMemory(0).getMemory() == null) {
                    throw new ChicoryException(
                            "Imported memory not defined, cannot run the program");
                }
                memory = mappedHostImports.getMemory(0).getMemory();
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
        int cnt;
        for (var impIdx = 0; impIdx < imports.length; impIdx++) {
            var i = imports[impIdx];
            var name = i.getModuleName() + "." + i.getFieldName();
            var found = false;
            switch (i.getDesc().getType()) {
                case FuncIdx:
                    cnt = hostImports.getFunctionCount();
                    for (int j = 0; j < cnt; j++) {
                        HostFunction f = hostImports.getFunction(j);
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
                    cnt = hostImports.getGlobalCount();
                    for (int j = 0; j < cnt; j++) {
                        HostGlobal g = hostImports.getGlobal(j);
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
                    cnt = hostImports.getMemoryCount();
                    for (int j = 0; j < cnt; j++) {
                        HostMemory m = hostImports.getMemory(j);
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
                    cnt = hostImports.getTableCount();
                    for (int j = 0; j < cnt; j++) {
                        HostTable t = hostImports.getTable(j);
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
                this.logger.warnf(
                        "Could not find host function for import number: %d named %s",
                        impIdx, name);
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

    /**
     * Use {@link #builder(File)}
     *
     * @deprecated
     */
    @Deprecated
    public static Module build(File wasmFile) {
        return builder(wasmFile).build();
    }

    /**
     * Use {@link #builder(InputStream)}
     *
     * @deprecated
     */
    @Deprecated
    public static Module build(InputStream is) {
        return builder(is).build();
    }

    /**
     * Use {@link #builder(ByteBuffer)}
     *
     * @deprecated
     */
    @Deprecated
    public static Module build(ByteBuffer buffer) {
        return builder(buffer).build();
    }

    /**
     * Use {@link #builder(File)}
     *
     * @deprecated
     */
    @Deprecated
    public static Module build(File wasmFile, ModuleType type) {
        return builder(wasmFile).withType(type).build();
    }

    /**
     * Creates a {@link Builder} for the specified {@link InputStream}
     *
     * @param input the input stream
     * @return a {@link Builder} for reading the module definition from the specified input stream
     */
    public static Builder builder(InputStream input) {
        return new Builder(() -> input);
    }

    /**
     * Creates a {@link Builder} for the specified {@link ByteBuffer}
     *
     * @param buffer the buffer
     * @return a {@link Builder} for reading the module definition from the specified buffer
     */
    public static Builder builder(ByteBuffer buffer) {
        return builder(buffer.array());
    }

    /**
     * Creates a {@link Builder} for the specified byte array
     *
     * @param buffer the buffer
     * @return a {@link Builder} for reading the module definition from the specified buffer
     */
    public static Builder builder(byte[] buffer) {
        return new Builder(() -> new ByteArrayInputStream(buffer));
    }

    /**
     * Creates a {@link Builder} for the specified {@link File} resource
     *
     * @param file the path of the resource
     * @return a {@link Builder} for reading the module definition from the specified file
     */
    public static Builder builder(File file) {
        return new Builder(
                () -> {
                    try {
                        return new FileInputStream(file);
                    } catch (FileNotFoundException e) {
                        throw new IllegalArgumentException(
                                "File not found at path: " + file.getPath(), e);
                    }
                });
    }

    /**
     * Creates a {@link Builder} for the specified classpath resource
     *
     * @param classpathResource the name of the resource
     * @return a {@link Builder}  for reading the module definition from the specified resource
     */
    public static Builder builder(String classpathResource) {
        return new Builder(
                () -> {
                    InputStream is =
                            Thread.currentThread()
                                    .getContextClassLoader()
                                    .getResourceAsStream(classpathResource);
                    if (is == null) {
                        throw new IllegalArgumentException(
                                "Resource not found at classpath: " + classpathResource);
                    }
                    return is;
                });
    }

    /**
     * Creates a {@link Builder} for the specified {@link Path} resource
     *
     * @param path the path of the resource
     * @return a {@link Builder} for reading the module definition from the specified path
     */
    public static Builder builder(Path path) {
        return new Builder(
                () -> {
                    try {
                        return Files.newInputStream(path);
                    } catch (IOException e) {
                        throw new IllegalArgumentException("Error opening file: " + path, e);
                    }
                });
    }

    public static class Builder {
        private final Supplier<InputStream> inputStreamSupplier;
        private Logger logger;
        private ModuleType moduleType;

        private Builder(Supplier<InputStream> inputStreamSupplier) {
            this.inputStreamSupplier = Objects.requireNonNull(inputStreamSupplier);
            this.moduleType = ModuleType.TEXT;
        }

        public Builder withLogger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder withType(ModuleType type) {
            this.moduleType = type;
            return this;
        }

        public Module build() {

            final Logger logger = this.logger != null ? this.logger : new SystemLogger();
            final Parser parser = new Parser(logger);

            try (final InputStream is = inputStreamSupplier.get()) {

                switch (this.moduleType) {
                    case TEXT:
                        return new Module(parser.parseModule(is), logger);
                    default:
                        // TODO: implement me
                        throw new InvalidException("type mismatch");
                }
            } catch (IOException e) {
                throw new WASMRuntimeException(e);
            }
        }
    }
}
