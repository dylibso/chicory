package com.dylibso.chicory.runtime;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.exceptions.InvalidException;
import com.dylibso.chicory.wasm.exceptions.MalformedException;
import com.dylibso.chicory.wasm.exceptions.UnlinkableException;
import com.dylibso.chicory.wasm.types.Element;
import com.dylibso.chicory.wasm.types.Export;
import com.dylibso.chicory.wasm.types.ExportSection;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionImport;
import com.dylibso.chicory.wasm.types.GlobalImport;
import com.dylibso.chicory.wasm.types.Import;
import com.dylibso.chicory.wasm.types.NameCustomSection;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.TableImport;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class Module {
    public static final String START_FUNCTION_NAME = "_start";
    private final com.dylibso.chicory.wasm.Module module;

    private final Map<String, Export> exports;
    private final Logger logger;

    private final boolean initialize;
    private final boolean start;
    private final boolean typeValidation;
    private final ExecutionListener listener;
    private final HostImports hostImports;
    private final Function<Instance, Machine> machineFactory;

    private Module(
            com.dylibso.chicory.wasm.Module module,
            Logger logger,
            Function<Instance, Machine> machineFactory,
            HostImports hostImports,
            ExecutionListener listener,
            boolean initialize,
            boolean start,
            boolean typeValidation) {
        this.logger = logger;
        this.machineFactory = machineFactory;
        this.hostImports = hostImports;
        this.listener = listener;
        this.initialize = initialize;
        this.start = start;
        this.typeValidation = typeValidation;
        this.module = validateModule(module);
        this.exports = genExports(module.exportSection());
    }

    private static Map<String, Export> genExports(ExportSection export) {
        var exports = new HashMap<String, Export>();
        int cnt = export.exportCount();
        for (int i = 0; i < cnt; i++) {
            Export e = export.getExport(i);
            if (exports.containsKey(e.name())) {
                throw new InvalidException("duplicate export name " + e.name());
            }
            exports.put(e.name(), e);
        }
        return exports;
    }

    private static com.dylibso.chicory.wasm.Module validateModule(
            com.dylibso.chicory.wasm.Module module) {
        var functionSectionSize = module.functionSection().functionCount();
        var codeSectionSize = module.codeSection().functionBodyCount();
        var dataSectionSize = module.dataSection().dataSegmentCount();
        if (functionSectionSize != codeSectionSize) {
            throw new MalformedException("function and code section have inconsistent lengths");
        }
        if (module.dataCountSection() != null
                && dataSectionSize != module.dataCountSection().dataCount()) {
            throw new MalformedException("data count and data section have inconsistent lengths");
        }
        return module;
    }

    public Logger logger() {
        return logger;
    }

    public Instance instantiate() {
        var globalInitializers = module.globalSection().globals();

        var dataSegments = module.dataSection().dataSegments();

        // TODO i guess we should explode if this is the case, is this possible?
        var types = module.typeSection().types();

        int numFuncTypes =
                module.functionSection().functionCount()
                        + module.importSection().count(ExternalType.FUNCTION);

        FunctionBody[] functions = module.codeSection().functionBodies();

        int importId = 0;
        var functionTypes = new int[numFuncTypes];
        var funcIdx = 0;

        int importCount = module.importSection().importCount();
        var imports = new Import[importCount];
        for (int i = 0; i < importCount; i++) {
            Import imprt = module.importSection().getImport(i);
            switch (imprt.importType()) {
                case FUNCTION:
                    {
                        var type = ((FunctionImport) imprt).typeIndex();
                        if (type >= this.module.typeSection().typeCount()) {
                            throw new InvalidException("unknown type");
                        }
                        functionTypes[funcIdx] = type;
                        // The global function id increases on this table
                        // function ids are assigned on imports first
                        imports[importId++] = imprt;
                        funcIdx++;
                        break;
                    }
                default:
                    imports[importId++] = imprt;
                    break;
            }
        }

        var mappedHostImports = mapHostImports(imports, hostImports);

        if (module.startSection() != null) {
            var export =
                    new Export(
                            START_FUNCTION_NAME,
                            (int) module.startSection().startIndex(),
                            ExternalType.FUNCTION);
            exports.put(START_FUNCTION_NAME, export);
        }

        for (int i = 0; i < module.functionSection().functionCount(); i++) {
            functionTypes[funcIdx++] = module.functionSection().getFunctionType(i);
        }

        var tableLength = module.tableSection().tableCount();
        Table[] tables = new Table[tableLength];
        for (int i = 0; i < tableLength; i++) {
            tables[i] = module.tableSection().getTable(i);
        }

        Element[] elements = module.elementSection().elements();

        Memory memory = null;
        if (module.memorySection() != null) {
            var memories = module.memorySection();
            if (memories.memoryCount() + mappedHostImports.memoryCount() > 1) {
                throw new InvalidException("multiple memories are not supported");
            }
            if (memories.memoryCount() > 0) {
                memory = new Memory(memories.getMemory(0).memoryLimits());
            }
        } else {
            if (mappedHostImports.memoryCount() > 0) {
                if (mappedHostImports.memoryCount() != 1) {
                    throw new InvalidException("multiple memories");
                }
                if (mappedHostImports.memory(0) == null
                        || mappedHostImports.memory(0).memory() == null) {
                    throw new InvalidException(
                            "unknown memory, imported memory not defined, cannot run the program");
                }
                memory = mappedHostImports.memory(0).memory();
            } else {
                // No memory defined
            }
        }

        var globalImportsOffset = 0;
        var functionImportsOffset = 0;
        var tablesImportsOffset = 0;
        var memoryImportsOffset = 0;
        for (int i = 0; i < imports.length; i++) {
            switch (imports[i].importType()) {
                case GLOBAL:
                    globalImportsOffset++;
                    break;
                case FUNCTION:
                    functionImportsOffset++;
                    break;
                case TABLE:
                    tablesImportsOffset++;
                    break;
                case MEMORY:
                    memoryImportsOffset++;
                    break;
                default:
                    break;
            }
        }

        for (var e : exports.values()) {
            switch (e.exportType()) {
                case FUNCTION:
                    {
                        if (e.index()
                                >= module.functionSection().functionCount()
                                        + functionImportsOffset) {
                            throw new InvalidException("unknown function " + e.index());
                        }
                        break;
                    }
                case GLOBAL:
                    {
                        if (e.index()
                                >= module.globalSection().globalCount() + globalImportsOffset) {
                            throw new InvalidException("unknown global " + e.index());
                        }
                        break;
                    }
                case TABLE:
                    {
                        if (e.index() >= module.tableSection().tableCount() + tablesImportsOffset) {
                            throw new InvalidException("unknown table " + e.index());
                        }
                        break;
                    }
                case MEMORY:
                    {
                        var memoryCount =
                                (module.memorySection() == null)
                                        ? 0
                                        : module.memorySection().memoryCount();
                        if (e.index() >= memoryCount + memoryImportsOffset) {
                            throw new InvalidException("unknown memory " + e);
                        }
                        break;
                    }
            }
        }

        return new Instance(
                this,
                globalInitializers,
                globalImportsOffset,
                functionImportsOffset,
                tablesImportsOffset,
                memory,
                dataSegments,
                functions,
                types,
                functionTypes,
                mappedHostImports,
                tables,
                elements,
                machineFactory,
                initialize,
                start,
                typeValidation,
                listener);
    }

    private void validateHostFunctionSignature(FunctionImport imprt, HostFunction f) {
        var expectedType = module.typeSection().getType(imprt.typeIndex());
        if (expectedType.params().size() != f.paramTypes().size()
                || expectedType.returns().size() != f.returnTypes().size()) {
            throw new UnlinkableException("incompatible import type");
        }
        for (int i = 0; i < expectedType.params().size(); i++) {
            var expected = expectedType.params().get(i);
            var got = f.paramTypes().get(i);
            if (expected != got) {
                throw new UnlinkableException("incompatible import type");
            }
        }
        for (int i = 0; i < expectedType.returns().size(); i++) {
            var expected = expectedType.returns().get(i);
            var got = f.returnTypes().get(i);
            if (expected != got) {
                throw new UnlinkableException("incompatible import type");
            }
        }
    }

    private void validateHostGlobalType(GlobalImport i, HostGlobal g) {
        if (i.type() != g.instance().getValue().type()
                || i.mutabilityType() != g.mutabilityType()) {
            throw new UnlinkableException("incompatible import type");
        }
    }

    private void validateHostTableType(TableImport i, HostTable t) {
        // TODO: verify table limits and fix everything accordingly
        if (i.entryType() != t.table().elementType()) {
            throw new UnlinkableException("incompatible import type");
        }
    }

    private void validateNegativeImportType(String moduleName, String name, FromHost[] fromHost) {
        for (var fh : fromHost) {
            if (fh.moduleName().equals(moduleName) && fh.fieldName().equals(name)) {
                throw new UnlinkableException("incompatible import type");
            }
        }
    }

    private void validateNegativeImportType(
            String moduleName, String name, ExternalType typ, HostImports hostImports) {
        switch (typ) {
            case FUNCTION:
                validateNegativeImportType(moduleName, name, hostImports.globals());
                validateNegativeImportType(moduleName, name, hostImports.memories());
                validateNegativeImportType(moduleName, name, hostImports.tables());
                break;
            case GLOBAL:
                validateNegativeImportType(moduleName, name, hostImports.functions());
                validateNegativeImportType(moduleName, name, hostImports.memories());
                validateNegativeImportType(moduleName, name, hostImports.tables());
                break;
            case MEMORY:
                validateNegativeImportType(moduleName, name, hostImports.functions());
                validateNegativeImportType(moduleName, name, hostImports.globals());
                validateNegativeImportType(moduleName, name, hostImports.tables());
                break;
            case TABLE:
                validateNegativeImportType(moduleName, name, hostImports.functions());
                validateNegativeImportType(moduleName, name, hostImports.globals());
                validateNegativeImportType(moduleName, name, hostImports.memories());
                break;
        }
    }

    private HostImports mapHostImports(Import[] imports, HostImports hostImports) {
        int hostFuncNum = 0;
        int hostGlobalNum = 0;
        int hostMemNum = 0;
        int hostTableNum = 0;
        for (var imprt : imports) {
            switch (imprt.importType()) {
                case FUNCTION:
                    hostFuncNum++;
                    break;
                case GLOBAL:
                    hostGlobalNum++;
                    break;
                case MEMORY:
                    hostMemNum++;
                    break;
                case TABLE:
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
        int cnt;
        for (var impIdx = 0; impIdx < imports.length; impIdx++) {
            var i = imports[impIdx];
            var name = i.moduleName() + "." + i.name();
            var found = false;
            validateNegativeImportType(i.moduleName(), i.name(), i.importType(), hostImports);
            switch (i.importType()) {
                case FUNCTION:
                    cnt = hostImports.functionCount();
                    for (int j = 0; j < cnt; j++) {
                        HostFunction f = hostImports.function(j);
                        if (i.moduleName().equals(f.moduleName())
                                && i.name().equals(f.fieldName())) {
                            validateHostFunctionSignature((FunctionImport) i, f);
                            hostFuncs[hostFuncIdx] = f;
                            found = true;
                            break;
                        }
                    }
                    hostFuncIdx++;
                    break;
                case GLOBAL:
                    cnt = hostImports.globalCount();
                    for (int j = 0; j < cnt; j++) {
                        HostGlobal g = hostImports.global(j);
                        if (i.moduleName().equals(g.moduleName())
                                && i.name().equals(g.fieldName())) {
                            validateHostGlobalType((GlobalImport) i, g);
                            hostGlobals[hostGlobalIdx] = g;
                            found = true;
                            break;
                        }
                    }
                    hostGlobalIdx++;
                    break;
                case MEMORY:
                    cnt = hostImports.memoryCount();
                    for (int j = 0; j < cnt; j++) {
                        HostMemory m = hostImports.memory(j);
                        if (i.moduleName().equals(m.moduleName())
                                && i.name().equals(m.fieldName())) {
                            hostMems[hostMemIdx] = m;
                            found = true;
                            break;
                        }
                    }
                    hostMemIdx++;
                    break;
                case TABLE:
                    cnt = hostImports.tableCount();
                    for (int j = 0; j < cnt; j++) {
                        HostTable t = hostImports.table(j);
                        if (i.moduleName().equals(t.moduleName())
                                && i.name().equals(t.fieldName())) {
                            validateHostTableType((TableImport) i, t);
                            hostTables[hostTableIdx] = t;
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
        return result;
    }

    public Export export(String name) {
        return this.exports.get(name);
    }

    public Map<String, Export> exports() {
        return this.exports;
    }

    public NameCustomSection nameSection() {
        return this.module.nameSection();
    }

    public com.dylibso.chicory.wasm.Module wasmModule() {
        return this.module;
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
     * Creates a {@link Builder} for the specified {@link com.dylibso.chicory.wasm.Module}
     *
     * @param wasmModule the already parsed Wasm module
     * @return a {@link Builder} for reading the module definition from the specified input stream
     */
    public static Builder builder(com.dylibso.chicory.wasm.Module wasmModule) {
        return new Builder(wasmModule);
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
        private final com.dylibso.chicory.wasm.Module parsed;
        private final Supplier<InputStream> inputStreamSupplier;
        private Logger logger;
        private ModuleType moduleType;

        private boolean initialize = true;
        private boolean start = true;
        private boolean typeValidation = true;
        private ExecutionListener listener = null;
        private HostImports hostImports = null;
        private Function<Instance, Machine> machineFactory = null;

        private Builder(Supplier<InputStream> inputStreamSupplier) {
            this.inputStreamSupplier = Objects.requireNonNull(inputStreamSupplier);
            this.parsed = null;
            this.moduleType = ModuleType.BINARY;
        }

        private Builder(com.dylibso.chicory.wasm.Module parsed) {
            this.parsed = Objects.requireNonNull(parsed);
            this.inputStreamSupplier = null;
            this.moduleType = ModuleType.BINARY;
        }

        public Builder withLogger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder withType(ModuleType type) {
            this.moduleType = type;
            return this;
        }

        public Builder withInitialize(boolean init) {
            this.initialize = init;
            return this;
        }

        public Builder withStart(boolean s) {
            this.start = s;
            return this;
        }

        public Builder withTypeValidation(boolean v) {
            this.typeValidation = v;
            return this;
        }

        /*
         * This method is experimental and might be dropped without notice in future releases.
         */
        public Builder withUnsafeExecutionListener(ExecutionListener listener) {
            this.listener = listener;
            return this;
        }

        public Builder withHostImports(HostImports hostImports) {
            this.hostImports = hostImports;
            return this;
        }

        public Builder withMachineFactory(Function<Instance, Machine> machineFactory) {
            this.machineFactory = machineFactory;
            return this;
        }

        public Module build() {
            final Logger logger = this.logger != null ? this.logger : new SystemLogger();
            final HostImports hostImports =
                    this.hostImports != null ? this.hostImports : new HostImports();
            final Function<Instance, Machine> machineFactory =
                    this.machineFactory != null ? this.machineFactory : InterpreterMachine::new;
            final Parser parser = new Parser(logger);

            com.dylibso.chicory.wasm.Module parsed = this.parsed;
            if (parsed == null) {
                try (final InputStream is = inputStreamSupplier.get()) {
                    parsed = parser.parseModule(is);
                } catch (IOException e) {
                    throw new WASMRuntimeException(e);
                }
            }

            switch (this.moduleType) {
                case BINARY:
                    return new Module(
                            parsed,
                            logger,
                            machineFactory,
                            hostImports,
                            this.listener,
                            this.initialize,
                            this.start,
                            this.typeValidation);
                default:
                    throw new InvalidException(
                            "Text format parsing is not implemented, but you can use wat2wasm"
                                    + " through Chicory.");
            }
        }
    }
}
