package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.runtime.ConstantEvaluators.computeConstantInstance;
import static com.dylibso.chicory.runtime.ConstantEvaluators.computeConstantValue;
import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;

import com.dylibso.chicory.wasm.Module;
import com.dylibso.chicory.wasm.TypeValidator;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.exceptions.InvalidException;
import com.dylibso.chicory.wasm.exceptions.MalformedException;
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
    private final HostImports imports;
    private final Table[] roughTables;
    private TableInstance[] tables;
    private final Element[] elements;
    private final Map<String, Export> exports;
    private final boolean start;
    private final boolean typeValidation;
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
            HostImports imports,
            Table[] tables,
            Element[] elements,
            Map<String, Export> exports,
            Function<Instance, Machine> machineFactory,
            boolean initialize,
            boolean start,
            boolean typeValidation,
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
        this.elements = elements.clone();
        this.exports = exports;
        this.start = start;
        this.listener = listener;
        this.typeValidation = typeValidation;

        if (initialize) {
            initialize(this.start);
        }
    }

    public Instance initialize(boolean start) {
        this.tables = new TableInstance[this.roughTables.length];
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
                    var index = offset.asInt() + i;
                    if (init.stream().filter(e -> e.opcode() != OpCode.END).count() > 1l) {
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
                        if (value.asFuncRef() != REF_NULL_VALUE) {
                            try {
                                function(value.asFuncRef());
                            } catch (InvalidException e) {
                                throw new InvalidException("type mismatch, " + e.getMessage(), e);
                            }
                        }
                        table.setRef(index, value.asFuncRef(), inst);
                    } else {
                        assert ae.type() == ValueType.ExternRef;
                        table.setRef(index, value.asExtRef(), inst);
                    }
                }
            } else if (el instanceof DeclarativeElement) {
                var de = (DeclarativeElement) el;

                List<List<Instruction>> initializers = de.initializers();
                for (int i = 0; i < initializers.size(); i++) {
                    final List<Instruction> init = initializers.get(i);
                    var value = computeConstantValue(this, init);
                    if (de.type() == ValueType.FuncRef
                            && value.asFuncRef() != REF_NULL_VALUE
                            && value.asFuncRef() >= importedFunctionsOffset) {
                        function(value.asFuncRef()).setInitializedByElem(true);
                    }
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
            globals[i] = new GlobalInstance(value);
            globals[i].setInstance(this);
        }

        if (memory != null) {
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
        if (typeValidation) {
            int startFunctionIndex = startFunction == null ? -1 : startFunction.index();
            // TODO: can be parallelized?
            for (int i = 0; i < this.functions.length; i++) {
                if (this.function(i) != null) {
                    var funcType = this.functionType(i);
                    if (funcType >= this.types.length) {
                        throw new InvalidException("unknown type " + funcType);
                    }
                    if (i == startFunctionIndex) {
                        // _start must be () -> ()
                        if (!this.types[funcType].params().isEmpty()
                                || !this.types[funcType].returns().isEmpty()) {
                            throw new InvalidException(
                                    "invalid start function, must have empty signature "
                                            + funcType);
                        }
                    }
                    new TypeValidator(module).validate(i, function(i), types[funcType]);
                }
            }
        }

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
        if (export == null) throw new ChicoryException("Unknown export with name " + name);

        switch (export.exportType()) {
            case FUNCTION:
                {
                    return args -> machine.call(export.index(), args);
                }
            case GLOBAL:
                {
                    return args -> {
                        assert (args.length == 0);
                        return new Value[] {readGlobal(export.index())};
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

    public void writeGlobal(int idx, Value val) {
        if (idx < importedGlobalsOffset) {
            imports.global(idx).instance().setValue(val);
        }
        globals[idx - importedGlobalsOffset].setValue(val);
    }

    public Value readGlobal(int idx) {
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

    public HostImports imports() {
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

    public Value[] callHostFunction(int funcId, Value[] args) {
        var imprt = imports.function(funcId);
        if (imprt == null) {
            throw new ChicoryException("Missing host import, number: " + funcId);
        }
        return imprt.handle().apply(this, args);
    }

    public void onExecution(Instruction instruction, long[] operands, MStack stack) {
        if (listener != null) {
            listener.onExecution(instruction, operands, stack);
        }
    }

    public static Builder builder(Module module) {
        return new Builder(module);
    }

    public static final class Builder {
        private final Module module;

        private boolean initialize = true;
        private boolean start = true;
        private boolean typeValidation = true;
        private boolean importValidation = true;
        private ExecutionListener listener = null;
        private HostImports hostImports = null;
        private Function<Instance, Machine> machineFactory = null;

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

        public Builder withTypeValidation(boolean v) {
            this.typeValidation = v;
            return this;
        }

        public Builder withImportValidation(boolean v) {
            this.importValidation = v;
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

        private void validateHostFunctionSignature(FunctionImport imprt, HostFunction f) {
            var expectedType = module.typeSection().getType(imprt.typeIndex());
            if (expectedType.params().size() != f.paramTypes().size()
                    || expectedType.returns().size() != f.returnTypes().size()) {
                throw new UnlinkableException(
                        "incompatible import type for host function "
                                + f.moduleName()
                                + "."
                                + f.fieldName());
            }
            for (int i = 0; i < expectedType.params().size(); i++) {
                var expected = expectedType.params().get(i);
                var got = f.paramTypes().get(i);
                if (expected != got) {
                    throw new UnlinkableException(
                            "incompatible import type for host function "
                                    + f.moduleName()
                                    + "."
                                    + f.fieldName());
                }
            }
            for (int i = 0; i < expectedType.returns().size(); i++) {
                var expected = expectedType.returns().get(i);
                var got = f.returnTypes().get(i);
                if (expected != got) {
                    throw new UnlinkableException(
                            "incompatible import type for host function "
                                    + f.moduleName()
                                    + "."
                                    + f.fieldName());
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
                                + t.moduleName()
                                + "."
                                + t.fieldName());
            }
        }

        private void validateHostMemoryType(MemoryImport i, HostMemory m) {
            var initialExpected = m.memory().initialPages();
            var maxExpected = m.memory().maximumPages();
            var initialCurrent = i.limits().initialPages();
            var maxCurrent =
                    (i.limits().maximumPages() == MemoryLimits.MAX_PAGES)
                            ? Memory.RUNTIME_MAX_PAGES
                            : i.limits().maximumPages();
            if (initialCurrent > initialExpected
                    || (maxCurrent < maxExpected && maxCurrent == initialCurrent)) {
                throw new UnlinkableException(
                        "incompatible import type, non-compatible limits, expected: "
                                + i.limits()
                                + ", current: "
                                + m.memory().limits()
                                + " on memory: "
                                + m.moduleName()
                                + "."
                                + m.fieldName());
            }
        }

        private void validateNegativeImportType(
                String moduleName, String name, FromHost[] fromHost) {
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

        private static void validateModule(Module module) {
            var functionSectionSize = module.functionSection().functionCount();
            var codeSectionSize = module.codeSection().functionBodyCount();
            var dataSectionSize = module.dataSection().dataSegmentCount();
            if (functionSectionSize != codeSectionSize) {
                throw new MalformedException("function and code section have inconsistent lengths");
            }
            if (module.dataCountSection()
                    .map(dcs -> dcs.dataCount() != dataSectionSize)
                    .orElse(false)) {
                throw new MalformedException(
                        "data count and data section have inconsistent lengths");
            }
        }

        private HostImports mapHostImports(
                Import[] imports, HostImports hostImports, int memoryCount) {
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
                Function<FromHost, Boolean> checkName =
                        (FromHost fh) ->
                                i.moduleName().equals(fh.moduleName())
                                        && i.name().equals(fh.fieldName());
                switch (i.importType()) {
                    case FUNCTION:
                        cnt = hostImports.functionCount();
                        for (int j = 0; j < cnt; j++) {
                            HostFunction f = hostImports.function(j);
                            if (checkName.apply(f)) {
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
                        cnt = hostImports.memoryCount();
                        for (int j = 0; j < cnt; j++) {
                            HostMemory m = hostImports.memory(j);
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
                        cnt = hostImports.tableCount();
                        for (int j = 0; j < cnt; j++) {
                            HostTable t = hostImports.table(j);
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
                    if (importValidation) {
                        throw new UnlinkableException(
                                "unknown import, could not find host function for import number: "
                                        + impIdx
                                        + " named "
                                        + name);
                    } else {
                        System.err.println(
                                "Could not find host function for import number: "
                                        + impIdx
                                        + " named "
                                        + name);
                    }
                }
            }

            var result = new HostImports(hostFuncs, hostGlobals, hostMems, hostTables);
            return result;
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
            validateModule(module);
            Map<String, Export> exports = genExports(module.exportSection());
            var globalInitializers = module.globalSection().globals();

            var dataSegments = module.dataSection().dataSegments();
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

            var mappedHostImports =
                    mapHostImports(
                            imports,
                            (hostImports == null) ? new HostImports() : hostImports,
                            module.memorySection().map(m -> m.memoryCount()).orElse(0));

            if (module.startSection().isPresent()) {
                var export =
                        new Export(
                                START_FUNCTION_NAME,
                                (int) module.startSection().get().startIndex(),
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
            if (module.memorySection().isPresent()) {
                var memories = module.memorySection().get();
                if (memories.memoryCount() > 0) {
                    memory = new Memory(memories.getMemory(0).memoryLimits());
                }
            } else {
                if (mappedHostImports.memoryCount() > 0) {
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
                            if (e.index()
                                    >= module.tableSection().tableCount() + tablesImportsOffset) {
                                throw new InvalidException("unknown table " + e.index());
                            }
                            break;
                        }
                    case MEMORY:
                        {
                            var memoryCount =
                                    module.memorySection().map(m -> m.memoryCount()).orElse(0);
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
                    typeValidation,
                    listener);
        }
    }
}
