package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.Value;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Represents a Java function that can be called from Wasm.
 */
@FunctionalInterface
public interface WasmFunctionHandle {
    Value[] apply(Instance instance, Value... args);

    /**
     * Create a {@code WasmFunctionHandle} which calls the given method handle with possible argument and return
     * value type conversions.
     *
     * @param mh the method handle (must not be {@code null})
     * @return the wrapper function handle (not {@code null})
     * @throws IllegalArgumentException if the method handle cannot be converted to a {@code WasmFunctionHandle}
     */
    static WasmFunctionHandle forMethodHandle(MethodHandle mh) {
        return WasmFunctionHandles.wasmFunctionHandle(mh);
    }

    /**
     * Get a method handle for this {@code WasmFunctionHandle} with the given type, converting arguments and return
     * types as necessary.
     *
     * @param type the type of the desired method handle (must not be {@code null})
     * @return the method handle (not {@code null})
     * @throws IllegalArgumentException if this {@code WasmFunctionHandle} cannot be converted to a method handle
     *  of the given type
     */
    default MethodHandle asMethodHandle(MethodType type) {
        return WasmFunctionHandles.methodHandle(this, type);
    }

    /**
     * Get a {@code WasmFunctionHandle} as a functional interface. Calling the functional interface method will
     * call this {@code WasmFunctionHandle}.
     * <p>
     * The functional interface method must accept a {@link Instance} as its first argument.
     *
     * @param interfaceType the interface type (must not be {@code null})
     * @param methodType the functional interface method (must not be {@code null}) (TODO: we could maybe detect it)
     * @return the functional interface instance
     * @param <T> the functional interface type
     * @throws IllegalArgumentException if this {@code WasmFunctionHandle} cannot be converted to a method handle
     *  of the given type
     */
    default <T> T asFunctionalInterface(Class<T> interfaceType, MethodType methodType) {
        return MethodHandleProxies.asInterfaceInstance(interfaceType, asMethodHandle(methodType));
    }

    static void main(String[] args) throws Throwable {
        WasmFunctionHandle addUp =
                WasmFunctionHandle.forMethodHandle(
                        MethodHandles.lookup()
                                .findStatic(
                                        WasmFunctionHandle.class,
                                        "addUp",
                                        MethodType.methodType(int.class, int.class, int.class)));
        MethodHandle mh =
                addUp.asMethodHandle(
                        MethodType.methodType(int.class, Instance.class, int.class, int.class));
        int res = (int) mh.invokeExact((Instance) null, 12, 50);
        System.out.println(res);
    }

    static int addUp(int a, int b) {
        System.out.println("I'm being called from WASM");
        return a + b;
    }
}
