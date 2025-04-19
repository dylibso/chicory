package com.dylibso.chicory.runtime;

// This is an ugly hack to work around a bug on some JVMs (Temurin 17-)
public final class MemCopyWorkaround {
    private MemCopyWorkaround() {}

    private interface MemoryCopyFunc {
        void apply(int destination, int offset, int size, Memory memory);
    }

    private static final class Actual implements MemoryCopyFunc {
        @Override
        public void apply(int destination, int offset, int size, Memory memory) {
            memory.copy(destination, offset, size);
        }
    }

    private static final class Noop1 implements MemoryCopyFunc {
        @Override
        public void apply(int destination, int offset, int size, Memory memory) {}
    }

    private static final class Noop2 implements MemoryCopyFunc {
        @Override
        public void apply(int destination, int offset, int size, Memory memory) {}
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
            Noop1 noop1 = new Noop1();
            Noop2 noop2 = new Noop2();
            // Warm up the JIT... to make it see memoryCopyFunc.apply is megamorphic
            for (int i = 0; i < 1000; i++) {
                memoryCopyFunc = noop1;
                MemCopyWorkaround.memoryCopy(0, 0, 0, null);
                memoryCopyFunc = noop2;
                MemCopyWorkaround.memoryCopy(0, 0, 0, null);
            }
        }
        memoryCopyFunc = new Actual();
    }

    static MemoryCopyFunc memoryCopyFunc;

    public static void memoryCopy(int destination, int offset, int size, Memory memory) {
        memoryCopyFunc.apply(destination, offset, size, memory);
    }
}
