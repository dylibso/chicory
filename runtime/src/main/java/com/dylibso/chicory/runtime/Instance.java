package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.runtime.Machine.computeConstantValue;
import static com.dylibso.chicory.runtime.Module.START_FUNCTION_NAME;

import com.dylibso.chicory.runtime.exceptions.WASMMachineException;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.DataSegment;
import com.dylibso.chicory.wasm.types.ElemElem;
import com.dylibso.chicory.wasm.types.ElemFunc;
import com.dylibso.chicory.wasm.types.ElemMem;
import com.dylibso.chicory.wasm.types.ElemTable;
import com.dylibso.chicory.wasm.types.ElemType;
import com.dylibso.chicory.wasm.types.Element;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Global;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.Value;
import java.util.Arrays;

public class Instance {
    private final Module module;
    private final Machine machine;
    private final FunctionBody[] functions;
    private final Memory memory;
    private final DataSegment[] dataSegments;
    private final Global[] globalInitializers;
    private final Value[] globals;
    private final int importedGlobalsOffset;
    private final int importedFunctionsOffset;
    private final int importedTablesOffset;
    private final FunctionType[] types;
    private final int[] functionTypes;
    private final HostImports imports;
    private final Table[] tables;
    private final Element[] elements;

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
            Element[] elements) {
        this.module = module;
        this.globalInitializers = globalInitializers.clone();
        this.globals = new Value[globalInitializers.length];
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
        this.tables = tables.clone();
        this.elements = elements.clone();

        initialize();
    }

    private void initialize() {

        for (var i = 0; i < globalInitializers.length; i++) {
            var g = globalInitializers[i];
            if (g.initInstructions().length > 2)
                throw new RuntimeException(
                        "We don't support a global initializer with multiple instructions");
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
                        globals[i] = readGlobal(idx);
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

        if (elements != null) {
            for (var el : elements) {
                switch (el.elemType()) {
                    case Type:
                        {
                            var typeElem = (ElemType) el;
                            var expr = typeElem.exprInstructions();
                            var addr = computeConstantValue(this, expr).asInt();
                            for (var fi : typeElem.funcIndices()) {
                                table(0).setRef(addr++, (int) fi);
                            }
                            break;
                        }
                    case Table:
                        {
                            var tableElem = (ElemTable) el;
                            var idx = (int) tableElem.tableIndex();
                            var expr = tableElem.exprInstructions();
                            var addr = computeConstantValue(this, expr).asInt();
                            for (var fi : tableElem.funcIndices()) {
                                table(idx).setRef(addr++, (int) fi);
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

        if (memory != null) {
            memory.initialize(this, dataSegments);
        } else if (imports.memories().length > 0) {
            imports.memories()[0].memory().initialize(this, dataSegments);
        }
        if (module.export(START_FUNCTION_NAME) != null) {
            export(START_FUNCTION_NAME).apply();
        }
    }

    public ExportFunction export(String name) {
        var export = module.export(name);
        if (export == null) throw new ChicoryException("Unknown export with name " + name);
        var funcId = (int) export.desc().index();
        return (args) -> {
            this.module.logger().debug(() -> "Args: " + Arrays.toString(args));
            try {
                return machine.call(this, funcId, args, true);
            } catch (Exception e) {
                throw new WASMMachineException(machine.getStackTrace(), e);
            }
        };
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

    public void writeGlobal(int idx, Value val) {
        if (idx < importedGlobalsOffset) {
            imports.global(idx).setValue(val);
        }
        globals[idx - importedGlobalsOffset] = val;
    }

    public Value readGlobal(int idx) {
        if (idx < importedGlobalsOffset) {
            return imports.global(idx).value();
        }
        return globals[idx - importedGlobalsOffset];
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

    public Table table(int idx) {
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
}
