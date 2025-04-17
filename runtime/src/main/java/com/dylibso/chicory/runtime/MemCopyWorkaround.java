package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.ChicoryException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

// This is an ugly hack to workaround a bug on some JVMs (Temurin 17-)
public class MemCopyWorkaround {
    public void apply(int destination, int offset, int size, Memory memory) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle =
                    lookup.findVirtual(
                            MemCopyWorkaround.class, "next", MethodType.methodType(void.class));
            handle.invoke(this, destination, offset, size, memory); // Indirect invocation
        } catch (Throwable e) {
            throw new ChicoryException("Failed to invoke method", e);
        }
    }

    public static void next(int destination, int offset, int size, Memory memory) {
        memory.copy(destination, offset, size);
    }
}
