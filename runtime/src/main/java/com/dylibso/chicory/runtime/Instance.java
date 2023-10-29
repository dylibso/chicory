package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.*;
import java.util.Arrays;

public class Instance {
    private static final System.Logger LOGGER = System.getLogger(Instance.class.getName());
    private Module module;
    private Machine machine;
    private FunctionBody[] functions;
    private Memory memory;
    private Global[] globalInitalizers;
    private Value[] globals;
    private FunctionType[] types;
    private int[] functionTypes;
    private HostFunction[] imports;
    private Table table;

    public Instance(
            Module module,
            Global[] globalInitalizers,
            Value[] globals,
            Memory memory,
            FunctionBody[] functions,
            FunctionType[] types,
            int[] functionTypes,
            HostFunction[] imports,
            Table table) {
        this.module = module;
        this.globalInitalizers = globalInitalizers;
        this.globals = globals;
        this.memory = memory;
        this.functions = functions;
        this.types = types;
        this.functionTypes = functionTypes;
        this.imports = imports;
        this.machine = new Machine(this);
        this.table = table;
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
        if (idx < this.imports.length) return null;
        return functions[idx - this.imports.length];
    }

    public Memory getMemory() {
        return memory;
    }

    public Value[] getGlobals() {
        return globals;
    }

    public void setGlobal(int idx, Value val) {
        globals[idx] = val;
    }

    public Value getGlobal(int idx) {
        return globals[idx];
    }

    public Global[] getGlobalInitalizers() {
        return globalInitalizers;
    }

    public FunctionType[] getTypes() {
        return types;
    }

    public int[] getFunctionTypes() {
        return functionTypes;
    }

    public HostFunction[] getImports() {
        return imports;
    }

    public Module getModule() {
        return module;
    }

    public Table getTable() {
        return table;
    }
}
