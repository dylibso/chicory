package com.dylibso.chicory.component.types;

/**
 * Represents an optional (nullable) WIT type.
 *
 * <p>WIT `option&lt;T&gt;` represents a value that may or may not be present. Maps to Java
 * `Optional&lt;T&gt;` for type safety.
 *
 * <p>Examples:
 * <ul>
 *   <li>WIT: `option&lt;s32&gt;` → Java: `Optional&lt;Integer&gt;`
 *   <li>WIT: `option&lt;string&gt;` → Java: `Optional&lt;String&gt;`
 *   <li>WIT: `option&lt;person&gt;` → Java: `Optional&lt;Person&gt;` (where Person is a POJO)
 * </ul>
 *
 * <p>Encoding/Decoding:
 * <ul>
 *   <li>Present value: Encoded as 1 (tag) + wrapped value
 *   <li>Absent value: Encoded as 0 (tag) + zero padding
 * </ul>
 */
public class OptionalType implements WitType {
    private final WitType elementType;

    public OptionalType(WitType elementType) {
        this.elementType = elementType;
    }

    @Override
    public String displayName() {
        return "option<" + elementType.displayName() + ">";
    }

    /**
     * Get the wrapped element type.
     *
     * @return the type that this optional wraps
     */
    public WitType elementType() {
        return elementType;
    }
}
