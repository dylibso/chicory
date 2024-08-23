package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;

public class FunctionSignature {
    private final String moduleName;
    private final String name;
    private final List<ValueType> paramTypes;
    private final List<ValueType> returnTypes;

    public FunctionSignature(
            String moduleName,
            String name,
            List<ValueType> paramTypes,
            List<ValueType> returnTypes) {
        this.moduleName = moduleName;
        this.name = name;
        this.paramTypes = paramTypes;
        this.returnTypes = returnTypes;
    }

    public String moduleName() {
        return moduleName;
    }

    public String name() {
        return name;
    }

    public List<ValueType> paramTypes() {
        return paramTypes;
    }

    public List<ValueType> returnTypes() {
        return returnTypes;
    }
}
