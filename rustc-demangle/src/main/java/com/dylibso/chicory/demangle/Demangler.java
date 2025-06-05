package com.dylibso.chicory.demangle;

import com.dylibso.chicory.annotations.WasmModuleInterface;
import com.dylibso.chicory.runtime.ByteArrayMemory;
import com.dylibso.chicory.runtime.Instance;

@WasmModuleInterface("rustc_demangle.wasm")
public final class Demangler {

    private static final Instance INSTANCE =
            Instance.builder(RustCDemangle.load())
                    .withMemoryFactory(ByteArrayMemory::new)
                    .withMachineFactory(RustCDemangle::create)
                    .build();
    private static final Demangler_ModuleExports exports = new Demangler_ModuleExports(INSTANCE);

    private Demangler() {}

    // Demangles symbol given in `mangled` argument into `out` buffer
    //
    // Returns 0 if `mangled` is not Rust symbol or if `out` buffer is too small
    // Returns 1 otherwise
    // int rustc_demangle(const char *mangled, char *out, size_t out_size);
    private static String rustcDemangle(String mangled) {
        int mangledPtr = 0;
        int mangledSize = mangled.length() + 1;
        int outPtr = 0;
        int outSize = 0;

        try {
            mangledPtr = exports.alloc(mangledSize);
            exports.memory().writeCString(mangledPtr, mangled);

            outSize = exports.rustcDemangleLen(mangledPtr);
            // Not a Rust symbol
            if (outSize == 0) {
                return mangled;
            } else {
                outPtr = exports.alloc(outSize);
                var result = exports.rustcDemangle(mangledPtr, outPtr, outSize);
                if (result == 0) {
                    throw new RuntimeException(
                            "Failed to demangle '"
                                    + mangled
                                    + "' is not Rust symbol or the out buffer is too small");
                } else {
                    return exports.memory().readCString(outPtr);
                }
            }
        } finally {
            if (mangledPtr != 0) {
                exports.dealloc(mangledPtr, mangledSize);
            }
            if (outPtr != 0 && outSize != 0) {
                exports.dealloc(outPtr, outSize);
            }
        }
    }

    public static String demangle(String str) {
        return rustcDemangle(str);
    }
}
