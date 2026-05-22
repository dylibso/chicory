package com.dylibso.chicory.component;

import java.util.Objects;

/**
 * Represents a variant (tagged union) value with a discriminant and optional data.
 * Provides a type-safe wrapper for variant types in component interactions.
 */
public class VariantValue {
    private final String caseName;
    private final Object data;

    /**
     * Create a variant case with no data (empty case).
     *
     * @param caseName Name of the variant case
     */
    public VariantValue(String caseName) {
        this(caseName, null);
    }

    /**
     * Create a variant case with data.
     *
     * @param caseName Name of the variant case
     * @param data Data associated with this case (can be null for empty cases)
     */
    public VariantValue(String caseName, Object data) {
        this.caseName = Objects.requireNonNull(caseName, "caseName cannot be null");
        this.data = data;
    }

    /**
     * Get the variant case name.
     *
     * @return Case name (discriminant identifier)
     */
    public String caseName() {
        return caseName;
    }

    /**
     * Get the data associated with this variant case.
     *
     * @return Case data, or null if this is an empty case
     */
    public Object data() {
        return data;
    }

    /**
     * Check if this variant case is empty (no data).
     *
     * @return true if data is null
     */
    public boolean isEmpty() {
        return data == null;
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return caseName;
        }
        return caseName + "(" + data + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VariantValue)) return false;
        VariantValue that = (VariantValue) o;
        return caseName.equals(that.caseName) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseName, data);
    }
}
