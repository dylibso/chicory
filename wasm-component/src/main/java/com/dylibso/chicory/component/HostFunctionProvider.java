package com.dylibso.chicory.component;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides host-side implementations of functions that the guest (WASM) imports.
 * Maintains a registry of imported function handlers.
 *
 * Example:
 * ```java
 * HostFunctionProvider host = new HostFunctionProvider();
 * host.register("host-log", (Object[] args) -> {
 *     String msg = (String) args[0];
 *     System.out.println(msg);
 *     return null;
 * });
 * ```
 */
public class HostFunctionProvider {

    /**
     * Callback interface for host functions.
     * Receives arguments and returns a result (or null for void functions).
     */
    @FunctionalInterface
    public interface HostFunction {
        /**
         * Execute the host function.
         * @param args Arguments from guest (already decoded from ABI format)
         * @return Result for guest, or null for void functions
         */
        Object call(Object... args);
    }

    private final Map<String, HostFunction> functions = new HashMap<>();

    /**
     * Register a host function implementation.
     * @param name Function name as declared in WIT
     * @param handler The Java implementation
     */
    public void register(String name, HostFunction handler) {
        functions.put(name, handler);
    }

    /**
     * Retrieve a registered host function.
     * @param name Function name as declared in WIT
     * @return The handler, or null if not registered
     */
    public HostFunction get(String name) {
        return functions.get(name);
    }

    /**
     * Check if a function is registered.
     * @param name Function name as declared in WIT
     * @return true if registered, false otherwise
     */
    public boolean has(String name) {
        return functions.containsKey(name);
    }

    /**
     * Call a registered host function.
     * @param name Function name as declared in WIT
     * @param args Arguments to pass
     * @return Result from the function
     * @throws IllegalArgumentException if function not found
     */
    public Object call(String name, Object... args) {
        HostFunction fn = functions.get(name);
        if (fn == null) {
            throw new IllegalArgumentException("Host function not found: " + name);
        }
        return fn.call(args);
    }
}
