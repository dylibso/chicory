package com.dylibso.chicory.aot;

import static java.lang.invoke.MethodHandles.publicLookup;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.types.Value;
import java.lang.invoke.MethodHandle;

public final class HostFunctionInvoker {
    public static final MethodHandle HANDLE;

    static {
        try {
            HANDLE =
                    publicLookup()
                            .unreflect(
                                    HostFunctionInvoker.class.getMethod(
                                            "invoke", Instance.class, int.class, Value[].class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private HostFunctionInvoker() {}

    public static Value[] invoke(Instance instance, int funcId, Value[] args) {
        return instance.callHostFunction(funcId, args);
    }
}
