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
    private final HostFunctionProvider hostFunctions;

    private ComponentModel(
            ComponentDefinition definition,
            Instance guestInstance,
            String moduleName,
            HostFunctionProvider hostFunctions) {
        this.definition = definition;
        this.guestInstance = guestInstance;
        this.moduleName = moduleName;
        this.exports = new HashMap<>();
        this.hostFunctions = hostFunctions;
    }

    /**
     * Load a component from WIT source and Wasm module.
     *
     * @param witSource WIT definition text
     * @param wasmModule Compiled Wasm module
     * @return Component model instance
     */
    public static ComponentModel load(String witSource, WasmModule wasmModule) {
        return load(witSource, wasmModule, "component", new HostFunctionProvider());
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
        return load(witSource, wasmModule, moduleName, new HostFunctionProvider());
    }

    /**
     * Load a component from WIT source and Wasm module with host functions.
     *
     * @param witSource WIT definition text
     * @param wasmModule Compiled Wasm module
     * @param hostFunctions Host function provider with implementations
     * @return Component model instance
     */
    public static ComponentModel load(
            String witSource, WasmModule wasmModule, HostFunctionProvider hostFunctions) {
        return load(witSource, wasmModule, "component", hostFunctions);
    }

    /**
     * Load a component from WIT source, Wasm module, module name, and host functions.
     *
     * @param witSource WIT definition text
     * @param wasmModule Compiled Wasm module
     * @param moduleName Module name to use for host functions
     * @param hostFunctions Host function provider with implementations
     * @return Component model instance
     */
    public static ComponentModel load(
            String witSource,
            WasmModule wasmModule,
            String moduleName,
            HostFunctionProvider hostFunctions) {
        // Parse WIT
        WitParser parser = new WitParser();
        ComponentDefinition definition = parser.parse(witSource);

        // Create Chicory store with host function bindings
        Store store = new Store();
        ImportBinder.bindHostFunctions(store, hostFunctions, definition, moduleName);

        // Instantiate module
        Instance instance = store.instantiate("component", wasmModule);

        return new ComponentModel(definition, instance, moduleName, hostFunctions);
    }

    /**
     * Register a host function that can be called by the guest.
     *
     * @param functionName Name of the function in the WIT definition
     * @param handler The Java implementation of the function
     */
    public void registerHostFunction(
            String functionName, HostFunctionProvider.HostFunction handler) {
        hostFunctions.register(functionName, handler);
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
     * Get the host function provider.
     */
    public HostFunctionProvider getHostFunctions() {
        return hostFunctions;
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
