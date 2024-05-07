package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.runtime.Machine.computeConstantValue;
import static com.dylibso.chicory.runtime.Module.START_FUNCTION_NAME;

import com.dylibso.chicory.runtime.exceptions.WASMMachineException;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.ActiveElement;
import com.dylibso.chicory.wasm.types.DataSegment;
import com.dylibso.chicory.wasm.types.Element;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Global;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.Arrays;
import java.util.List;

public class Instance {
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
    private final boolean start;
    private final boolean initialize;

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
            boolean initialize,
            boolean start) {
        this.module = module;
        this.globalInitializers = globalInitializers.clone();
        this.globals = new GlobalInstance[globalInitializers.length + importedGlobalsOffset];
        this.importedGlobalsOffset = importedGlobalsOffset;
        this.importedFunctionsOffset = importedFunctionsOffset;
        this.importedTablesOffset = importedTablesOffset;
        this.memory = memory;
        this.dataSegments = dataSegments;
        this.functions = functions.clone();
        this.types = types.clone();
        this.functionTypes = functionTypes.clone();
        this.imports = imports;
        this.machine = new Machine(this);
        this.roughTables = tables.clone();
        this.elements = elements.clone();
        this.start = start;
        this.initialize = initialize;

        if (initialize) {
            initialize(this.start);
        }
    }

    public Instance(Instance instance) {
        this(
                instance.module,
                instance.globalInitializers,
                instance.importedGlobalsOffset,
                instance.importedFunctionsOffset,
                instance.importedTablesOffset,
                instance.memory,
                instance.dataSegments,
                instance.functions,
                instance.types,
                instance.functionTypes,
                instance.imports,
                instance.roughTables,
                instance.elements,
                instance.initialize,
                instance.start);
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

                Value offset = computeConstantValue(this, ae.offset());
                if (offset.type() != ValueType.I32) {
                    throw new ChicoryException("Invalid offset type in element");
                }
                List<List<Instruction>> initializers = ae.initializers();
                for (int i = 0; i < initializers.size(); i++) {
                    final List<Instruction> init = initializers.get(i);
                    var index = offset.asInt() + i;
                    if (ae.type() == ValueType.FuncRef) {
                        table.setRef(index, computeConstantValue(this, init).asFuncRef(), this);
                    } else {
                        assert ae.type() == ValueType.ExternRef;
                        table.setRef(index, computeConstantValue(this, init).asExtRef(), this);
                    }
                }
            }
        }

        for (var i = 0; i < globalInitializers.length; i++) {
            var g = globalInitializers[i];
            if (g.initInstructions().size() > 2)
                throw new RuntimeException(
                        "We don't support a global initializer with multiple instructions");
            var instr = g.initInstructions().get(0);
            switch (instr.opcode()) {
                case I32_CONST:
                    globals[i] = new GlobalInstance((Value.i32(instr.operands()[0])));
                    break;
                case I64_CONST:
                    globals[i] = new GlobalInstance(Value.i64(instr.operands()[0]));
                    break;
                case F32_CONST:
                    globals[i] = new GlobalInstance(Value.f32(instr.operands()[0]));
                    break;
                case F64_CONST:
                    globals[i] = new GlobalInstance(Value.f64(instr.operands()[0]));
                    break;
                case GLOBAL_GET:
                    {
                        var idx = (int) instr.operands()[0];
                        globals[i] =
                                idx < imports.globalCount()
                                        ? imports.global(idx).instance()
                                        : globals[idx];
                        break;
                    }
                case REF_NULL:
                    globals[i] = new GlobalInstance(Value.EXTREF_NULL);
                    break;
                case REF_FUNC:
                    globals[i] = new GlobalInstance(Value.funcRef(instr.operands()[0]));
                    break;
                default:
                    throw new RuntimeException(
                            "We only support i32.const, i64.const, f32.const, f64.const,"
                                    + " global.get, ref.func and ref.null opcodes on global"
                                    + " initializers right now. We failed to initialize opcode: "
                                    + instr.opcode());
            }
        }

        if (memory != null) {
            memory.initialize(this, dataSegments);
        } else if (imports.memories().length > 0) {
            imports.memories()[0].memory().initialize(this, dataSegments);
        }

        if (start && module.export(START_FUNCTION_NAME) != null) {
            export(START_FUNCTION_NAME).apply();
        }

        return this;
    }

    public ExportFunction export(String name) {
        var export = module.export(name);
        if (export == null) throw new ChicoryException("Unknown export with name " + name);

        switch (export.exportType()) {
            case FUNCTION:
                {
                    var funcId = export.index();
                    return (args) -> {
                        this.module.logger().debug(() -> "Args: " + Arrays.toString(args));
                        try {
                            return machine.call(funcId, args, true);
                        } catch (Exception e) {
                            throw new WASMMachineException(machine.getStackTrace(), e);
                        }
                    };
                }
            case GLOBAL:
                {
                    return new ExportFunction() {
                        @Override
                        public Value[] apply(Value... args) throws ChicoryException {
                            assert (args.length == 0);
                            return new Value[] {readGlobal(export.index())};
                        }
                    };
                }
            default:
                {
                    throw new ChicoryException("not implemented");
                }
        }
    }

    public FunctionBody function(int idx) {
        if (idx < importedFunctionsOffset) return null;
        return functions[idx - importedFunctionsOffset];
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
        return globals[idx - importedGlobalsOffset].getValue();
    }

    public Global globalInitializer(int idx) {
        if (idx < importedGlobalsOffset) {
            return null;
        }
        return globalInitializers[idx - importedGlobalsOffset];
    }

    public int globalCount() {
        return globals.length;
    }

    public FunctionType type(int idx) {
        return types[idx];
    }

    public int functionType(int idx) {
        return functionTypes[idx];
    }

    public HostImports imports() {
        return imports;
    }

    public Module module() {
        return module;
    }

    public int importedTableCount() {
        return importedTablesOffset;
    }

    public TableInstance table(int idx) {
        if (idx < importedTablesOffset) {
            return imports.table(idx).table();
        }
        return tables[idx - importedTablesOffset];
    }

    public Element element(int idx) {
        return elements[idx];
    }

    public int elementCount() {
        return elements.length;
    }

    public void setElement(int idx, Element val) {
        elements[idx] = val;
    }

    /*
     * This method is experimental and might be dropped without notice in future releases.
     */
    public void onExecutionUnsafe(Instruction instruction, long[] operands, MStack stack) {
        /*
         * WARNING:
         *
         * Implementing this function you will be executing code on the very hot path of the interpreter for each and every instruction.
         * Any issue or performance degradation caused by this code is not going to be supported.
         * This interface along with its usage is experimental and we might drop it at a later stage.
         *
         * If you have a specific use case for this functionality, please, open an Issue at: https://github.com/dylibso/chicory/issues
         */
    }
}
