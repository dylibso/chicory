package com.dylibso.chicory.wasm.types;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public final class FunctionType {
    private final List<ValueType> params;
    private final List<ValueType> returns;
    private final int hashCode;

    private FunctionType(List<ValueType> params, List<ValueType> returns) {
        this.params = params;
        this.returns = returns;
        hashCode = Objects.hash(params, returns);
    }

    public List<ValueType> params() {
        return params;
    }

    public List<ValueType> returns() {
        return returns;
    }

    public boolean paramsMatch(FunctionType other) {
        return params.equals(other.params);
    }

    public boolean returnsMatch(FunctionType other) {
        return returns.equals(other.returns);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FunctionType && equals((FunctionType) obj);
    }

    public boolean equals(FunctionType other) {
        return hashCode == other.hashCode && paramsMatch(other) && returnsMatch(other);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private static final HashMap<ValueType, FunctionType> returning;
    private static final HashMap<ValueType, FunctionType> accepting;

    static {
        HashMap<ValueType, FunctionType> map = new HashMap<>();
        map.put(ValueType.ExternRef, new FunctionType(List.of(), List.of(ValueType.ExternRef)));
        map.put(ValueType.ExnRef, new FunctionType(List.of(), List.of(ValueType.ExnRef)));
        map.put(ValueType.FuncRef, new FunctionType(List.of(), List.of(ValueType.FuncRef)));
        map.put(ValueType.V128, new FunctionType(List.of(), List.of(ValueType.V128)));
        map.put(ValueType.F64, new FunctionType(List.of(), List.of(ValueType.F64)));
        map.put(ValueType.F32, new FunctionType(List.of(), List.of(ValueType.F32)));
        map.put(ValueType.I64, new FunctionType(List.of(), List.of(ValueType.I64)));
        map.put(ValueType.I32, new FunctionType(List.of(), List.of(ValueType.I32)));
        returning = map;
        map = new HashMap<>();
        map.put(ValueType.ExternRef, new FunctionType(List.of(ValueType.ExternRef), List.of()));
        map.put(ValueType.ExnRef, new FunctionType(List.of(ValueType.ExnRef), List.of()));
        map.put(ValueType.FuncRef, new FunctionType(List.of(ValueType.FuncRef), List.of()));
        map.put(ValueType.V128, new FunctionType(List.of(ValueType.V128), List.of()));
        map.put(ValueType.F64, new FunctionType(List.of(ValueType.F64), List.of()));
        map.put(ValueType.F32, new FunctionType(List.of(ValueType.F32), List.of()));
        map.put(ValueType.I64, new FunctionType(List.of(ValueType.I64), List.of()));
        map.put(ValueType.I32, new FunctionType(List.of(ValueType.I32), List.of()));
        accepting = map;
    }

    private static final FunctionType empty = new FunctionType(List.of(), List.of());

    public static FunctionType returning(ValueType valueType) {
        return returning.get(valueType);
    }

    public static FunctionType accepting(ValueType valueType) {
        return accepting.get(valueType);
    }

    public boolean typesMatch(FunctionType other) {
        return paramsMatch(other) && returnsMatch(other);
    }

    public static FunctionType of(List<ValueType> params, List<ValueType> returns) {
        if (params.isEmpty()) {
            if (returns.isEmpty()) {
                return empty;
            }
            if (returns.size() == 1) {
                return returning(returns.get(0));
            }
        } else if (returns.isEmpty()) {
            if (params.size() == 1) {
                return accepting(params.get(0));
            }
        }
        return new FunctionType(List.copyOf(params), List.copyOf(returns));
    }

    public static FunctionType of(ValueType[] params, ValueType[] returns) {
        return of(List.of(params), List.of(returns));
    }

    public static FunctionType empty() {
        return empty;
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();
        builder.append('(');
        var nParams = this.params.size();
        for (var i = 0; i < nParams; i++) {
            builder.append(this.params.get(i).toString());
            if (i < nParams - 1) {
                builder.append(',');
            }
        }
        builder.append(") -> ");
        var nReturns = this.returns.size();
        if (nReturns == 0) {
            builder.append("nil");
        } else {
            builder.append('(');
            for (var i = 0; i < nReturns; i++) {
                builder.append(this.returns.get(i).toString());
                if (i < nReturns - 1) {
                    builder.append(',');
                }
            }
            builder.append(')');
        }
        return builder.toString();
    }
}
