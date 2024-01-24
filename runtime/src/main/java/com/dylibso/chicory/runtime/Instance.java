package com.dylibso.chicory.runtime;

import com.dylibso.chicory.runtime.exceptions.WASMMachineException;
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
        this.functions = functions.clone();
        this.types = types.clone();
        this.functionTypes = functionTypes.clone();
        this.imports = imports;
        this.machine = new Machine(this);
        this.tables = tables.clone();
        this.elements = elements.clone();
    }

    public ExportFunction getExport(String name) {
        var export = module.getExport(name);
        var funcId = (int) export.getDesc().getIndex();
        return (args) -> {
            this.module.logger().debug(() -> "Args: " + Arrays.toString(args));
            try {
                return machine.call(funcId, args, true);
            } catch (Exception e) {
                throw new WASMMachineException(machine.getStackTrace(), e);
            }
        };
    }

    public FunctionBody getFunction(int idx) {
        if (idx < importedFunctionsOffset) return null;
        return functions[idx - importedFunctionsOffset];
    }

    public int getFunctionCount() {
        return importedFunctionsOffset + functions.length;
    }

    public Memory getMemory() {
        return memory;
    }

    public void setGlobal(int idx, Value val) {
        if (idx < importedGlobalsOffset) {
            imports.getGlobal(idx).setValue(val);
        }
        globals[idx - importedGlobalsOffset] = val;
    }

    public Value getGlobal(int idx) {
        if (idx < importedGlobalsOffset) {
            return null;
        }
        return globals[idx - importedGlobalsOffset];
    }

    public Global getGlobalInitializer(int idx) {
        if (idx < importedGlobalsOffset) {
            return null;
        }
        return globalInitializers[idx - importedGlobalsOffset];
    }

    public int getGlobalCount() {
        return globals.length;
    }

    public FunctionType getType(int idx) {
        return types[idx];
    }

    public int getFunctionType(int idx) {
        return functionTypes[idx];
    }

    public HostImports getImports() {
        return imports;
    }

    public Module getModule() {
        return module;
    }

    public Table getTable(int idx) {
        if (idx < importedTablesOffset) {
            return null;
        }
        return tables[idx - importedTablesOffset];
    }

    public Element getElement(int idx) {
        return elements[idx];
    }

    public int getElementCount() {
        return elements.length;
    }

    public void setElement(int idx, Element val) {
        elements[idx] = val;
    }
}
