package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.ArrayList;
import java.util.List;

public class ImportFunction implements ImportValue {
    private final String module;
    private final String name;
    private final List<ValType> paramTypes;
    private final List<ValType> returnTypes;
    private final WasmFunctionHandle handle;
    // Optional: source instance for cross-module canonical type matching
    private final Instance sourceInstance;

    @Deprecated(since = "1.3.0")
    protected static List<ValType> convert(List objs) {
        var result = new ArrayList<ValType>(objs.size());
        for (var v : objs) {
            if (v instanceof ValType) {
                result.add((ValType) v);
            } else if (v instanceof ValueType) {
                result.add(((ValueType) v).toValType());
            } else {
                throw new IllegalArgumentException(
                        "Expected ValueType or ValType, but got: "
                                + v.getClass().getCanonicalName());
            }
        }
        return result;
    }

    public ImportFunction(
            String module, String name, FunctionType type, WasmFunctionHandle handle) {
        this(module, name, type, handle, null);
    }

    public ImportFunction(
            String module,
            String name,
            FunctionType type,
            WasmFunctionHandle handle,
            Instance sourceInstance) {
        this.module = module;
        this.name = name;
        this.paramTypes = type.params();
        this.returnTypes = type.returns();
        this.handle = handle;
        this.sourceInstance = sourceInstance;
    }

    @Deprecated(since = "1.3.0")
    public ImportFunction(
            String module,
            String name,
            List paramTypes,
            List returnTypes,
            WasmFunctionHandle handle) {
        this.module = module;
        this.name = name;
        this.paramTypes = convert(paramTypes);
        this.returnTypes = convert(returnTypes);
        this.handle = handle;
        this.sourceInstance = null;
    }

    public WasmFunctionHandle handle() {
        return handle;
    }

    @Override
    public String module() {
        return module;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Type type() {
        return Type.FUNCTION;
    }

    public List<ValType> paramTypes() {
        return paramTypes;
    }

    public List<ValType> returnTypes() {
        return returnTypes;
    }

    public FunctionType functionType() {
        return FunctionType.of(paramTypes, returnTypes);
    }

    public Instance sourceInstance() {
        return sourceInstance;
    }

    /**
     * Creates an {@link ImportFunction} that wraps an exported function from an instance,
     * suitable for linking into another module's imports via the store.
     *
     * @param moduleName the import module name to register under
     * @param fieldName the import field name to register under
     * @param instance the source instance that exports the function
     * @param exportName the name of the export in the source instance
     */
    public static ImportFunction exportAsImport(
            String moduleName, String fieldName, Instance instance, String exportName) {
        ExportFunction f = instance.export(exportName);
        FunctionType ftype = instance.exportType(exportName);
        return new ImportFunction(
                moduleName, fieldName, ftype, (inst, args) -> f.apply(args), instance);
    }
}
