package com.dylibso.chicory.source.compiler.internal;

import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;

/**
 * Minimal compiler utilities needed by the source compiler.
 *
 * <p>This intentionally omits all ASM-related helpers from the original bytecode compiler and keeps
 * only what is required for the Java source generator (e.g., computing local types).
 */
final class CompilerUtil {

    private CompilerUtil() {}

    /**
     * Return the WASM type of a local (parameter or function-local), matching the original
     * compiler's behaviour.
     */
    public static ValType localType(FunctionType type, FunctionBody body, int localIndex) {
        if (localIndex < type.params().size()) {
            return type.params().get(localIndex);
        } else {
            return body.localTypes().get(localIndex - type.params().size());
        }
    }
}
