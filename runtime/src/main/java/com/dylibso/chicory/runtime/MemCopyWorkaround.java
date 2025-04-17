package com.dylibso.chicory.runtime;

// This is an ugly hack to workaround a bug on some JVMs (Temurin 17-)
public final class MemCopyWorkaround {
    private MemCopyWorkaround() {}

    private interface Func {
        void apply(int destination, int offset, int size, Memory memory);
    }

    private static final class Actual implements Func {
        @Override
        public void apply(int destination, int offset, int size, Memory memory) {
            memory.copy(destination, offset, size);
        }
    }

    private static final class AddOption implements Func {
        @Override
        public void apply(int destination, int offset, int size, Memory memory) {
            hits += 1;
        }
    }

    private static final class SubtractOption implements Func {
        @Override
        public void apply(int destination, int offset, int size, Memory memory) {
            var x = hits;
            hits = x + 1;
        }
    }

    private static int hits;

    static {
        // Warm up the the JIT... to make it think func is megamorphic
        int loops = 10_000;
        func = new AddOption();
        for (int i = 0; i < loops; i++) {
            MemCopyWorkaround.apply(0, 0, 0, null);
        }
        func = new SubtractOption();
        for (int i = 0; i < loops; i++) {
            MemCopyWorkaround.apply(0, 0, 0, null);
        }
        func = new Actual();
    }

    static Func func;

    public static void apply(int destination, int offset, int size, Memory memory) {
        func.apply(destination, offset, size, memory);
    }
}
