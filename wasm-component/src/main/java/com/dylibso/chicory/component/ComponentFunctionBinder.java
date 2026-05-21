package com.dylibso.chicory.component;

import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.WitType;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import java.util.ArrayList;
import java.util.List;

/**
 * Binds WIT function signatures to HostFunction implementations.
 * Handles argument decoding and result encoding for the Canonical ABI.
 */
public class ComponentFunctionBinder {

    /**
     * Create a HostFunction from a WIT function signature and handler.
     *
     * @param definition Component definition containing type information
     * @param signature Function signature to bind
     * @param handler The Java method to call (will be invoked with decoded arguments)
     * @param moduleName Module name for the HostFunction
     * @return HostFunction ready to be passed to Chicory
     */
    public static HostFunction bindFunction(
            ComponentDefinition definition,
            ComponentDefinition.FunctionSignature signature,
            ComponentFunctionHandler handler,
            String moduleName) {

        // Build function type for Chicory runtime
        List<ValType> paramTypes = new ArrayList<>();
        for (ComponentDefinition.FunctionSignature.Parameter param : signature.parameters()) {
            paramTypes.add(witTypeToValType(param.type));
        }

        List<ValType> returnTypes = new ArrayList<>();
        for (WitType returnType : signature.returns()) {
            returnTypes.add(witTypeToValType(returnType));
        }

        FunctionType functionType = FunctionType.of(paramTypes, returnTypes);

        // Create the WasmFunctionHandle that decodes args and encodes result
        return new HostFunction(
                moduleName,
                signature.name(),
                functionType,
                (Instance instance, long... args) -> {
                    Memory memory = instance.memory();

                    // Decode arguments
                    Object[] decodedArgs = new Object[signature.parameters().size()];
                    int argIndex = 0;
                    for (ComponentDefinition.FunctionSignature.Parameter param :
                            signature.parameters()) {
                        long encodedArg = args[argIndex];
                        decodedArgs[argIndex] =
                                CanonicalAbi.decode(new long[] {encodedArg}, param.type, memory);
                        argIndex++;
                    }

                    // Call the handler with decoded arguments
                    Object result = handler.invoke(decodedArgs);

                    // Encode result
                    if (signature.returns().isEmpty()) {
                        return new long[0];
                    }

                    // For single return value, encode and return
                    WitType returnType = signature.returns().get(0);
                    long[] encoded = CanonicalAbi.encode(result, returnType, memory);
                    return encoded;
                });
    }

    /**
     * Convert a WIT type to Chicory's ValType for function signatures.
     */
    private static ValType witTypeToValType(WitType witType) {
        if (!(witType instanceof PrimitiveType)) {
            // For now, non-primitives are encoded as i64 (ptr/len pairs)
            return ValType.I64;
        }

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
                return ValType.I32; // bool encoded as i32 (0/1)
            case CHAR:
                return ValType.I32; // char encoded as i32
            case STRING:
                return ValType.I64; // string encoded as i64 (ptr,len pair)
            default:
                throw new IllegalArgumentException("Unknown primitive: " + prim);
        }
    }

    /**
     * Handler interface for bound component functions.
     * Receives decoded arguments and returns a value to be encoded.
     */
    @FunctionalInterface
    public interface ComponentFunctionHandler {
        Object invoke(Object[] args);
    }
}
