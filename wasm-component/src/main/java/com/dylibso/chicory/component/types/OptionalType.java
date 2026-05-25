package com.dylibso.chicory.component.types;

/**
 * Represents an optional (nullable) WIT type.
 *
 * <p>WIT `option<T>` represents a value that may or may not be present. Maps to Java
 * `Optional<T>` for type safety.
 *
 * <p>Examples:
 * <ul>
 *   <li>WIT: `option<s32>` → Java: `Optional<Integer>`
 *   <li>WIT: `option<string>` → Java: `Optional<String>`
 *   <li>WIT: `option<person>` → Java: `Optional<Person>` (where Person is a POJO)
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
