package com.dylibso.chicory.wasm.types;

import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

/**
 * Represents the signature of a WebAssembly function, defining its parameter and return types.
 */
public final class FunctionType {
    private final List<ValueType> params;
    private final List<ValueType> returns;
    private final int hashCode;

    private FunctionType(List<ValueType> params, List<ValueType> returns) {
        this.params = params;
        this.returns = returns;
        hashCode = Objects.hash(params, returns);
    }

    /**
     * Returns the list of parameter types for this function signature.
     *
     * @return an unmodifiable {@link List} of parameter {@link ValueType}s.
     */
    public List<ValueType> params() {
        return params;
    }

    /**
     * Returns the list of return types for this function signature.
     *
     * @return an unmodifiable {@link List} of return {@link ValueType}s.
     */
    public List<ValueType> returns() {
        return returns;
    }

    /**
     * Checks if the parameter types of this signature match another signature.
     *
     * @param other the other {@link FunctionType} to compare against.
     * @return {@code true} if the parameter lists are equal, {@code false} otherwise.
     */
    public boolean paramsMatch(FunctionType other) {
        return params.equals(other.params);
    }

    /**
     * Checks if the return types of this signature match another signature.
     *
     * @param other the other {@link FunctionType} to compare against.
     * @return {@code true} if the return lists are equal, {@code false} otherwise.
     */
    public boolean returnsMatch(FunctionType other) {
        return returns.equals(other.returns);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FunctionType && equals((FunctionType) obj);
    }

    /**
     * Compares this function type to another function type for equality.
     * Equality is based on both parameter and return types.
     *
     * @param other the {@code FunctionType} to compare against.
     * @return {@code true} if the types are equal, {@code false} otherwise.
     */
    public boolean equals(FunctionType other) {
        return hashCode == other.hashCode && paramsMatch(other) && returnsMatch(other);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private static final EnumMap<ValueType, FunctionType> returning;
    private static final EnumMap<ValueType, FunctionType> accepting;

    static {
        EnumMap<ValueType, FunctionType> map = new EnumMap<>(ValueType.class);
        map.put(ValueType.ExternRef, new FunctionType(List.of(), List.of(ValueType.ExternRef)));
        map.put(ValueType.ExnRef, new FunctionType(List.of(), List.of(ValueType.ExnRef)));
        map.put(ValueType.FuncRef, new FunctionType(List.of(), List.of(ValueType.FuncRef)));
        map.put(ValueType.V128, new FunctionType(List.of(), List.of(ValueType.V128)));
        map.put(ValueType.F64, new FunctionType(List.of(), List.of(ValueType.F64)));
        map.put(ValueType.F32, new FunctionType(List.of(), List.of(ValueType.F32)));
        map.put(ValueType.I64, new FunctionType(List.of(), List.of(ValueType.I64)));
        map.put(ValueType.I32, new FunctionType(List.of(), List.of(ValueType.I32)));
        returning = map;
        map = new EnumMap<>(ValueType.class);
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

    /**
     * Returns a cached {@link FunctionType} instance representing a function with no parameters
     * and a single return value of the specified type.
     *
     * @param valueType the single return type.
     * @return a cached {@link FunctionType} instance, or {@code null} if the type is not cached.
     */
    public static FunctionType returning(ValueType valueType) {
        return returning.get(valueType);
    }

    /**
     * Returns a cached {@link FunctionType} instance representing a function with a single parameter
     * of the specified type and no return values.
     *
     * @param valueType the single parameter type.
     * @return a cached {@link FunctionType} instance, or {@code null} if the type is not cached.
     */
    public static FunctionType accepting(ValueType valueType) {
        return accepting.get(valueType);
    }

    /**
     * Checks if this function type signature matches another signature exactly
     * (both parameters and return types).
     *
     * @param other the other {@link FunctionType} to compare against.
     * @return {@code true} if both parameter and return lists are equal, {@code false} otherwise.
     */
    public boolean typesMatch(FunctionType other) {
        return paramsMatch(other) && returnsMatch(other);
    }

    /**
     * Creates or retrieves a {@link FunctionType} instance for the given parameter and return types.
     * Uses cached instances for common simple types (empty, single param, single return).
     *
     * @param params the list of parameter types.
     * @param returns the list of return types.
     * @return a {@link FunctionType} instance representing the signature.
     */
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

    /**
     * Creates or retrieves a {@link FunctionType} instance for the given parameter and return types.
     * Uses cached instances for common simple types (empty, single param, single return).
     *
     * @param params the array of parameter types.
     * @param returns the array of return types.
     * @return a {@link FunctionType} instance representing the signature.
     */
    public static FunctionType of(ValueType[] params, ValueType[] returns) {
        return of(List.of(params), List.of(returns));
    }

    /**
     * Returns the cached {@link FunctionType} instance representing a function with no parameters
     * and no return values.
     *
     * @return the empty {@link FunctionType} instance.
     */
    public static FunctionType empty() {
        return empty;
    }

    /**
     * Returns a string representation of this function type signature.
     *
     * @return a string representation of the function type.
     */
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
