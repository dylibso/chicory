package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.ValueType;

import java.util.List;

public record HostFunction(WasmFunctionHandle handle, String moduleName, String fieldName, List<ValueType> paramTypes, List<ValueType> returnTypes) {
}
