package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.*;
import java.util.Arrays;

public class Instance {
    private static final System.Logger LOGGER = System.getLogger(Instance.class.getName());
    private Module module;
    private Machine machine;
    private FunctionBody[] functions;
    private Memory memory;
    private Global[] globalInitializers;
    private Value[] globals;

    private int importedGlobalsOffset;
    private int importedFunctionsOffset;
    private int importedTablesOffset;
    private FunctionType[] types;
    private int[] functionTypes;
    private HostImports imports;
    private Table[] tables;
    private Element[] elements;

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
        this.globalInitializers = globalInitializers;
        this.globals = globals;
        this.importedGlobalsOffset = importedGlobalsOffset;
        this.importedFunctionsOffset = importedFunctionsOffset;
        this.importedTablesOffset = importedTablesOffset;
        this.memory = memory;
        this.functions = functions;
        this.types = types;
        this.functionTypes = functionTypes;
        this.imports = imports;
        this.machine = new Machine(this);
        this.tables = tables;
        this.elements = elements;
    }

    public ExportFunction getExport(String name) {
        var export = module.getExport(name);
        var funcId = (int) export.getDesc().getIndex();
        return (args) -> {
            LOGGER.log(System.Logger.Level.DEBUG, "Args: " + Arrays.toString(args));
            try {
                return machine.call(funcId, args, true);
            } catch (Exception e) {
                machine.printStackTrace();
                throw e;
            }
        };
    }

    public FunctionBody[] getFunctions() {
        return functions;
    }

    public FunctionBody getFunction(int idx) {
        if (idx < importedFunctionsOffset) return null;
        return functions[idx - importedFunctionsOffset];
    }

    public Memory getMemory() {
        return memory;
    }

    public Value[] getGlobals() {
        return globals;
    }

    public void setGlobal(int idx, Value val) {
        if (idx < importedGlobalsOffset) {
            imports.getGlobals()[idx].setValue(val);
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

    public FunctionType[] getTypes() {
        return types;
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

    public void setElement(int idx, Element val) {
        elements[idx] = val;
    }
}
