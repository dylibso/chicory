package com.dylibso.chicory.wabt;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Machine;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.Parser;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public final class Wat2WasmModule {

    private Wat2WasmModule() {}

    public static Machine create(Instance instance) {
        return new Wat2WasmMachine(instance);
    }

    private static class WasmModuleHolder {

        static final WasmModule INSTANCE;

        static {
            try (InputStream in =
                    Wat2WasmModule.class.getResourceAsStream("Wat2WasmModule.meta")) {
                INSTANCE = Parser.parse(in);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to load .meta WASM module", e);
            }
        }
    }

    public static WasmModule load() {
        return WasmModuleHolder.INSTANCE;
    }
}
