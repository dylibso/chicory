package com.example.generated;

import com.dylibso.chicory.component.CanonicalAbi;
import com.dylibso.chicory.component.annotation.WitCase;
import com.dylibso.chicory.component.annotation.WitVariant;
import com.dylibso.chicory.component.types.VariantType;
import com.dylibso.chicory.runtime.Memory;

/** Generated sealed variant class for WIT variant: color { red, green, blue } */
@WitVariant("color")
public abstract class Color {
    protected final String caseName;

    protected Color(String caseName) {
        this.caseName = caseName;
    }

    public String getCaseName() {
        return caseName;
    }

    public long[] encode(Memory memory) throws Exception {
        com.dylibso.chicory.component.VariantValue variant =
                new com.dylibso.chicory.component.VariantValue(caseName, null);
        return CanonicalAbi.encode(variant, createVariantType(), memory);
    }

    @WitCase(0)
    public static class Red extends Color {
        public Red() {
            super("red");
        }

        @Override
        public String toString() {
            return "Red";
        }
    }

    @WitCase(1)
    public static class Green extends Color {
        public Green() {
            super("green");
        }

        @Override
        public String toString() {
            return "Green";
        }
    }

    @WitCase(2)
    public static class Blue extends Color {
        public Blue() {
            super("blue");
        }

        @Override
        public String toString() {
            return "Blue";
        }
    }

    public static Color decode(long[] encoded, Memory memory) throws Exception {
        Object obj = CanonicalAbi.decode(encoded, createVariantType(), memory);
        if (obj instanceof com.dylibso.chicory.component.VariantValue) {
            com.dylibso.chicory.component.VariantValue variant =
                    (com.dylibso.chicory.component.VariantValue) obj;
            switch (variant.caseName()) {
                case "red":
                    return new Red();
                case "green":
                    return new Green();
                case "blue":
                    return new Blue();
                default:
                    throw new IllegalArgumentException(
                            "Unknown variant case: " + variant.caseName());
            }
        }
        throw new IllegalArgumentException("Invalid decode result type");
    }

    private static VariantType createVariantType() {
        VariantType variant = new VariantType("color");
        variant.addCase("red", java.util.Optional.empty());
        variant.addCase("green", java.util.Optional.empty());
        variant.addCase("blue", java.util.Optional.empty());
        return variant;
    }
}
