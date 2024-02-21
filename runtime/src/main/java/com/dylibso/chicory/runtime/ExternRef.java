package com.dylibso.chicory.runtime;

import java.util.Objects;

/**
 * A reference to a value which is external to the WASM runtime.
 */
public final class ExternRef extends Ref {
    private final Object value;

    /**
     * Construct a new instance.
     *
     * @param value the external value (must not be {@code null})
     */
    public ExternRef(final Object value) {
        this.value = Objects.requireNonNull(value);
    }

    /**
     * {@return the externally referenced value}
     */
    public Object value() {
        return value;
    }

    public int hashCode() {
        return value.hashCode();
    }

    public boolean equals(final Object obj) {
        return obj instanceof ExternRef && equals((ExternRef) obj);
    }

    public boolean equals(final ExternRef other) {
        return this == other || other != null && Objects.equals(value, other.value);
    }

    public String toString() {
        return "extern(" + value + ")";
    }
}
