package com.dylibso.chicory.runtime;

// This is an ugly hack to work around a bug on some JVMs (Temurin 17-)
public final class MemCopyWorkaround {
    private MemCopyWorkaround() {}

    private interface MemoryCopyFunc {
        void apply(int destination, int offset, int size, Memory memory);
    }

    static {
        String workaround = System.getProperty("chicory.enableMemCopyWorkaround");

        boolean enableMemCopyWorkaround;
        if (workaround != null) {
            enableMemCopyWorkaround = Boolean.parseBoolean(workaround);
        } else {
            enableMemCopyWorkaround = Runtime.version().feature() < 21;
        }

        if (enableMemCopyWorkaround) {
            MemoryCopyFunc noop1 = (destination, offset, size, memory) -> {};
            MemoryCopyFunc noop2 = (destination, offset, size, memory) -> {};
            // Warm up the JIT... to make it see memoryCopyFunc.apply is megamorphic
            for (int i = 0; i < 5000; i++) {
                memoryCopyFunc = noop1;
                MemCopyWorkaround.memoryCopy(0, 0, 0, null);
                memoryCopyFunc = noop2;
                MemCopyWorkaround.memoryCopy(0, 0, 0, null);
            }
        }
        memoryCopyFunc =
                (destination, offset, size, memory) -> memory.copy(destination, offset, size);
    }

    static MemoryCopyFunc memoryCopyFunc;

    public static void memoryCopy(int destination, int offset, int size, Memory memory) {
        memoryCopyFunc.apply(destination, offset, size, memory);
    }
}
