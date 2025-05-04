package com.dylibso.chicory.wasm.types;

import java.util.List;
import java.util.Objects;

public final class FunctionType {
    private final List<ValType> params;
    private final List<ValType> returns;
    private final int hashCode;

    private FunctionType(List<ValType> params, List<ValType> returns) {
        this.params = params;
        this.returns = returns;
        hashCode = Objects.hash(params, returns);
    }

    public List<ValType> params() {
        return params;
    }

    public List<ValType> returns() {
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

    private static final FunctionType empty = new FunctionType(List.of(), List.of());

    public static FunctionType returning(ValType valType) {
        switch (valType.opcode()) {
            case ValType.ID.ExnRef:
            case ValType.ID.V128:
            case ValType.ID.F64:
            case ValType.ID.F32:
            case ValType.ID.I64:
            case ValType.ID.I32:
                return new FunctionType(List.of(), List.of(valType));
            case ValType.ID.RefNull:
                if (valType.equals(ValType.ExternRef)
                        || valType.equals(ValType.FuncRef)
                        || valType.equals(ValType.ExnRef)) {
                    return new FunctionType(List.of(), List.of(valType));
                }
                // fallthrough
            default:
                throw new IllegalArgumentException("invalid ValType " + valType);
        }
    }

    public static FunctionType accepting(ValType valType) {
        switch (valType.opcode()) {
            case ValType.ID.ExnRef:
            case ValType.ID.V128:
            case ValType.ID.F64:
            case ValType.ID.F32:
            case ValType.ID.I64:
            case ValType.ID.I32:
                return new FunctionType(List.of(valType), List.of());
            case ValType.ID.RefNull:
                if (valType.equals(ValType.ExternRef)
                        || valType.equals(ValType.FuncRef)
                        || valType.equals(ValType.ExnRef)) {
                    return new FunctionType(List.of(valType), List.of());
                }
                // fallthrough
            default:
                throw new IllegalArgumentException("invalid ValType " + valType);
        }
    }

    public boolean typesMatch(FunctionType other) {
        return paramsMatch(other) && returnsMatch(other);
    }

    public static FunctionType of(List<ValType> params, List<ValType> returns) {
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

    public static FunctionType of(ValType[] params, ValType[] returns) {
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
