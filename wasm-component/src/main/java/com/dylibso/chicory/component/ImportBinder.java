package com.dylibso.chicory.component;

import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.WitType;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import java.util.ArrayList;
import java.util.List;

/**
 * Binds host functions (from HostFunctionProvider) to a Chicory Store for import resolution.
 * Converts between WIT type definitions and Chicory's FunctionType representation.
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

            // Convert WIT signature to Chicory FunctionType
            FunctionType chicoryType = witSignatureToFunctionType(sig);

            // Create host function wrapper
            HostFunctionProvider.HostFunction hostFunc = hostProvider.get(funcName);
            HostFunction hostImport =
                    createHostFunction(moduleName, funcName, chicoryType, hostFunc);

            // Add to store
            store.addFunction(hostImport);
        }
    }

    /**
     * Convert a WIT function signature to Chicory's FunctionType.
     *
     * @param sig WIT function signature
     * @return Chicory FunctionType with parameter and return types
     */
    private static FunctionType witSignatureToFunctionType(
            ComponentDefinition.FunctionSignature sig) {
        List<ValType> paramTypes = new ArrayList<>();
        List<ValType> returnTypes = new ArrayList<>();

        // Convert parameter types
        for (ComponentDefinition.FunctionSignature.Parameter param : sig.parameters()) {
            paramTypes.add(witTypeToValType(param.type));
        }

        // Convert return types
        for (WitType returnType : sig.returns()) {
            returnTypes.add(witTypeToValType(returnType));
        }

        return FunctionType.of(paramTypes, returnTypes);
    }

    /**
     * Convert a WIT type to Chicory's ValType.
     *
     * @param witType WIT type to convert
     * @return Corresponding ValType
     */
    private static ValType witTypeToValType(WitType witType) {
        if (witType instanceof PrimitiveType) {
            PrimitiveType prim = (PrimitiveType) witType;
            switch (prim) {
                case I32:
                    return ValType.I32;
                case I64:
                    return ValType.I64;
                case F32:
                    return ValType.F32;
                case F64:
                    return ValType.F64;
                case BOOL:
                    return ValType.I32; // bool is i32 in WASM
                case CHAR:
                    return ValType.I32; // char is i32 in WASM
                case STRING:
                    // String is (ptr, len) - two i32 values
                    // This is handled specially below
                    return ValType.I32;
                default:
                    throw new IllegalArgumentException("Unknown primitive type: " + prim);
            }
        }
        throw new IllegalArgumentException(
                "Complex types not yet supported in imports: " + witType.displayName());
    }

    /**
     * Create a HostFunction wrapper that adapts a Java HostFunctionProvider.HostFunction to
     * Chicory's WasmFunctionHandle interface.
     */
    private static HostFunction createHostFunction(
            String moduleName,
            String funcName,
            FunctionType type,
            HostFunctionProvider.HostFunction javaFunc) {

        // Wrap the Java function as a WASM function handle
        return new HostFunction(
                moduleName,
                funcName,
                type,
                (instance, args) -> {
                    // Convert WASM arguments to Java objects
                    Object[] javaArgs = new Object[args.length];
                    for (int i = 0; i < args.length; i++) {
                        javaArgs[i] = args[i];
                    }

                    // Call Java function
                    Object result = javaFunc.call(javaArgs);

                    // Convert result back to WASM format
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
                        // Strings require special handling via CanonicalAbi
                        // For now, just store the string as a pointer
                        // This will be enhanced later
                        return new long[] {0};
                    }

                    return new long[] {};
                });
    }
}
