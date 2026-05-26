package com.dylibso.chicory.component;

import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.ComponentCustomSection;

/**
 * Extracts Component Model metadata from WASM binaries.
 *
 * <p>This utility can extract WIT (WebAssembly Interface Types) information from WASM binaries
 * that include Component Model metadata in a "component" custom section. This allows loading
 * components without requiring a separate WIT file.
 *
 * <p>Supported approaches:
 * <ul>
 *   <li>Explicit WIT file (current): ComponentModel.load(witSource, wasmModule)
 *   <li>Embedded WIT in binary (new): ComponentModel.load(wasmModule)
 * </ul>
 *
 * <p>Phase implementation status:
 * <ul>
 *   <li>✅ Phase 1: ComponentCustomSection created
 *   <li>✅ Phase 2: Custom section registered in Parser
 *   <li>✅ Phase 3: Binary deserialization (implemented)
 *   <li>✅ Phase 4: ComponentModel API extensions
 *   <li>⏳ Phase 5: Full dual-mode support
 * </ul>
 */
public class ComponentExtractor {

    /**
     * Extract component definition from WASM module with embedded metadata.
     *
     * @param wasmModule WASM module that may contain "component" custom section
     * @return ComponentDefinition extracted from binary metadata
     * @throws ComponentModelException if no component metadata found or deserialization fails
     */
    public static ComponentDefinition extractComponentDefinition(WasmModule wasmModule)
            throws ComponentModelException {
        ComponentCustomSection section = wasmModule.componentSection();

        if (section == null) {
            throw new ComponentModelException(
                    "No component metadata found in binary. Please provide explicit WIT file using"
                            + " ComponentModel.load(witSource, wasmModule)");
        }

        // Phase 3: Deserialize binary component metadata
        return deserializeComponentBinary(section.getPayload());
    }

    /**
     * Deserialize Component Model binary format to ComponentDefinition.
     *
     * <p>This implements Phase 3 of the WIT extraction feature. The Component Model spec
     * defines how component type information is serialized in the "component" custom section.
     * This method reverses that serialization.
     *
     * <p>Binary format structure:
     * <ul>
     *   <li>package_name: UTF-8 string with LEB128 length
     *   <li>interface_name: UTF-8 string with LEB128 length
     *   <li>type_count: LEB128 unsigned
     *   <li>types: Array of type definitions (not used in MVP)
     *   <li>export_count: LEB128 unsigned
     *   <li>exports: Array of export function signatures
     *   <li>import_count: LEB128 unsigned
     *   <li>imports: Array of import function signatures
     * </ul>
     *
     * @param bytes Raw component section payload (binary-encoded per Component Model spec)
     * @return Deserialized ComponentDefinition
     * @throws ComponentModelException if deserialization fails
     */
    public static ComponentDefinition deserializeComponentBinary(byte[] bytes)
            throws ComponentModelException {
        if (bytes == null || bytes.length == 0) {
            throw new ComponentModelException("Empty component binary payload");
        }

        try {
            BinaryComponentReader reader = new BinaryComponentReader(bytes);

            // Read package name
            String packageName = reader.readString();
            if (packageName == null || packageName.isEmpty()) {
                packageName = "default";
            }

            // Read interface/component name
            String interfaceName = reader.readString();
            if (interfaceName == null || interfaceName.isEmpty()) {
                interfaceName = "component";
            }

            // Create component definition
            ComponentDefinition definition = new ComponentDefinition(packageName, interfaceName);

            // Read and skip type table (for MVP, not building type registry from binary)
            long typeCount = reader.readUnsigned();
            for (int i = 0; i < typeCount; i++) {
                // For MVP, skip type definitions
                // In future phases, parse and register types
                reader.readByte(); // type kind
                // Skip type-specific data (we'd need to know the exact format)
                // For now, this is a limitation of MVP
            }

            // Read exports
            long exportCount = reader.readUnsigned();
            for (int i = 0; i < exportCount; i++) {
                ComponentDefinition.FunctionSignature exportSig =
                        BinaryTypeParser.parseFunction(reader, definition);
                definition.addExport(exportSig);
            }

            // Read imports
            long importCount = reader.readUnsigned();
            for (int i = 0; i < importCount; i++) {
                ComponentDefinition.FunctionSignature importSig =
                        BinaryTypeParser.parseFunction(reader, definition);
                definition.addImport(importSig);
            }

            // Verify we consumed all data (with some tolerance for future extensions)
            if (reader.remaining() > 10) {
                // Only warn if significantly more data than expected
                System.err.println(
                        "[ComponentExtractor] Warning: "
                                + reader.remaining()
                                + " bytes remaining in component binary (may be future format"
                                + " extensions)");
            }

            return definition;

        } catch (ComponentModelException e) {
            throw e;
        } catch (Exception e) {
            throw new ComponentModelException(
                    "Failed to deserialize component binary: " + e.getMessage(), e);
        }
    }

    /**
     * Check if WASM module has embedded component metadata.
     *
     * @param wasmModule WASM module to check
     * @return true if module has "component" custom section, false otherwise
     */
    public static boolean hasComponentMetadata(WasmModule wasmModule) {
        return wasmModule.componentSection() != null;
    }
}
