package com.dylibso.chicory.runtime;

//
// This class is used by compiler generated classes. It MUST remain backwards compatible
// so that older generated code can run on newer versions of the library.
//
// This is an ugly hack to work around a bug on some JVMs (Temurin 17-)
public final class MemCopyWorkaround {
    private MemCopyWorkaround() {}

    private interface MemoryCopyFunc {
        void apply(int destination, int offset, int size, Memory memory);
    }

    private interface I32GEUFunc {
        int apply(int a, int b);
    }

    public static boolean shouldUseMemWorkaround() {
        return shouldUseMemWorkaround(System.getProperty("java.version"));
    }

    public static boolean shouldUseMemWorkaround(String version) {
        if (version == null || version.equals("0")) {
            // Android https://developer.android.com/reference/java/lang/System#getProperties()
            return false;
        } else if (version.startsWith("1.")) {
            // Java 8 or earlier: "1.8.0_231" → 8
            return true;
        } else {
            // Java 9 or later: "11.0.9" or "17.0.1"
            int dotIndex = version.indexOf(".");
            String majorStr = (dotIndex != -1) ? version.substring(0, dotIndex) : version;
            int dashIndex = majorStr.indexOf("-");
            majorStr = (dashIndex != -1) ? majorStr.substring(0, dashIndex) : majorStr;
            try {
                return Integer.parseInt(majorStr) <= 17;
            } catch (NumberFormatException nfe) {
                return false;
            }
        }
    }

    static {
        if (shouldUseMemWorkaround()) {
            MemoryCopyFunc noop1 = (destination, offset, size, memory) -> {};
            MemoryCopyFunc noop2 = (destination, offset, size, memory) -> {};

            I32GEUFunc noop3 = (a, b) -> a;
            I32GEUFunc noop4 = (a, b) -> b;

            // Warm up the JIT... to make it see memoryCopyFunc.apply is megamorphic
            for (int i = 0; i < 1000; i++) {
                memoryCopyFunc = noop1;
                MemCopyWorkaround.memoryCopy(0, 0, 0, null);
                memoryCopyFunc = noop2;
                MemCopyWorkaround.memoryCopy(0, 0, 0, null);

                i32geuFunc = noop3;
                MemCopyWorkaround.i32_ge_u(0, 0);
                i32geuFunc = noop4;
                MemCopyWorkaround.i32_ge_u(0, 0);
            }

            memoryCopyFunc =
                    (destination, offset, size, memory) -> memory.copy(destination, offset, size);
            i32geuFunc = (a, b) -> OpcodeImpl.I32_GE_U(a, b);
        }
    }

    static MemoryCopyFunc memoryCopyFunc;
    static I32GEUFunc i32geuFunc;

    public static void memoryCopy(int destination, int offset, int size, Memory memory) {
        memoryCopyFunc.apply(destination, offset, size, memory);
    }

    public static int i32_ge_u(int a, int b) {
        return i32geuFunc.apply(a, b);
    }
}
