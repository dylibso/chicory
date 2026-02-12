package com.dylibso.chicory.runtime;

//
// This class is used by compiler generated classes. It MUST remain backwards compatible
// so that older generated code can run on newer versions of the library.
//
// This is an ugly hack to work around a bug on some JVMs (Temurin 17-)
public final class MemCopyWorkaround {
    private MemCopyWorkaround() {}

    public static boolean shouldUseMemWorkaround() {
        return false;
    }

    public static boolean shouldUseMemWorkaround(String version) {
        return false;
    }

    public static void memoryCopy(int destination, int offset, int size, Memory memory) {
        memory.copy(destination, offset, size);
    }

    public static int i32_ge_u(int a, int b) {
        return OpcodeImpl.I32_GE_U(a, b);
    }
}
