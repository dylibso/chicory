package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.runtime.ConstantEvaluators.computeConstantInstance;
import static com.dylibso.chicory.runtime.ConstantEvaluators.computeConstantValue;
import static com.dylibso.chicory.wasm.types.ExternalType.FUNCTION;
import static com.dylibso.chicory.wasm.types.ExternalType.GLOBAL;
import static com.dylibso.chicory.wasm.types.ExternalType.MEMORY;
import static com.dylibso.chicory.wasm.types.ExternalType.TABLE;
import static com.dylibso.chicory.wasm.types.ExternalType.TAG;
import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.InvalidException;
import com.dylibso.chicory.wasm.UninstantiableException;
import com.dylibso.chicory.wasm.UnlinkableException;
import com.dylibso.chicory.wasm.WasmModule;
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
import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.TableImport;
import com.dylibso.chicory.wasm.types.TagImport;
import com.dylibso.chicory.wasm.types.TagSection;
import com.dylibso.chicory.wasm.types.TagType;
import com.dylibso.chicory.wasm.types.ValType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Instance {
    public static final String START_FUNCTION_NAME = "_start";

    private final WasmModule module;
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
    private final TagInstance[] tags;
    private final Map<String, Export> exports;
    private final ExecutionListener listener;
    private final Exports fluentExports;

    private final Map<Integer, WasmException> exnRefs;

    Instance(
            WasmModule module,
            Global[] globalInitializers,
            Memory memory,
            DataSegment[] dataSegments,
            FunctionBody[] functions,
            FunctionType[] types,
            int[] functionTypes,
            ImportValues imports,
            Table[] tables,
            Element[] elements,
            TagType[] tags,
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
        this.elements = elements.clone();
        this.tags = (tags == null) ? new TagInstance[0] : new TagInstance[tags.length];
        for (int i = 0; i < this.tags.length; i++) {
            this.tags[i] = new TagInstance(tags[i]);
            this.tags[i].setType(types[tags[i].typeIdx()]);
        }
        this.exports = exports;
        this.listener = listener;
        this.fluentExports = new Exports(this);

        this.exnRefs = new HashMap<>();

        for (int i = 0; i < tables.length; i++) {
            var initValue = (int) computeConstantValue(this, tables[i].initialize())[0];
            this.tables[i] = new TableInstance(tables[i], initValue);
        }

        if (initialize) {
            initialize(start);
        }
    }

    public Instance initialize(boolean start) {
        for (var el : elements) {
            if (el instanceof ActiveElement) {
                var ae = (ActiveElement) el;
                var table = table(ae.tableIndex());
                int offset = (int) computeConstantValue(this, ae.offset())[0];

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

                    assert ae.type().isReference();
                    table.setRef(index, (int) value[0], inst);
                }
            }
        }

        for (var i = 0; i < globalInitializers.length; i++) {
            var g = globalInitializers[i];
            var values = computeConstantValue(this, g.initInstructions());
            globals[i] =
                    new GlobalInstance(
                            values[0],
                            (values.length > 1) ? values[1] : 0,
                            g.valueType(),
                            g.mutabilityType());
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

    public static final class Exports {
        private final Instance instance;

        private Exports(Instance instance) {
            this.instance = instance;
        }

        private Export getExport(ExternalType type, String name) throws InvalidException {
            var export = instance.exports.get(name);
            if (export == null) {
                throw new InvalidException("Unknown export with name " + name);
            } else if (export.exportType() != type) {
                throw new InvalidException(
                        "The export "
                                + export.name()
                                + " is of type "
                                + export.exportType()
                                + " and cannot be converted to "
                                + type);
            }
            return export;
        }

        public ExportFunction function(String name) {
            var export = getExport(FUNCTION, name);
            return args -> instance.machine.call(export.index(), args);
        }

        public GlobalInstance global(String name) {
            var export = getExport(GLOBAL, name);
            return instance.global(export.index());
        }

        public TableInstance table(String name) {
            var export = getExport(TABLE, name);
            return instance.table(export.index());
        }

        public Memory memory(String name) {
            var export = getExport(MEMORY, name);
            assert (export.index() == 0);
            return instance.memory();
        }
    }

    public Exports exports() {
        return fluentExports;
    }

    public ExportFunction export(String name) {
        return this.fluentExports.function(name);
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

    public WasmModule module() {
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

    public TagInstance tag(int idx) {
        if (idx < imports.tagCount()) {
            return imports.tag(idx).tag();
        }
        return tags[idx - imports.tagCount()];
    }

    public int tagCount() {
        return tags.length;
    }

    public int registerException(WasmException ex) {
        exnRefs.put(ex.tagIdx(), ex);
        return ex.tagIdx();
    }

    public WasmException exn(int idx) {
        return exnRefs.get(idx);
    }

    public Machine getMachine() {
        return machine;
    }

    void onExecution(Instruction instruction, MStack stack) {
        if (listener != null) {
            listener.onExecution(instruction, stack);
        }
    }

    public static Builder builder(WasmModule module) {
        return new Builder(module);
    }

    public static final class Builder {
        private final WasmModule module;

        private boolean initialize = true;
        private boolean start = true;
        private MemoryLimits memoryLimits;
        private Function<MemoryLimits, Memory> memoryFactory;
        private ExecutionListener listener;
        private ImportValues importValues;
        private Function<Instance, Machine> machineFactory;

        private Builder(WasmModule module) {
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

        public Builder withMemoryLimits(MemoryLimits limits) {
            this.memoryLimits = limits;
            return this;
        }

        public Builder withMemoryFactory(Function<MemoryLimits, Memory> memoryFactory) {
            this.memoryFactory = memoryFactory;
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

        private boolean checkExternalFunctionSignature(FunctionImport imprt, ImportFunction f) {
            try {
                validateExternalFunctionSignature(imprt, f);
                return true;
            } catch (UnlinkableException e) {
                return false;
            }
        }

        private void validateExternalFunctionSignature(FunctionImport imprt, ImportFunction f) {
            // TODO: not too sure how defined function types across modules are checked.
            // are they substitued until we get an exact type?
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
                if (!ValType.matches(module, got, expected)) {
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
                // TODO: runtime subtype check?
                if (!ValType.matches(module, expected, got)) {
                    throw new UnlinkableException(
                            "incompatible import type for host function "
                                    + f.module()
                                    + "."
                                    + f.name());
                }
            }
        }

        private boolean checkHostGlobalType(GlobalImport i, ImportGlobal g) {
            try {
                validateHostGlobalType(i, g);
                return true;
            } catch (UnlinkableException e) {
                return false;
            }
        }

        private void validateHostGlobalType(GlobalImport i, ImportGlobal g) {
            boolean typesMatch;

            if (i.mutabilityType() == MutabilityType.Var) {
                // for mutable globals, types must match exactly
                typesMatch = i.type().equals(g.instance().getType());
            } else if (i.mutabilityType() == MutabilityType.Const) {
                // for const, subtyping is allowed
                typesMatch = ValType.matches(module, g.instance().getType(), i.type());
            } else {
                throw new ChicoryException(
                        "internal error: mutability type is not var or const: "
                                + i.mutabilityType());
            }

            if (!typesMatch || i.mutabilityType() != g.instance().getMutabilityType()) {
                throw new UnlinkableException("incompatible import type");
            }
        }

        private boolean checkHostTagType(TagImport i, ImportTag t) {
            try {
                validateHostTagType(i, t);
                return true;
            } catch (UnlinkableException e) {
                return false;
            }
        }

        private void validateHostTagType(TagImport i, ImportTag t) {
            var expectedType = module.typeSection().getType(i.tagType().typeIdx());
            var gotType = t.tag().type();
            if (expectedType.params().size() != gotType.params().size()
                    || expectedType.returns().size() != gotType.returns().size()) {
                throw new UnlinkableException(
                        "incompatible import type for tag " + t.module() + "." + t.name());
            }
            for (int j = 0; j < expectedType.params().size(); j++) {
                var expected = expectedType.params().get(j);
                var got = gotType.params().get(j);
                if (!expected.equals(got)) {
                    throw new UnlinkableException(
                            "incompatible import type for tag " + t.module() + "." + t.name());
                }
            }
            for (int j = 0; j < expectedType.returns().size(); j++) {
                var expected = expectedType.returns().get(j);
                var got = gotType.returns().get(j);
                if (!expected.equals(got)) {
                    throw new UnlinkableException(
                            "incompatible import type for tag " + t.module() + "." + t.name());
                }
            }
        }

        private boolean checkHostTableType(TableImport i, ImportTable t) {
            try {
                validateHostTableType(i, t);
                return true;
            } catch (UnlinkableException e) {
                return false;
            }
        }

        private void validateHostTableType(TableImport i, ImportTable t) {
            var minExpected = t.table().limits().min();
            var maxExpected = t.table().limits().max();
            var minCurrent = i.limits().min();
            var maxCurrent = i.limits().max();
            if (!i.entryType().equals(t.table().elementType())) {
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
                    validateNegativeImportType(moduleName, name, importValues.tags());
                    break;
                case GLOBAL:
                    validateNegativeImportType(moduleName, name, importValues.functions());
                    validateNegativeImportType(moduleName, name, importValues.memories());
                    validateNegativeImportType(moduleName, name, importValues.tables());
                    validateNegativeImportType(moduleName, name, importValues.tags());
                    break;
                case MEMORY:
                    validateNegativeImportType(moduleName, name, importValues.functions());
                    validateNegativeImportType(moduleName, name, importValues.globals());
                    validateNegativeImportType(moduleName, name, importValues.tables());
                    validateNegativeImportType(moduleName, name, importValues.tags());
                    break;
                case TABLE:
                    validateNegativeImportType(moduleName, name, importValues.functions());
                    validateNegativeImportType(moduleName, name, importValues.globals());
                    validateNegativeImportType(moduleName, name, importValues.memories());
                    validateNegativeImportType(moduleName, name, importValues.tags());
                    break;
                case TAG:
                    validateNegativeImportType(moduleName, name, importValues.functions());
                    validateNegativeImportType(moduleName, name, importValues.globals());
                    validateNegativeImportType(moduleName, name, importValues.memories());
                    validateNegativeImportType(moduleName, name, importValues.tables());
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
            var hostTags = new ImportTag[count.apply(TAG)];
            var hostTagIdx = 0;
            if (hostMems.length + memoryCount > 1) {
                throw new InvalidException("multiple memories");
            }
            var hostTables = new ImportTable[count.apply(TABLE)];
            var hostTableIdx = 0;
            int cnt;
            var names =
                    Arrays.stream(imports)
                            .map(i -> i.module() + "." + i.name())
                            .collect(Collectors.toList());
            for (var impIdx = 0; impIdx < imports.length; impIdx++) {
                var i = imports[impIdx];
                var name = i.module() + "." + i.name();
                var aliases = names.stream().filter(s -> s.equals(name));
                var aliasesCount = aliases.count();
                var aliasNum = 0;
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
                                if (aliasesCount == 1 || ++aliasNum == aliasesCount) {
                                    validateExternalFunctionSignature((FunctionImport) i, f);
                                } else if (!checkExternalFunctionSignature((FunctionImport) i, f)) {
                                    continue;
                                }
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
                                if (aliasesCount == 1 || ++aliasNum == aliasesCount) {
                                    validateHostGlobalType((GlobalImport) i, g);
                                } else if (!checkHostGlobalType((GlobalImport) i, g)) {
                                    continue;
                                }
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
                                if (aliasesCount == 1 || ++aliasNum == aliasesCount) {
                                    validateHostTableType((TableImport) i, t);
                                } else if (!checkHostTableType((TableImport) i, t)) {
                                    continue;
                                }
                                hostTables[hostTableIdx] = t;
                                found = true;
                                break;
                            }
                        }
                        hostTableIdx++;
                        break;
                    case TAG:
                        cnt = importValues.tagCount();
                        for (int j = 0; j < cnt; j++) {
                            ImportTag t = importValues.tag(j);
                            if (checkName.apply(t)) {
                                if (aliasesCount == 1 || ++aliasNum == aliasesCount) {
                                    validateHostTagType((TagImport) i, t);
                                } else if (!checkHostTagType((TagImport) i, t)) {
                                    continue;
                                }
                                hostTags[hostTagIdx] = t;
                                found = true;
                                break;
                            }
                        }
                        hostTagIdx++;
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
                    .addTag(hostTags)
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
                    var defaultLimits = memories.getMemory(0).limits();
                    memory =
                            requireNonNullElse(memoryFactory, ByteBufferMemory::new)
                                    .apply(requireNonNullElse(memoryLimits, defaultLimits));
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
                    module.tagSection().map(TagSection::types).orElse(null),
                    exports,
                    machineFactory,
                    initialize,
                    start,
                    listener);
        }
    }
}
