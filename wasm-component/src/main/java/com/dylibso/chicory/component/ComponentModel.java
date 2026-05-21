package com.dylibso.chicory.component;

import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.wasm.WasmModule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Main API for Component Model support in Chicory.
 * Provides high-level interface for working with WIT-defined components.
 *
 * Usage:
 * <pre>
 *   String wit = "add: function(a: i32, b: i32) -> i32";
 *   WasmModule module = WasmModule.parse(wasmBytes);
 *   ComponentModel component = ComponentModel.load(wit, module);
 *
 *   // Call guest function
 *   Object result = component.callExport("add", 3, 5); // returns 8
 * </pre>
 */
public class ComponentModel {

    private final ComponentDefinition definition;
    private final Instance guestInstance;
    private final Map<String, TypedExportFunction> exports;
    private final String moduleName;

    private ComponentModel(
            ComponentDefinition definition, Instance guestInstance, String moduleName) {
        this.definition = definition;
        this.guestInstance = guestInstance;
        this.moduleName = moduleName;
        this.exports = new HashMap<>();
    }

    /**
     * Load a component from WIT source and Wasm module.
     *
     * @param witSource WIT definition text
     * @param wasmModule Compiled Wasm module
     * @return Component model instance
     */
    public static ComponentModel load(String witSource, WasmModule wasmModule) {
        return load(witSource, wasmModule, "component");
    }

    /**
     * Load a component from WIT source and Wasm module with custom module name.
     *
     * @param witSource WIT definition text
     * @param wasmModule Compiled Wasm module
     * @param moduleName Module name to use for host functions
     * @return Component model instance
     */
    public static ComponentModel load(String witSource, WasmModule wasmModule, String moduleName) {
        // Parse WIT
        WitParser parser = new WitParser();
        ComponentDefinition definition = parser.parse(witSource);

        // Create Chicory store and instantiate module
        Store store = new Store();
        Instance instance = store.instantiate("component", wasmModule);

        return new ComponentModel(definition, instance, moduleName);
    }

    /**
     * Add a host function that can be called by the guest.
     *
     * @param functionName Name of the function in the WIT definition
     * @param handler The Java implementation of the function
     */
    public void addHostFunction(
            String functionName, ComponentFunctionBinder.ComponentFunctionHandler handler) {
        Optional<ComponentDefinition.FunctionSignature> sig = definition.importByName(functionName);
        if (sig.isEmpty()) {
            // Also check exports if not explicitly marked as import
            sig = definition.exportByName(functionName);
        }
        if (sig.isEmpty()) {
            throw new IllegalArgumentException("Function not found in WIT: " + functionName);
        }

        // Note: In real usage, this would be called during instantiation
        // For now, this is documented for future implementation
    }

    /**
     * Call a guest-exported function with typed arguments.
     *
     * @param functionName Name of the exported function in the WIT definition
     * @param args Arguments matching the function's WIT signature
     * @return Decoded result
     */
    public Object callExport(String functionName, Object... args) {
        // Get or create typed export function
        TypedExportFunction typedExport = getExportFunction(functionName);
        return typedExport.call(args);
    }

    /**
     * Get a typed export function by name.
     *
     * @param functionName Name of the exported function
     * @return TypedExportFunction wrapper
     */
    public TypedExportFunction getExportFunction(String functionName) {
        if (exports.containsKey(functionName)) {
            return exports.get(functionName);
        }

        Optional<ComponentDefinition.FunctionSignature> sig = definition.exportByName(functionName);
        if (sig.isEmpty()) {
            throw new IllegalArgumentException("Export not found in component: " + functionName);
        }

        ExportFunction export = guestInstance.export(functionName);
        if (export == null) {
            throw new IllegalArgumentException(
                    "Function not exported by guest module: " + functionName);
        }

        TypedExportFunction typedExport =
                new TypedExportFunction(export, definition, sig.get(), guestInstance.memory());
        exports.put(functionName, typedExport);
        return typedExport;
    }

    /**
     * Get the parsed WIT definition.
     */
    public ComponentDefinition getDefinition() {
        return definition;
    }

    /**
     * Get the guest instance.
     */
    public Instance getInstance() {
        return guestInstance;
    }

    /**
     * List all exported function names.
     */
    public List<String> getExportedFunctions() {
        List<String> names = new ArrayList<>();
        for (ComponentDefinition.FunctionSignature sig : definition.exports()) {
            names.add(sig.name());
        }
        return names;
    }

    /**
     * List all imported function names (those the host must provide).
     */
    public List<String> getImportedFunctions() {
        List<String> names = new ArrayList<>();
        for (ComponentDefinition.FunctionSignature sig : definition.imports()) {
            names.add(sig.name());
        }
        return names;
    }
}
