package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.runtime.Module.START_FUNCTION_NAME;

import com.dylibso.chicory.runtime.exceptions.WASMMachineException;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.DataSegment;
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
            Value[] globals,
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
        this.globals = globals.clone();
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
        if (memory != null) {
            memory.initialize(dataSegments);
        } else if (imports.memories().length > 0) {
            imports.memories()[0].memory().initialize(dataSegments);
        }
        if (module.export(START_FUNCTION_NAME) != null) {
            export(START_FUNCTION_NAME).apply();
        }
    }

    public ExportFunction export(String name) {
        var export = module.export(name);
        if (export == null) throw new ChicoryException("Unknown export with name " + name);
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
            return null;
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
            return null;
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
