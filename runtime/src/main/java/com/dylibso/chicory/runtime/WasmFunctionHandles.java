package com.dylibso.chicory.runtime;

import static java.lang.invoke.MethodHandles.lookup;

import com.dylibso.chicory.wasm.types.Value;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation helpers for {@link WasmFunctionHandle}.
 */
final class WasmFunctionHandles {
    private WasmFunctionHandles() {}

    static WasmFunctionHandle wasmFunctionHandle(MethodHandle mh) {
        Objects.requireNonNull(mh, "mh");
        MethodType type = mh.type();
        // the first arg may or may not be an `Instance`
        int cnt = type.parameterCount();
        if (cnt > 0) {
            Class<?> argType = type.parameterType(0);
            if (argType != Instance.class) {
                // drop the instance argument
                mh = MethodHandles.dropArguments(mh, 0, Instance.class);
                type = mh.type();
                cnt = type.parameterCount();
            }
            // apply transformations to each argument
            MethodHandle[] filters = new MethodHandle[cnt];
            for (int i = 1; i < cnt; i++) {
                // add a mapping for each argument
                argType = type.parameterType(i);
                if (argType == Value.class) {
                    // no conversion; pass through
                } else if (argType.isPrimitive()) {
                    // wrap primitives
                    // todo: replace with switch (22+)
                    MethodHandle filter;
                    filter = unboxingFilter(argType);
                    filters[i] = filter;
                } else {
                    // todo: funcref, externalref, vector boxes
                    throw new IllegalArgumentException("Unsupported argument type " + argType);
                }
            }
            mh = MethodHandles.filterArguments(mh, 0, filters);

            // and last, collect the boxed arguments
            mh = mh.asSpreader(1, Value[].class, cnt - 1);
        }
        // adapt the return type
        Class<?> rt = type.returnType();
        if (rt != Value[].class) {
            // some other conversion
            // todo: replace with switch (22+)
            MethodHandle filter = boxingFilter(rt);
            rt = filter.type().returnType();

            mh = MethodHandles.filterReturnValue(mh, filter);
            if (rt == Value.class) {
                // wrap in single-element array
                mh = MethodHandles.filterReturnValue(mh, WRAP_VALUE_WITH_ARRAY);
            }
        }
        // note: this is slow-ish older JDKs - see https://bugs.openjdk.org/browse/JDK-6983726
        return MethodHandleProxies.asInterfaceInstance(WasmFunctionHandle.class, mh);
    }

    static MethodHandle methodHandle(final WasmFunctionHandle fh, final MethodType type) {
        // validate the type
        int cnt = type.parameterCount();
        if (cnt < 1 || type.parameterType(0) != Instance.class) {
            throw new IllegalArgumentException(
                    "The given method type must specify `Instance` as its first parameter type");
        }
        MethodHandle mh = WASM_FUNCTION_HANDLE_APPLY;

        // pass in the instance as the receiver
        mh = mh.bindTo(fh);

        // box the return value
        Class<?> retType = type.returnType();
        if (retType != void.class) {
            if (retType.isArray()) {
                // todo
                if (retType != Value[].class) {
                    throw new IllegalStateException("Arrays are not supported yet");
                }
            } else {
                mh =
                        MethodHandles.filterReturnValue(
                                mh,
                                MethodHandles.insertArguments(
                                        MethodHandles.arrayElementGetter(Value[].class),
                                        1,
                                        Integer.valueOf(0)));
                mh = MethodHandles.filterReturnValue(mh, unboxingFilter(retType));
            }
        }

        // split the arguments
        mh = mh.asCollector(1, Value[].class, cnt - 1);

        // unbox each argument
        MethodHandle[] filters = new MethodHandle[cnt];

        for (int i = 1; i < cnt; i++) {
            filters[i] = boxingFilter(type.parameterType(i));
        }

        mh = MethodHandles.filterArguments(mh, 0, filters);

        return mh.asType(type);
    }

    private static MethodHandle unboxingFilter(final Class<?> toType) {
        MethodHandle filter = UNBOXING_FILTERS.get(toType);
        if (filter == null) {
            // todo: char, boolean
            throw new IllegalArgumentException("Unsupported unboxing of type " + toType);
        }
        return filter;
    }

    private static MethodHandle boxingFilter(final Class<?> fromType) {
        MethodHandle filter = BOXING_FILTERS.get(fromType);
        if (filter == null) {
            // todo: primitive arrays, vectors, refs, etc.
            throw new IllegalArgumentException("Unsupported boxing of type " + fromType);
        }
        return filter;
    }

    static final MethodHandle VALUE_I32;
    static final MethodHandle VALUE_I64;
    static final MethodHandle VALUE_FROM_FLOAT;
    static final MethodHandle VALUE_FROM_DOUBLE;
    static final MethodHandle VALUE_FROM_BOOLEAN;

    static final MethodHandle VALUE_AS_INT;
    static final MethodHandle VALUE_AS_LONG;
    static final MethodHandle VALUE_AS_BYTE;
    static final MethodHandle VALUE_AS_SHORT;
    static final MethodHandle VALUE_AS_FLOAT;
    static final MethodHandle VALUE_AS_DOUBLE;

    static final MethodHandle DROP_RET;
    static final MethodHandle WRAP_VALUE_WITH_ARRAY;

    static final MethodHandle WASM_FUNCTION_HANDLE_APPLY;

    static final MethodHandle VALUE_IDENTITY = MethodHandles.identity(Value.class);

    static Value[] wrapValueWithArray(Value value) {
        return new Value[] {value};
    }

    static {
        MethodHandles.Lookup lookup = lookup();
        try {
            VALUE_I32 =
                    lookup.findStatic(
                            Value.class, "i32", MethodType.methodType(Value.class, long.class));
            VALUE_I64 =
                    lookup.findStatic(
                            Value.class, "i64", MethodType.methodType(Value.class, long.class));
            VALUE_FROM_FLOAT =
                    lookup.findStatic(
                            Value.class,
                            "fromFloat",
                            MethodType.methodType(Value.class, float.class));
            VALUE_FROM_DOUBLE =
                    lookup.findStatic(
                            Value.class,
                            "fromDouble",
                            MethodType.methodType(Value.class, double.class));
            VALUE_FROM_BOOLEAN =
                    lookup.findStatic(
                            Value.class,
                            "fromBoolean",
                            MethodType.methodType(Value.class, boolean.class));

            VALUE_AS_INT =
                    lookup.findVirtual(Value.class, "asInt", MethodType.methodType(int.class));
            VALUE_AS_LONG =
                    lookup.findVirtual(Value.class, "asLong", MethodType.methodType(long.class));
            VALUE_AS_BYTE =
                    lookup.findVirtual(Value.class, "asByte", MethodType.methodType(byte.class));
            VALUE_AS_SHORT =
                    lookup.findVirtual(Value.class, "asShort", MethodType.methodType(short.class));
            VALUE_AS_FLOAT =
                    lookup.findVirtual(Value.class, "asFloat", MethodType.methodType(float.class));
            VALUE_AS_DOUBLE =
                    lookup.findVirtual(
                            Value.class, "asDouble", MethodType.methodType(double.class));

            DROP_RET = lookup.findStaticGetter(Value.class, "EMPTY_VALUES", Value[].class);
            WRAP_VALUE_WITH_ARRAY =
                    lookup.findStatic(
                            WasmFunctionHandles.class,
                            "wrapValueWithArray",
                            MethodType.methodType(Value[].class, Value.class));

            WASM_FUNCTION_HANDLE_APPLY =
                    lookup.findVirtual(
                            WasmFunctionHandle.class,
                            "apply",
                            MethodType.methodType(Value[].class, Instance.class, Value[].class));
        } catch (NoSuchFieldException e) {
            throw new NoSuchFieldError(e.getMessage());
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    private static final Map<Class<?>, MethodHandle> BOXING_FILTERS =
            Map.of(
                    void.class, DROP_RET,
                    Value.class, VALUE_IDENTITY,
                    int.class, VALUE_I32.asType(MethodType.methodType(Value.class, int.class)),
                    short.class, VALUE_I32.asType(MethodType.methodType(Value.class, short.class)),
                    byte.class, VALUE_I32.asType(MethodType.methodType(Value.class, byte.class)),
                    char.class, VALUE_I32.asType(MethodType.methodType(Value.class, char.class)),
                    long.class, VALUE_I64,
                    float.class, VALUE_FROM_FLOAT,
                    double.class, VALUE_FROM_DOUBLE,
                    boolean.class, VALUE_FROM_BOOLEAN);

    private static final Map<Class<?>, MethodHandle> UNBOXING_FILTERS =
            Map.of(
                    Value.class, VALUE_IDENTITY,
                    int.class, VALUE_AS_INT,
                    short.class, VALUE_AS_SHORT,
                    byte.class, VALUE_AS_BYTE,
                    long.class, VALUE_AS_LONG,
                    float.class, VALUE_AS_FLOAT,
                    double.class, VALUE_AS_DOUBLE);
}
