package com.dylibso.chicory.component.types;

/**
 * Represents a primitive WIT type.
 */
public enum PrimitiveType implements WitType {
    I32("i32", int.class),
    I64("i64", long.class),
    F32("f32", float.class),
    F64("f64", double.class),
    BOOL("bool", boolean.class),
    CHAR("char", char.class),
    STRING("string", String.class);

    private final String displayNameValue;
    private final Class<?> javaType;

    PrimitiveType(String displayNameValue, Class<?> javaType) {
        this.displayNameValue = displayNameValue;
        this.javaType = javaType;
    }

    @Override
    public String displayName() {
        return displayNameValue;
    }

    public Class<?> javaType() {
        return javaType;
    }

    public static PrimitiveType fromName(String name) {
        for (PrimitiveType t : values()) {
            if (t.displayNameValue.equals(name)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown primitive type: " + name);
    }

    public boolean isIntegral() {
        return this == I32 || this == I64;
    }

    public boolean isFloatingPoint() {
        return this == F32 || this == F64;
    }

    public int wasmBitWidth() {
        if (this == I32 || this == F32) {
            return 32;
        } else if (this == I64 || this == F64) {
            return 64;
        } else if (this == BOOL) {
            return 1;
        } else if (this == CHAR) {
            return 32;
        } else if (this == STRING) {
            return 64; // encoded as (ptr, len)
        }
        return 32;
    }
}
