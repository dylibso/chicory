package com.dylibso.chicory.aot;

import static java.lang.invoke.MethodHandles.lookup;

import java.lang.invoke.MethodHandle;

final class LongArrayWrapper {
    public static final MethodHandle HANDLE;

    static {
        try {
            HANDLE = lookup().unreflect(LongArrayWrapper.class.getMethod("wrap", long.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new LinkageError(e.getMessage(), e);
        }
    }

    private LongArrayWrapper() {}

    public static long[] wrap(long value) {
        return new long[] {value};
    }
}
