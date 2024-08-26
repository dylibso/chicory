package com.dylibso.chicory.aot;

import static java.lang.invoke.MethodHandles.lookup;

import com.dylibso.chicory.wasm.types.Value;
import java.lang.invoke.MethodHandle;

final class ValueWrapper {
    public static final MethodHandle HANDLE;

    static {
        try {
            HANDLE = lookup().unreflect(ValueWrapper.class.getMethod("wrap", Value.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new LinkageError(e.getMessage(), e);
        }
    }

    private ValueWrapper() {}

    public static Value[] wrap(Value value) {
        return new Value[] {value};
    }
}
