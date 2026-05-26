package com.dylibso.chicory.wasm.types;

/**
 * The "component" custom section.
 *
 * <p>Stores binary-encoded Component Model metadata according to the WebAssembly Component
 * Model specification. The payload contains the serialized component type information including
 * types, imports, exports, and instances.
 */
public final class ComponentCustomSection extends CustomSection {

    private final byte[] payload;

    private ComponentCustomSection(byte[] payload) {
        this.payload = payload;
    }

    /**
     * Parse component custom section from raw bytes.
     *
     * @param bytes Raw component section payload
     * @return Parsed ComponentCustomSection
     */
    public static ComponentCustomSection parse(byte[] bytes) {
        return new ComponentCustomSection(bytes);
    }

    /**
     * Get the raw binary payload of the component section.
     *
     * <p>This payload is binary-encoded according to the Component Model spec and contains
     * serialized type information. Use ComponentExtractor to deserialize into ComponentDefinition.
     *
     * @return Raw component section bytes
     */
    public byte[] getPayload() {
        return payload;
    }

    @Override
    public String name() {
        return "component";
    }

    @Override
    public String toString() {
        return String.format("ComponentCustomSection{size=%d bytes}", payload.length);
    }
}
