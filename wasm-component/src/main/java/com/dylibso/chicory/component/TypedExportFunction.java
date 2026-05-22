package com.dylibso.chicory.component;

import com.dylibso.chicory.component.types.WitType;
import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Memory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Typed wrapper around guest export function.
 * Provides type-safe interface for calling Wasm functions from Java,
 * handling Canonical ABI encoding/decoding automatically.
 */
public class TypedExportFunction {
    private final ExportFunction exportFunction;
    private final ComponentDefinition definition;
    private final ComponentDefinition.FunctionSignature signature;
    private final Memory memory;

    public TypedExportFunction(
            ExportFunction exportFunction,
            ComponentDefinition definition,
            ComponentDefinition.FunctionSignature signature,
            Memory memory) {
        this.exportFunction = exportFunction;
        this.definition = definition;
        this.signature = signature;
        this.memory = memory;
    }

    /**
     * Call the guest function with typed arguments.
     *
     * @param args Java objects matching the function's WIT signature
     * @return Decoded result from the guest function
     * @throws IllegalArgumentException if arguments don't match signature
     */
    public Object call(Object... args) {
        // Validate argument count
        List<ComponentDefinition.FunctionSignature.Parameter> params = signature.parameters();
        if (args.length != params.size()) {
            throw new IllegalArgumentException(
                    String.format("Expected %d arguments, got %d", params.size(), args.length));
        }

        // Encode arguments according to Canonical ABI
        List<Long> wasmArgsList = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            WitType paramType = params.get(i).type;
            long[] encoded = CanonicalAbi.encode(args[i], paramType, memory);

            // Add all encoded values (strings use 2 values: ptr, len)
            for (long value : encoded) {
                wasmArgsList.add(value);
            }
        }

        // Convert to array
        long[] wasmArgs = new long[wasmArgsList.size()];
        for (int i = 0; i < wasmArgsList.size(); i++) {
            wasmArgs[i] = wasmArgsList.get(i);
        }

        // Call the export function
        long[] results = exportFunction.apply(wasmArgs);

        // Decode and return the result
        List<WitType> returnTypes = signature.returns();
        if (returnTypes.isEmpty()) {
            return null;
        }

        WitType returnType = returnTypes.get(0);

        // For strings and complex types, results may contain multiple values
        return CanonicalAbi.decode(
                Arrays.copyOf(results, Math.max(1, results.length)), returnType, memory);
    }

    /**
     * Get the underlying export function.
     */
    public ExportFunction getExportFunction() {
        return exportFunction;
    }

    /**
     * Get the function signature.
     */
    public ComponentDefinition.FunctionSignature getSignature() {
        return signature;
    }
}
