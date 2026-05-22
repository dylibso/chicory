package com.dylibso.chicory.component;

import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.WitType;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Binds host functions (from HostFunctionProvider) to a Chicory Store for import resolution.
 * Converts between WIT type definitions and Chicory's FunctionType representation.
 *
 * Key insights:
 * - Strings in canonical ABI are (ptr, len) pairs - two i32 values
 * - wit-bindgen uses "sret" (stack return) convention: func() -> string becomes func(out_ptr: i32)
 * - When a function has a string return, the lowered form has no return but takes an output pointer
 * - String parameters passed directly as (ptr, len) pairs (different from export encoding!)
 */
public class ImportBinder {

    /**
     * Bind host functions from a provider to a Chicory store for a specific module.
     *
     * @param store Chicory store to add imports to
     * @param hostProvider Host function provider with implementations
     * @param definition Component definition with function signatures
     * @param moduleName Module name to register imports under (typically "env" or component name)
     */
    public static void bindHostFunctions(
            Store store,
            HostFunctionProvider hostProvider,
            ComponentDefinition definition,
            String moduleName) {
        // Bind each imported function
        for (ComponentDefinition.FunctionSignature sig : definition.imports()) {
            String funcName = sig.name();

            // Check if host has an implementation
            if (!hostProvider.has(funcName)) {
                // Optional: silently skip unimplemented imports
                // Required imports will fail at call time with proper error
                continue;
            }

            // Convert WIT signature to Chicory FunctionType with wit-bindgen ABI lowering
            FunctionType chicoryType = witSignatureToFunctionType(sig);

            // Create host function wrapper
            HostFunctionProvider.HostFunction hostFunc = hostProvider.get(funcName);
            HostFunction hostImport =
                    createHostFunction(moduleName, funcName, chicoryType, hostFunc, sig);

            // Add to store
            store.addFunction(hostImport);
        }
    }

    /**
     * Convert a WIT function signature to Chicory's FunctionType with wit-bindgen ABI lowering.
     *
     * Key transformation:
     * - If function returns a string, use sret convention: add output pointer parameter, no return
     * - Otherwise, expand string parameters to (ptr, len)
     *
     * @param sig WIT function signature
     * @return Chicory FunctionType with ABI lowering applied
     */
    private static FunctionType witSignatureToFunctionType(
            ComponentDefinition.FunctionSignature sig) {
        List<ValType> paramTypes = new ArrayList<>();
        List<ValType> returnTypes = new ArrayList<>();

        // Check if this function returns a string (for sret lowering)
        boolean returnsString =
                sig.returns().size() == 1
                        && sig.returns().get(0) instanceof PrimitiveType
                        && ((PrimitiveType) sig.returns().get(0)) == PrimitiveType.STRING;

        // Convert parameter types (expand strings to ptr, len)
        for (ComponentDefinition.FunctionSignature.Parameter param : sig.parameters()) {
            expandWitType(param.type, paramTypes);
        }

        // If returns string, add output pointer parameter (sret convention)
        if (returnsString) {
            paramTypes.add(ValType.I32); // output pointer for (ptr, len) pair
            // No return types for sret functions
        } else {
            // Convert return types (expand strings to ptr, len)
            for (WitType returnType : sig.returns()) {
                expandWitType(returnType, returnTypes);
            }
        }

        return FunctionType.of(paramTypes, returnTypes);
    }

    /**
     * Expand a WIT type into ValType list, handling strings as (ptr, len) pairs.
     *
     * @param witType WIT type to expand
     * @param valTypes List to append expanded types to
     */
    private static void expandWitType(WitType witType, List<ValType> valTypes) {
        if (witType instanceof PrimitiveType) {
            PrimitiveType prim = (PrimitiveType) witType;
            switch (prim) {
                case I32:
                    valTypes.add(ValType.I32);
                    break;
                case I64:
                    valTypes.add(ValType.I64);
                    break;
                case F32:
                    valTypes.add(ValType.F32);
                    break;
                case F64:
                    valTypes.add(ValType.F64);
                    break;
                case BOOL:
                    valTypes.add(ValType.I32); // bool is i32 in WASM
                    break;
                case CHAR:
                    valTypes.add(ValType.I32); // char is i32 in WASM
                    break;
                case STRING:
                    // String parameter is (ptr, len) - TWO i32 values
                    valTypes.add(ValType.I32); // ptr
                    valTypes.add(ValType.I32); // len
                    break;
                default:
                    throw new IllegalArgumentException("Unknown primitive type: " + prim);
            }
        } else {
            throw new IllegalArgumentException(
                    "Complex types not yet supported in imports: " + witType.displayName());
        }
    }

    /**
     * Create a HostFunction wrapper that adapts a Java HostFunctionProvider.HostFunction to
     * Chicory's WasmFunctionHandle interface.
     *
     * Decodes string parameters from guest memory and re-encodes string returns.
     */
    private static HostFunction createHostFunction(
            String moduleName,
            String funcName,
            FunctionType type,
            HostFunctionProvider.HostFunction javaFunc,
            ComponentDefinition.FunctionSignature witSig) {

        // Wrap the Java function as a WASM function handle
        return new HostFunction(
                moduleName,
                funcName,
                type,
                (instance, args) -> {
                    Memory memory = instance.memory();

                    // Decode WASM arguments to Java objects
                    // IMPORTANT: For imports, string parameters are passed directly as (ptr, len),
                    // not as a pointer to a structure (that's only for exports)
                    Object[] javaArgs = new Object[witSig.parameters().size()];
                    int argIndex = 0;
                    int paramIndex = 0;

                    for (ComponentDefinition.FunctionSignature.Parameter param :
                            witSig.parameters()) {
                        if (param.type instanceof PrimitiveType) {
                            PrimitiveType prim = (PrimitiveType) param.type;
                            if (prim == PrimitiveType.STRING) {
                                // Decode string from (ptr, len) pair passed directly
                                long ptr = args[argIndex];
                                long len = args[argIndex + 1];

                                // Read string directly from memory using ptr and len
                                String str =
                                        memory.readString(
                                                (int) ptr, (int) len, StandardCharsets.UTF_8);
                                javaArgs[paramIndex] = str;
                                argIndex += 2; // consumed 2 args
                                paramIndex += 1;
                            } else if (prim == PrimitiveType.BOOL) {
                                // bool: i32 -> Boolean
                                javaArgs[paramIndex] = (args[argIndex] != 0);
                                argIndex += 1;
                                paramIndex += 1;
                            } else {
                                // Primitive: i32, i64, f32, f64
                                javaArgs[paramIndex] = args[argIndex];
                                argIndex += 1;
                                paramIndex += 1;
                            }
                        }
                    }

                    // Call Java function
                    Object result = javaFunc.call(javaArgs);

                    // Check if this is an sret function (returns string via output pointer)
                    boolean returnsString =
                            witSig.returns().size() == 1
                                    && witSig.returns().get(0) instanceof PrimitiveType
                                    && ((PrimitiveType) witSig.returns().get(0))
                                            == PrimitiveType.STRING;

                    if (returnsString) {
                        // For sret: encode the string at the output pointer
                        long outPtr = args[args.length - 1]; // last parameter is output pointer
                        if (result instanceof String) {
                            String str = (String) result;
                            // TODO: Implement string encoding and write to outPtr
                            // For now, return empty to indicate success
                        }
                        return new long[] {};
                    }

                    // Convert result back to WASM format (for non-sret returns)
                    if (result == null) {
                        return new long[] {};
                    }

                    if (result instanceof Boolean) {
                        return new long[] {(Boolean) result ? 1 : 0};
                    }

                    if (result instanceof Number) {
                        return new long[] {((Number) result).longValue()};
                    }

                    if (result instanceof String) {
                        // Non-sret string return (shouldn't happen for imports, but handle it)
                        return new long[] {0, 0}; // placeholder
                    }

                    return new long[] {};
                });
    }
}
