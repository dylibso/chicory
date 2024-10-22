package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;

public class ImportFunction implements ImportValue {
    private final String module;
    private final String name;
    private final List<ValueType> paramTypes;
    private final List<ValueType> returnTypes;
    private final WasmFunctionHandle handle;

    public ImportFunction(
            String module,
            String name,
            List<ValueType> paramTypes,
            List<ValueType> returnTypes,
            WasmFunctionHandle handle) {
        this.module = module;
        this.name = name;
        this.paramTypes = paramTypes;
        this.returnTypes = returnTypes;
        this.handle = handle;
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

    public List<ValueType> paramTypes() {
        return paramTypes;
    }

    public List<ValueType> returnTypes() {
        return returnTypes;
    }
}
