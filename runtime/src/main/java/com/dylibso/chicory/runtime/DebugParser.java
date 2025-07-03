package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.WasmModule;
import java.util.function.Function;

public interface DebugParser extends Function<WasmModule, Stratum> {}
