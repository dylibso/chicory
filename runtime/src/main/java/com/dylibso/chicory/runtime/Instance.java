package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.runtime.ConstantEvaluators.computeConstantInstance;
import static com.dylibso.chicory.runtime.ConstantEvaluators.computeConstantValue;
import static com.dylibso.chicory.wasm.types.ExternalType.FUNCTION;
import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;
import static java.util.Objects.requireNonNullElseGet;

import com.dylibso.chicory.wasm.Module;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.exceptions.InvalidException;
import com.dylibso.chicory.wasm.exceptions.UninstantiableException;
import com.dylibso.chicory.wasm.exceptions.UnlinkableException;
import com.dylibso.chicory.wasm.types.ActiveDataSegment;
import com.dylibso.chicory.wasm.types.ActiveElement;
import com.dylibso.chicory.wasm.types.DataSegment;
import com.dylibso.chicory.wasm.types.DeclarativeElement;
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
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.OpCode;
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
    private final int importedGlobalsOffset;
    private final int importedFunctionsOffset;
    private final int importedTablesOffset;
    private final FunctionType[] types;
    private final int[] functionTypes;
    private final ExternalValues imports;
    private final Table[] roughTables;
    private final TableInstance[] tables;
    private final Element[] elements;
    private final Map<String, Export> exports;
    private final ExecutionListener listener;

    public Instance(
            Module module,
            Global[] globalInitializers,
            int importedGlobalsOffset,
            int importedFunctionsOffset,
            int importedTablesOffset,
            Memory memory,
            DataSegment[] dataSegments,
            FunctionBody[] functions,
            FunctionType[] types,
            int[] functionTypes,
            ExternalValues imports,
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
        this.importedGlobalsOffset = importedGlobalsOffset;
        this.importedFunctionsOffset = importedFunctionsOffset;
        this.importedTablesOffset = importedTablesOffset;
        this.memory = memory;
        this.dataSegments = dataSegments;
        this.functions = functions.clone();
        this.types = types.clone();
        this.functionTypes = functionTypes.clone();
        this.imports = imports;
        this.machine = machineFactory.apply(this);
        this.roughTables = tables.clone();
        this.tables = new TableInstance[this.roughTables.length];
        this.elements = elements.clone();
        this.exports = exports;
        this.listener = listener;

        if (initialize) {
            initialize(start);
        }
    }

    public Instance initialize(boolean start) {
        for (var i = 0; i < this.roughTables.length; i++) {
            this.tables[i] = new TableInstance(this.roughTables[i]);
        }

        for (var el : elements) {
            if (el instanceof ActiveElement) {
                var ae = (ActiveElement) el;
                var table = table(ae.tableIndex());

                if (ae.offset().size() > 1) {
                    throw new InvalidException(
                            "constant expression required, type mismatch, expected [] but found"
                                    + " extra instructions");
                }
                Value offset = computeConstantValue(this, ae.offset());
                if (offset.type() != ValueType.I32) {
                    throw new InvalidException(
                            "type mismatch, invalid offset type in element " + offset.type());
                }
                List<List<Instruction>> initializers = ae.initializers();
                if (offset.asInt() > table.limits().min()
                        || (offset.asInt() + initializers.size() - 1) >= table.size()) {
                    throw new UninstantiableException("out of bounds table access");
                }
                for (int i = 0; i < initializers.size(); i++) {
                    final List<Instruction> init = initializers.get(i);
                    int index = offset.asInt() + i;
                    if (init.stream().filter(e -> e.opcode() != OpCode.END).count() > 1L) {
                        throw new InvalidException(
                                "constant expression required, type mismatch, expected [] but found"
                                        + " extra instructions");
                    }
                    var value = computeConstantValue(this, init);
                    var inst = computeConstantInstance(this, init);
                    if (value.type() != ae.type() || table.elementType() != ae.type()) {
                        throw new InvalidException(
                                "type mismatch, element type: "
                                        + ae.type()
                                        + ", table type: "
                                        + table.elementType()
                                        + ", value type: "
                                        + value.type());
                    }
                    if (ae.type() == ValueType.FuncRef) {
                        if (((int) value.raw()) != REF_NULL_VALUE) {
                            try {
                                function(value.raw());
                            } catch (InvalidException e) {
                                throw new InvalidException("type mismatch, " + e.getMessage(), e);
                            }
                        }
                        table.setRef(index, (int) value.raw(), inst);
                    } else {
                        assert ae.type() == ValueType.ExternRef;
                        table.setRef(index, (int) value.raw(), inst);
                    }
                }
            } else if (el instanceof DeclarativeElement) {
                for (var init : el.initializers()) {
                    computeConstantValue(this, init);
                }
            }
        }

        for (var i = 0; i < globalInitializers.length; i++) {
            var g = globalInitializers[i];
            if (g.mutabilityType() == MutabilityType.Const && g.initInstructions().size() > 1) {
                throw new InvalidException(
                        "constant expression required, type mismatch, expected [] but found extra"
                                + " instructions");
            }
            var value = computeConstantValue(this, g.initInstructions());
            if (g.valueType() != value.type()) {
                throw new InvalidException(
                        "type mismatch, expected: " + g.valueType() + ", got: " + value.type());
            }
            globals[i] = new GlobalInstance(value, g.mutabilityType());
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
                        var v = readGlobal(export.index());
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
        if (idx < 0 || idx >= (functions.length + importedFunctionsOffset)) {
            throw new InvalidException("unknown function " + idx);
        } else if (idx < importedFunctionsOffset) {
            return null;
        }
        return functions[(int) idx - importedFunctionsOffset];
    }

    public int functionCount() {
        return importedFunctionsOffset + functions.length;
    }

    public Memory memory() {
        return memory;
    }

    public GlobalInstance global(int idx) {
        if (idx < importedGlobalsOffset) {
            return imports.global(idx).instance();
        }
        return globals[idx - importedGlobalsOffset];
    }

    public void writeGlobal(int idx, long val) {
        if (idx < importedGlobalsOffset) {
            imports.global(idx).instance().setValue(val);
        }
        globals[idx - importedGlobalsOffset].setValue(val);
    }

    public ValueType readGlobalType(int idx) {
        if (idx < importedGlobalsOffset) {
            return imports.global(idx).instance().getType();
        }
        return globals[idx - importedGlobalsOffset].getType();
    }

    public long readGlobal(int idx) {
        if (idx < importedGlobalsOffset) {
            return imports.global(idx).instance().getValue();
        }
        var i = idx - importedGlobalsOffset;
        if (i < 0 || i >= globals.length) {
            throw new InvalidException("unknown global " + idx);
        }
        return globals[idx - importedGlobalsOffset].getValue();
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

    public ExternalValues imports() {
        return imports;
    }

    public Module module() {
        return module;
    }

    public TableInstance table(int idx) {
        if (idx < 0 || idx >= (tables.length + importedTablesOffset)) {
            throw new InvalidException("unknown table " + idx);
        }
        if (idx < importedTablesOffset) {
            return imports.table(idx).table();
        }
        return tables[idx - importedTablesOffset];
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

    public long[] callHostFunction(int funcId, long[] args) {
        var imprt = imports.function(funcId);
        if (imprt == null) {
            throw new ChicoryException("Missing host import, number: " + funcId);
        }
        return imprt.handle().apply(this, args);
    }

    public void onExecution(Instruction instruction, MStack stack) {
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
        private boolean skipImportMapping;
        private ExecutionListener listener;
        private ExternalValues externalValues;
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

        public Builder withSkipImportMapping(boolean s) {
            this.skipImportMapping = s;
            return this;
        }

        /*
         * This method is experimental and might be dropped without notice in future releases.
         */
        public Builder withUnsafeExecutionListener(ExecutionListener listener) {
            this.listener = listener;
            return this;
        }

        public Builder withExternalValues(ExternalValues externalValues) {
            this.externalValues = externalValues;
            return this;
        }

        public Builder withMachineFactory(Function<Instance, Machine> machineFactory) {
            this.machineFactory = machineFactory;
            return this;
        }

        private void validateExternalFunctionSignature(FunctionImport imprt, ExternalFunction f) {
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

        private void validateHostGlobalType(GlobalImport i, ExternalGlobal g) {
            if (i.type() != g.instance().getType()
                    || i.mutabilityType() != g.instance().getMutabilityType()) {
                throw new UnlinkableException("incompatible import type");
            }
        }

        private void validateHostTableType(TableImport i, ExternalTable t) {
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

        private void validateHostMemoryType(MemoryImport i, ExternalMemory m) {
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
                                + ", host: "
                                + m.memory().limits()
                                + " on memory: "
                                + m.module()
                                + "."
                                + m.name());
            }
        }

        private void validateNegativeImportType(
                String moduleName, String name, ExternalValue[] external) {
            for (var fh : external) {
                if (fh.module().equals(moduleName) && fh.name().equals(name)) {
                    throw new UnlinkableException("incompatible import type");
                }
            }
        }

        private void validateNegativeImportType(
                String moduleName, String name, ExternalType typ, ExternalValues externalValues) {
            switch (typ) {
                case FUNCTION:
                    validateNegativeImportType(moduleName, name, externalValues.globals());
                    validateNegativeImportType(moduleName, name, externalValues.memories());
                    validateNegativeImportType(moduleName, name, externalValues.tables());
                    break;
                case GLOBAL:
                    validateNegativeImportType(moduleName, name, externalValues.functions());
                    validateNegativeImportType(moduleName, name, externalValues.memories());
                    validateNegativeImportType(moduleName, name, externalValues.tables());
                    break;
                case MEMORY:
                    validateNegativeImportType(moduleName, name, externalValues.functions());
                    validateNegativeImportType(moduleName, name, externalValues.globals());
                    validateNegativeImportType(moduleName, name, externalValues.tables());
                    break;
                case TABLE:
                    validateNegativeImportType(moduleName, name, externalValues.functions());
                    validateNegativeImportType(moduleName, name, externalValues.globals());
                    validateNegativeImportType(moduleName, name, externalValues.memories());
                    break;
            }
        }

        private ExternalValues mapHostImports(
                Import[] imports, ExternalValues externalValues, int memoryCount) {
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

            if (hostMemNum + memoryCount > 1) {
                throw new InvalidException("multiple memories");
            }

            // TODO: this can probably be refactored ...
            var hostFuncs = new ExternalFunction[hostFuncNum];
            var hostFuncIdx = 0;
            var hostGlobals = new ExternalGlobal[hostGlobalNum];
            var hostGlobalIdx = 0;
            var hostMems = new ExternalMemory[hostMemNum];
            var hostMemIdx = 0;
            var hostTables = new ExternalTable[hostTableNum];
            var hostTableIdx = 0;
            int cnt;
            for (var impIdx = 0; impIdx < imports.length; impIdx++) {
                var i = imports[impIdx];
                var name = i.module() + "." + i.name();
                var found = false;
                validateNegativeImportType(i.module(), i.name(), i.importType(), externalValues);
                Function<ExternalValue, Boolean> checkName =
                        (ExternalValue fh) ->
                                i.module().equals(fh.module()) && i.name().equals(fh.name());
                switch (i.importType()) {
                    case FUNCTION:
                        cnt = externalValues.functionCount();
                        for (int j = 0; j < cnt; j++) {
                            ExternalFunction f = externalValues.function(j);
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
                        cnt = externalValues.globalCount();
                        for (int j = 0; j < cnt; j++) {
                            ExternalGlobal g = externalValues.global(j);
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
                        cnt = externalValues.memoryCount();
                        for (int j = 0; j < cnt; j++) {
                            ExternalMemory m = externalValues.memory(j);
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
                        cnt = externalValues.tableCount();
                        for (int j = 0; j < cnt; j++) {
                            ExternalTable t = externalValues.table(j);
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

            return new ExternalValues(hostFuncs, hostGlobals, hostMems, hostTables);
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
                    skipImportMapping
                            ? null
                            : mapHostImports(
                                    imports,
                                    requireNonNullElseGet(externalValues, ExternalValues::new),
                                    module.memorySection()
                                            .map(MemorySection::memoryCount)
                                            .orElse(0));

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
                    memory = new Memory(memories.getMemory(0).memoryLimits());
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

            // TODO: refactor with "mapHostImports"
            var globalImportsOffset = 0;
            var functionImportsOffset = 0;
            var tablesImportsOffset = 0;
            var memoryImportsOffset = 0;
            for (Import imp : imports) {
                switch (imp.importType()) {
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
                            if (e.index()
                                    >= module.tableSection().tableCount() + tablesImportsOffset) {
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
                            if (e.index() >= memoryCount + memoryImportsOffset) {
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
                    exports,
                    machineFactory,
                    initialize,
                    start,
                    listener);
        }
    }
}
