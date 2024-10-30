package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.runtime.ConstantEvaluators.computeConstantInstance;
import static com.dylibso.chicory.runtime.ConstantEvaluators.computeConstantValue;
import static com.dylibso.chicory.wasm.types.ExternalType.FUNCTION;
import static com.dylibso.chicory.wasm.types.ExternalType.GLOBAL;
import static com.dylibso.chicory.wasm.types.ExternalType.MEMORY;
import static com.dylibso.chicory.wasm.types.ExternalType.TABLE;
import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;
import static java.util.Objects.requireNonNullElseGet;

import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.InvalidException;
import com.dylibso.chicory.wasm.Module;
import com.dylibso.chicory.wasm.UninstantiableException;
import com.dylibso.chicory.wasm.UnlinkableException;
import com.dylibso.chicory.wasm.types.ActiveDataSegment;
import com.dylibso.chicory.wasm.types.ActiveElement;
import com.dylibso.chicory.wasm.types.DataSegment;
import com.dylibso.chicory.wasm.types.Element;
import com.dylibso.chicory.wasm.types.Export;
import com.dylibso.chicory.wasm.types.ExportSection;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionImport;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Global;
import com.dylibso.chicory.wasm.types.GlobalImport;
import com.dylibso.chicory.wasm.types.Import;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.MemoryImport;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import com.dylibso.chicory.wasm.types.MemorySection;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.TableImport;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class Instance {
    public static final String START_FUNCTION_NAME = "_start";

    private final Module module;
    private final Machine machine;
    private final FunctionBody[] functions;
    private final Memory memory;
    private final DataSegment[] dataSegments;
    private final Global[] globalInitializers;
    private final GlobalInstance[] globals;
    private final FunctionType[] types;
    private final int[] functionTypes;
    private final ImportValues imports;
    private final TableInstance[] tables;
    private final Element[] elements;
    private final Map<String, Export> exports;
    private final ExecutionListener listener;

    Instance(
            Module module,
            Global[] globalInitializers,
            Memory memory,
            DataSegment[] dataSegments,
            FunctionBody[] functions,
            FunctionType[] types,
            int[] functionTypes,
            ImportValues imports,
            Table[] tables,
            Element[] elements,
            Map<String, Export> exports,
            Function<Instance, Machine> machineFactory,
            boolean initialize,
            boolean start,
            ExecutionListener listener) {
        this.module = module;
        this.globalInitializers = globalInitializers.clone();
        this.globals = new GlobalInstance[globalInitializers.length];
        this.memory = memory;
        this.dataSegments = dataSegments;
        this.functions = functions.clone();
        this.types = types.clone();
        this.functionTypes = functionTypes.clone();
        this.imports = imports;
        this.machine = machineFactory.apply(this);
        this.tables = new TableInstance[tables.length];
        for (int i = 0; i < tables.length; i++) {
            this.tables[i] = new TableInstance(tables[i]);
        }
        this.elements = elements.clone();
        this.exports = exports;
        this.listener = listener;

        if (initialize) {
            initialize(start);
        }
    }

    public Instance initialize(boolean start) {
        for (var el : elements) {
            if (el instanceof ActiveElement) {
                var ae = (ActiveElement) el;
                var table = table(ae.tableIndex());
                int offset = (int) computeConstantValue(this, ae.offset());

                List<List<Instruction>> initializers = ae.initializers();
                if (offset > table.limits().min()
                        || (offset + initializers.size() - 1) >= table.size()) {
                    throw new UninstantiableException("out of bounds table access");
                }
                for (int i = 0; i < initializers.size(); i++) {
                    final List<Instruction> init = initializers.get(i);
                    int index = offset + i;
                    var value = computeConstantValue(this, init);
                    var inst = computeConstantInstance(this, init);

                    if (ae.type() == ValueType.FuncRef) {
                        if (value != REF_NULL_VALUE
                                && (value < 0
                                        || value >= (functions.length + imports.functionCount()))) {
                            throw new InvalidException("unknown function " + value);
                        }
                        table.setRef(index, (int) value, inst);
                    } else {
                        assert ae.type() == ValueType.ExternRef;
                        table.setRef(index, (int) value, inst);
                    }
                }
            }
        }

        for (var i = 0; i < globalInitializers.length; i++) {
            var g = globalInitializers[i];
            var value = computeConstantValue(this, g.initInstructions());
            globals[i] = new GlobalInstance(new Value(g.valueType(), value), g.mutabilityType());
            globals[i].setInstance(this);
        }

        if (memory != null && imports.memories().length == 0) {
            memory.zero();
            memory.initialize(this, dataSegments);
        } else if (imports.memories().length > 0) {
            imports.memories()[0].memory().initialize(this, dataSegments);
        } else if (Arrays.stream(dataSegments).anyMatch(ds -> ds instanceof ActiveDataSegment)) {
            for (var ds : dataSegments) {
                if (ds instanceof ActiveDataSegment) {
                    var memory = (ActiveDataSegment) ds;
                    throw new InvalidException("unknown memory " + memory.index());
                }
            }
            throw new InvalidException("unknown memory");
        }

        Export startFunction = this.exports.get(START_FUNCTION_NAME);
        if (startFunction != null && start) {
            try {
                export(START_FUNCTION_NAME).apply();
            } catch (TrapException e) {
                throw new UninstantiableException(e.getMessage(), e);
            }
        }

        return this;
    }

    public FunctionType exportType(String name) {
        return type(functionType(exports.get(name).index()));
    }

    public ExportFunction export(String name) {
        var export = this.exports.get(name);
        if (export == null) {
            throw new ChicoryException("Unknown export with name " + name);
        }

        switch (export.exportType()) {
            case FUNCTION:
                {
                    return args -> machine.call(export.index(), args);
                }
            case GLOBAL:
                {
                    return args -> {
                        assert (args.length == 0);
                        var v = global(export.index()).getValue();
                        return new long[] {v};
                    };
                }
            default:
                {
                    throw new ChicoryException("not implemented");
                }
        }
    }

    public FunctionBody function(long idx) {
        if (idx < 0 || idx >= (functions.length + imports.functionCount())) {
            throw new InvalidException("unknown function " + idx);
        } else if (idx < imports.functionCount()) {
            return null;
        }
        return functions[(int) idx - imports.functionCount()];
    }

    public int functionCount() {
        return imports.functionCount() + functions.length;
    }

    public Memory memory() {
        return memory;
    }

    public GlobalInstance global(int idx) {
        if (idx < imports.globalCount()) {
            return imports.global(idx).instance();
        }
        var i = idx - imports.globalCount();
        if (i < 0 || i >= globals.length) {
            throw new InvalidException("unknown global " + idx);
        }
        return globals[idx - imports.globalCount()];
    }

    public FunctionType type(int idx) {
        if (idx >= types.length) {
            throw new InvalidException("unknown type " + idx);
        }
        return types[idx];
    }

    public int functionType(int idx) {
        if (idx >= functionTypes.length) {
            throw new InvalidException("unknown function " + idx);
        }
        return functionTypes[idx];
    }

    public ImportValues imports() {
        return imports;
    }

    public Module module() {
        return module;
    }

    public TableInstance table(int idx) {
        if (idx < 0 || idx >= (tables.length + imports.tableCount())) {
            throw new InvalidException("unknown table " + idx);
        }
        if (idx < imports.tableCount()) {
            return imports.table(idx).table();
        }
        return tables[idx - imports.tableCount()];
    }

    public Element element(int idx) {
        if (idx < 0 || idx >= elements.length) {
            throw new InvalidException("unknown elem segment " + idx);
        }
        return elements[idx];
    }

    public int elementCount() {
        return elements.length;
    }

    public void setElement(int idx, Element val) {
        elements[idx] = val;
    }

    public Machine getMachine() {
        return machine;
    }

    void onExecution(Instruction instruction, MStack stack) {
        if (listener != null) {
            listener.onExecution(instruction, stack);
        }
    }

    public static Builder builder(Module module) {
        return new Builder(module);
    }

    public static final class Builder {
        private final Module module;

        private boolean initialize = true;
        private boolean start = true;
        private ExecutionListener listener;
        private ImportValues importValues;
        private Function<Instance, Machine> machineFactory;

        private Builder(Module module) {
            this.module = Objects.requireNonNull(module);
        }

        public Builder withInitialize(boolean init) {
            this.initialize = init;
            return this;
        }

        public Builder withStart(boolean s) {
            this.start = s;
            return this;
        }

        /*
         * This method is experimental and might be dropped without notice in future releases.
         */
        public Builder withUnsafeExecutionListener(ExecutionListener listener) {
            this.listener = listener;
            return this;
        }

        public Builder withImportValues(ImportValues importValues) {
            this.importValues = importValues;
            return this;
        }

        public Builder withMachineFactory(Function<Instance, Machine> machineFactory) {
            this.machineFactory = machineFactory;
            return this;
        }

        private void validateExternalFunctionSignature(FunctionImport imprt, ImportFunction f) {
            var expectedType = module.typeSection().getType(imprt.typeIndex());
            if (expectedType.params().size() != f.paramTypes().size()
                    || expectedType.returns().size() != f.returnTypes().size()) {
                throw new UnlinkableException(
                        "incompatible import type for host function "
                                + f.module()
                                + "."
                                + f.name());
            }
            for (int i = 0; i < expectedType.params().size(); i++) {
                var expected = expectedType.params().get(i);
                var got = f.paramTypes().get(i);
                if (expected != got) {
                    throw new UnlinkableException(
                            "incompatible import type for host function "
                                    + f.module()
                                    + "."
                                    + f.name());
                }
            }
            for (int i = 0; i < expectedType.returns().size(); i++) {
                var expected = expectedType.returns().get(i);
                var got = f.returnTypes().get(i);
                if (expected != got) {
                    throw new UnlinkableException(
                            "incompatible import type for host function "
                                    + f.module()
                                    + "."
                                    + f.name());
                }
            }
        }

        private void validateHostGlobalType(GlobalImport i, ImportGlobal g) {
            if (i.type() != g.instance().getType()
                    || i.mutabilityType() != g.instance().getMutabilityType()) {
                throw new UnlinkableException("incompatible import type");
            }
        }

        private void validateHostTableType(TableImport i, ImportTable t) {
            var minExpected = t.table().limits().min();
            var maxExpected = t.table().limits().max();
            var minCurrent = i.limits().min();
            var maxCurrent = i.limits().max();
            if (i.entryType() != t.table().elementType()) {
                throw new UnlinkableException("incompatible import type");
            } else if (minExpected < minCurrent || maxExpected > maxCurrent) {
                throw new UnlinkableException(
                        "incompatible import type, non-compatible limits, expected: "
                                + i.limits()
                                + ", current: "
                                + t.table().limits()
                                + " on table: "
                                + t.module()
                                + "."
                                + t.name());
            }
        }

        private void validateHostMemoryType(MemoryImport i, ImportMemory m) {
            // Notice we do not compare to m.memory().initialPages()
            // because m might have grown in the meantime.
            // Instead, we use the current number of pages.
            var hostMemCurrentPages = m.memory().pages();
            var hostMemMaxPages = m.memory().maximumPages();
            var importInitialPages = i.limits().initialPages();
            var importMaxPages =
                    (i.limits().maximumPages() == MemoryLimits.MAX_PAGES)
                            ? Memory.RUNTIME_MAX_PAGES
                            : i.limits().maximumPages();

            // HostMem bounds [x,y] must be within the import bounds [a, b]; i.e., a <= x, y >= b.
            // In other words, the bounds are not valid when:
            // - HostMem current number of pages cannot be less than the import lower bound.
            // - HostMem upper bound cannot be larger than the given upper bound.
            if (hostMemCurrentPages < importInitialPages || hostMemMaxPages > importMaxPages) {
                throw new UnlinkableException(
                        "incompatible import type, non-compatible limits, import: "
                                + i.limits()
                                + ", host initial pages: "
                                + m.memory().initialPages()
                                + ", host max pages: "
                                + m.memory().maximumPages()
                                + " on memory: "
                                + m.module()
                                + "."
                                + m.name());
            }
        }

        private void validateNegativeImportType(
                String moduleName, String name, ImportValue[] external) {
            for (var fh : external) {
                if (fh.module().equals(moduleName) && fh.name().equals(name)) {
                    throw new UnlinkableException("incompatible import type");
                }
            }
        }

        private void validateNegativeImportType(
                String moduleName, String name, ExternalType typ, ImportValues importValues) {
            switch (typ) {
                case FUNCTION:
                    validateNegativeImportType(moduleName, name, importValues.globals());
                    validateNegativeImportType(moduleName, name, importValues.memories());
                    validateNegativeImportType(moduleName, name, importValues.tables());
                    break;
                case GLOBAL:
                    validateNegativeImportType(moduleName, name, importValues.functions());
                    validateNegativeImportType(moduleName, name, importValues.memories());
                    validateNegativeImportType(moduleName, name, importValues.tables());
                    break;
                case MEMORY:
                    validateNegativeImportType(moduleName, name, importValues.functions());
                    validateNegativeImportType(moduleName, name, importValues.globals());
                    validateNegativeImportType(moduleName, name, importValues.tables());
                    break;
                case TABLE:
                    validateNegativeImportType(moduleName, name, importValues.functions());
                    validateNegativeImportType(moduleName, name, importValues.globals());
                    validateNegativeImportType(moduleName, name, importValues.memories());
                    break;
            }
        }

        private ImportValues mapHostImports(
                Import[] imports, ImportValues importValues, int memoryCount) {
            Function<ExternalType, Integer> count =
                    t -> (int) Arrays.stream(imports).filter(i -> i.importType() == t).count();

            // TODO: this can probably be refactored ...
            var hostFuncs = new ImportFunction[count.apply(FUNCTION)];
            var hostFuncIdx = 0;
            var hostGlobals = new ImportGlobal[count.apply(GLOBAL)];
            var hostGlobalIdx = 0;
            var hostMems = new ImportMemory[count.apply(MEMORY)];
            var hostMemIdx = 0;
            if (hostMems.length + memoryCount > 1) {
                throw new InvalidException("multiple memories");
            }
            var hostTables = new ImportTable[count.apply(TABLE)];
            var hostTableIdx = 0;
            int cnt;
            for (var impIdx = 0; impIdx < imports.length; impIdx++) {
                var i = imports[impIdx];
                var name = i.module() + "." + i.name();
                var found = false;
                validateNegativeImportType(i.module(), i.name(), i.importType(), importValues);
                Function<ImportValue, Boolean> checkName =
                        (ImportValue fh) ->
                                i.module().equals(fh.module()) && i.name().equals(fh.name());
                switch (i.importType()) {
                    case FUNCTION:
                        cnt = importValues.functionCount();
                        for (int j = 0; j < cnt; j++) {
                            ImportFunction f = importValues.function(j);
                            if (checkName.apply(f)) {
                                validateExternalFunctionSignature((FunctionImport) i, f);
                                hostFuncs[hostFuncIdx] = f;
                                found = true;
                                break;
                            }
                        }
                        hostFuncIdx++;
                        break;
                    case GLOBAL:
                        cnt = importValues.globalCount();
                        for (int j = 0; j < cnt; j++) {
                            ImportGlobal g = importValues.global(j);
                            if (checkName.apply(g)) {
                                validateHostGlobalType((GlobalImport) i, g);
                                hostGlobals[hostGlobalIdx] = g;
                                found = true;
                                break;
                            }
                        }
                        hostGlobalIdx++;
                        break;
                    case MEMORY:
                        cnt = importValues.memoryCount();
                        for (int j = 0; j < cnt; j++) {
                            ImportMemory m = importValues.memory(j);
                            if (checkName.apply(m)) {
                                validateHostMemoryType((MemoryImport) i, m);
                                hostMems[hostMemIdx] = m;
                                found = true;
                                break;
                            }
                        }
                        hostMemIdx++;
                        break;
                    case TABLE:
                        cnt = importValues.tableCount();
                        for (int j = 0; j < cnt; j++) {
                            ImportTable t = importValues.table(j);
                            if (checkName.apply(t)) {
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
                    throw new UnlinkableException(
                            "unknown import, could not find host function for import number: "
                                    + impIdx
                                    + " named "
                                    + name);
                }
            }

            return ImportValues.builder()
                    .addFunction(hostFuncs)
                    .addGlobal(hostGlobals)
                    .addMemory(hostMems)
                    .addTable(hostTables)
                    .build();
        }

        private Map<String, Export> genExports(ExportSection export) {
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

        public Instance build() {
            Map<String, Export> exports = genExports(module.exportSection());
            var globalInitializers = module.globalSection().globals();

            var dataSegments = module.dataSection().dataSegments();
            var types = module.typeSection().types();
            int numFuncTypes =
                    module.functionSection().functionCount()
                            + module.importSection().count(FUNCTION);

            FunctionBody[] functions = module.codeSection().functionBodies();

            int importId = 0;
            var functionTypes = new int[numFuncTypes];
            var funcIdx = 0;

            int importCount = module.importSection().importCount();
            var imports = new Import[importCount];
            for (int i = 0; i < importCount; i++) {
                Import imprt = module.importSection().getImport(i);
                if (imprt.importType() == FUNCTION) {
                    var type = ((FunctionImport) imprt).typeIndex();
                    if (type >= this.module.typeSection().typeCount()) {
                        throw new InvalidException("unknown type");
                    }
                    functionTypes[funcIdx] = type;
                    funcIdx++;
                }
                imports[importId++] = imprt;
            }

            var mappedHostImports =
                    mapHostImports(
                            imports,
                            requireNonNullElseGet(importValues, ImportValues::empty),
                            module.memorySection().map(MemorySection::memoryCount).orElse(0));

            if (module.startSection().isPresent()) {
                var export =
                        new Export(
                                START_FUNCTION_NAME,
                                (int) module.startSection().get().startIndex(),
                                FUNCTION);
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
            if (module.memorySection().isPresent()) {
                var memories = module.memorySection().get();
                if (memories.memoryCount() > 0) {
                    memory = new Memory(memories.getMemory(0).limits());
                }
            } else {
                if (mappedHostImports != null && mappedHostImports.memoryCount() > 0) {
                    if (mappedHostImports.memory(0) == null
                            || mappedHostImports.memory(0).memory() == null) {
                        throw new InvalidException(
                                "unknown memory, imported memory not defined, cannot run the"
                                        + " program");
                    }
                    memory = mappedHostImports.memory(0).memory();
                } else {
                    // No memory defined
                }
            }

            for (var e : exports.values()) {
                switch (e.exportType()) {
                    case FUNCTION:
                        {
                            if (e.index()
                                    >= module.functionSection().functionCount()
                                            + mappedHostImports.functionCount()) {
                                throw new InvalidException("unknown function " + e.index());
                            }
                            break;
                        }
                    case GLOBAL:
                        {
                            if (e.index()
                                    >= module.globalSection().globalCount()
                                            + mappedHostImports.globalCount()) {
                                throw new InvalidException("unknown global " + e.index());
                            }
                            break;
                        }
                    case TABLE:
                        {
                            if (e.index()
                                    >= module.tableSection().tableCount()
                                            + mappedHostImports.tableCount()) {
                                throw new InvalidException("unknown table " + e.index());
                            }
                            break;
                        }
                    case MEMORY:
                        {
                            var memoryCount =
                                    module.memorySection()
                                            .map(MemorySection::memoryCount)
                                            .orElse(0);
                            if (e.index() >= memoryCount + mappedHostImports.memoryCount()) {
                                throw new InvalidException("unknown memory " + e);
                            }
                            break;
                        }
                }
            }

            if (machineFactory == null) {
                machineFactory = InterpreterMachine::new;
            }

            return new Instance(
                    module,
                    globalInitializers,
                    memory,
                    dataSegments,
                    functions,
                    types,
                    functionTypes,
                    mappedHostImports,
                    tables,
                    elements,
                    exports,
                    machineFactory,
                    initialize,
                    start,
                    listener);
        }
    }
}
