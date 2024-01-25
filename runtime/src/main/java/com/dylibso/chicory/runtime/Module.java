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
        if (module.exportSection() != null) {
            for (var e : module.exportSection().exports()) {
                exports.put(e.name(), e);
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
        if (this.module.globalSection() != null) {
            globalInitializers = this.module.globalSection().globals();
        }
        var globals = new Value[globalInitializers.length];
        for (var i = 0; i < globalInitializers.length; i++) {
            var g = globalInitializers[i];
            if (g.initInstructions().length > 2)
                throw new RuntimeException(
                        "We don't a global initializer with multiple instructions");
            var instr = g.initInstructions()[0];
            switch (instr.opcode()) {
                case I32_CONST:
                    globals[i] = Value.i32(instr.operands()[0]);
                    break;
                case I64_CONST:
                    globals[i] = Value.i64(instr.operands()[0]);
                    break;
                case F32_CONST:
                    globals[i] = Value.f32(instr.operands()[0]);
                    break;
                case F64_CONST:
                    globals[i] = Value.f64(instr.operands()[0]);
                    break;
                case GLOBAL_GET:
                    {
                        // TODO this assumes that these are already initialized declared in order
                        // should we make this more resilient? Should initialization happen later?
                        var idx = (int) instr.operands()[0];
                        globals[i] =
                                idx < hostImports.globalCount()
                                        ? hostImports.global(idx).value()
                                        : globals[idx];
                        break;
                    }
                case REF_NULL:
                    globals[i] = Value.EXTREF_NULL;
                    break;
                case REF_FUNC:
                    globals[i] = Value.funcRef(instr.operands()[0]);
                    break;
                default:
                    throw new RuntimeException(
                            "We only support i32.const, i64.const, f32.const, f64.const,"
                                    + " global.get, ref.func and ref.null opcodes on global"
                                    + " initializers right now. We failed to initialize opcode: "
                                    + instr.opcode());
            }
        }

        var dataSegments = new DataSegment[0];
        if (module.dataSection() != null) {
            dataSegments = module.dataSection().dataSegments();
        }

        var types = new FunctionType[0];
        // TODO i guess we should explode if this is the case, is this possible?
        if (module.typeSection() != null) {
            types = module.typeSection().types();
        }

        var numFuncTypes = 0;
        var funcSection = module.functionSection();
        if (funcSection != null) {
            numFuncTypes = funcSection.typeIndices().length;
        }
        if (module.importSection() != null) {
            numFuncTypes +=
                    Arrays.stream(module.importSection().imports())
                            .filter(is -> is.desc().type() == ImportDescType.FuncIdx)
                            .count();
        }

        FunctionBody[] functions = new FunctionBody[0];
        var codeSection = module.codeSection();
        if (codeSection != null) {
            functions = module.codeSection().functionBodies();
        }

        int importId = 0;
        Integer startFuncId = null;
        var functionTypes = new int[numFuncTypes];
        var imports = new Import[0];
        var funcIdx = 0;

        if (module.importSection() != null) {
            imports = new Import[module.importSection().imports().length];
            for (var imprt : module.importSection().imports()) {
                switch (imprt.desc().type()) {
                    case FuncIdx:
                        {
                            var type = (int) imprt.desc().index();
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

        if (module.startSection() != null) {
            startFuncId = (int) module.startSection().startIndex();
            var desc = new ExportDesc(startFuncId, ExportDescType.FuncIdx);
            var export = new Export("_start", desc);
            exports.put("_start", export);
        }

        if (module.functionSection() != null) {
            for (var ft : module.functionSection().typeIndices()) {
                functionTypes[funcIdx++] = ft;
            }
        }

        Table[] tables = new Table[0];
        Element[] elements = new Element[0];
        if (module.tableSection() != null) {
            var tableLength = module.tableSection().tables().length;
            tables = new Table[tableLength];
            for (int i = 0; i < tableLength; i++) {
                tables[i] = module.tableSection().tables()[i];
            }
            if (module.elementSection() != null) {
                elements = module.elementSection().elements();
                for (var el : module.elementSection().elements()) {
                    switch (el.elemType()) {
                        case Type:
                            {
                                var typeElem = (ElemType) el;
                                var expr = typeElem.exprInstructions();
                                var addr = computeConstantValue(expr);
                                for (var fi : typeElem.funcIndices()) {
                                    tables[0].setRef(addr++, (int) fi);
                                }
                                break;
                            }
                        case Table:
                            {
                                var tableElem = (ElemTable) el;
                                var idx = (int) tableElem.tableIndex();
                                var expr = tableElem.exprInstructions();
                                var addr = computeConstantValue(expr);
                                for (var fi : tableElem.funcIndices()) {
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
                                    "Elment type: " + el.elemType() + " not yet supported");
                    }
                }
            }
        }

        Memory memory = null;
        if (module.memorySection() != null) {
            assert (mappedHostImports.memoryCount() == 0);

            var memories = module.memorySection().memories();
            if (memories.length > 1) {
                throw new ChicoryException("Multiple memories are not supported");
            }
            if (memories.length > 0) {
                memory = new Memory(memories[0].memoryLimits(), dataSegments);
            }
        } else {
            if (mappedHostImports.memoryCount() > 0) {
                assert (mappedHostImports.memoryCount() == 1);
                if (mappedHostImports.memory(0) == null
                        || mappedHostImports.memory(0).memory() == null) {
                    throw new ChicoryException(
                            "Imported memory not defined, cannot run the program");
                }
                memory = mappedHostImports.memory(0).memory();
            } else {
                // No memory defined
            }
        }

        var globalImportsOffset = 0;
        var functionImportsOffset = 0;
        var tablesImportsOffset = 0;
        for (int i = 0; i < imports.length; i++) {
            switch (imports[i].desc().type()) {
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

    public static int computeConstantValue(Instruction[] expr) {
        assert (expr.length == 1);
        if (expr[0].opcode() != OpCode.I32_CONST) {
            throw new RuntimeException(
                    "Don't support data segment expressions other than"
                            + " i32.const yet, found: "
                            + expr[0].opcode());
        }
        return (int) expr[0].operands()[0];
    }

    private HostImports mapHostImports(Import[] imports, HostImports hostImports) {
        int hostFuncNum = 0;
        int hostGlobalNum = 0;
        int hostMemNum = 0;
        int hostTableNum = 0;
        for (var imprt : imports) {
            switch (imprt.desc().type()) {
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
            var name = i.moduleName() + "." + i.fieldName();
            var found = false;
            switch (i.desc().type()) {
                case FuncIdx:
                    cnt = hostImports.functionCount();
                    for (int j = 0; j < cnt; j++) {
                        HostFunction f = hostImports.function(j);
                        if (i.moduleName().equals(f.moduleName())
                                && i.fieldName().equals(f.fieldName())) {
                            hostFuncs[hostFuncIdx] = f;
                            hostIndex[impIdx] = f;
                            found = true;
                            break;
                        }
                    }
                    hostFuncIdx++;
                    break;
                case GlobalIdx:
                    cnt = hostImports.globalCount();
                    for (int j = 0; j < cnt; j++) {
                        HostGlobal g = hostImports.global(j);
                        if (i.moduleName().equals(g.moduleName())
                                && i.fieldName().equals(g.fieldName())) {
                            hostGlobals[hostGlobalIdx] = g;
                            hostIndex[impIdx] = g;
                            found = true;
                            break;
                        }
                    }
                    hostGlobalIdx++;
                    break;
                case MemIdx:
                    cnt = hostImports.memoryCount();
                    for (int j = 0; j < cnt; j++) {
                        HostMemory m = hostImports.memory(j);
                        if (i.moduleName().equals(m.moduleName())
                                && i.fieldName().equals(m.fieldName())) {
                            hostMems[hostMemIdx] = m;
                            hostIndex[impIdx] = m;
                            found = true;
                            break;
                        }
                    }
                    hostMemIdx++;
                    break;
                case TableIdx:
                    cnt = hostImports.tableCount();
                    for (int j = 0; j < cnt; j++) {
                        HostTable t = hostImports.table(j);
                        if (i.moduleName().equals(t.moduleName())
                                && i.fieldName().equals(t.fieldName())) {
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

    public Export export(String name) {
        var e = this.exports.get(name);
        if (e == null) throw new ChicoryException("Unknown export with name " + name);
        return e;
    }

    public NameSection nameSection() {
        if (nameSec != null) return nameSec;
        nameSec = this.module.nameSection();
        return nameSec;
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
