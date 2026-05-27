package com.example.generated;

import com.dylibso.chicory.component.CanonicalAbi;
import com.dylibso.chicory.component.PojoRegistry;
import com.dylibso.chicory.component.VariantValue;
import com.dylibso.chicory.component.annotation.WitCase;
import com.dylibso.chicory.component.annotation.WitVariant;
import com.dylibso.chicory.component.types.VariantType;
import com.dylibso.chicory.runtime.Memory;
import java.util.Optional;

@WitVariant("color")
public abstract class Color {
    protected final String caseName;
    protected final Object data;

    protected Color(String caseName, Object data) {
        this.caseName = caseName;
        this.data = data;
    }

    public String getCaseName() {
        return caseName;
    }

    public Object getData() {
        return data;
    }

    public long[] encode(Memory memory) throws Exception {
        VariantValue variant = new VariantValue(caseName, data);
        return CanonicalAbi.encode(variant, createVariantType(), memory);
    }

    @WitCase(0)
    public static class Red extends Color {
        public Red() {
            super("red", null);
        }
    }

    @WitCase(1)
    public static class Green extends Color {
        public Green() {
            super("green", null);
        }
    }

    @WitCase(2)
    public static class Blue extends Color {
        public Blue() {
            super("blue", null);
        }
    }

    public static Color decode(long[] encoded, Memory memory) throws Exception {
        Object obj = CanonicalAbi.decode(encoded, createVariantType(), memory);
        if (obj instanceof VariantValue) {
            VariantValue variant = (VariantValue) obj;
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
        variant.addCase("red", Optional.empty());
        variant.addCase("green", Optional.empty());
        variant.addCase("blue", Optional.empty());
        return variant;
    }

    static {
        PojoRegistry.registerVariant("color", Color.class);
    }
}
