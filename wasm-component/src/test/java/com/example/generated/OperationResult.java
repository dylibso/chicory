package com.example.generated;

import com.dylibso.chicory.component.CanonicalAbi;
import com.dylibso.chicory.component.PojoRegistry;
import com.dylibso.chicory.component.VariantValue;
import com.dylibso.chicory.component.annotation.WitCase;
import com.dylibso.chicory.component.annotation.WitVariant;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.VariantType;
import com.dylibso.chicory.runtime.Memory;
import java.util.Optional;

/** Generated sealed variant class for WIT variant: operation-result { ok(string), err(s32) } */
@WitVariant("operation-result")
public abstract class OperationResult {
    protected final String caseName;
    protected final Object data;

    protected OperationResult(String caseName, Object data) {
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
    public static class Ok extends OperationResult {
        public final String value;

        public Ok(String value) {
            super("ok", value);
            this.value = value;
        }

        @Override
        public String toString() {
            return "Ok(" + value + ")";
        }
    }

    @WitCase(1)
    public static class Err extends OperationResult {
        public final int value;

        public Err(int value) {
            super("err", value);
            this.value = value;
        }

        @Override
        public String toString() {
            return "Err(" + value + ")";
        }
    }

    public static OperationResult decode(long[] encoded, Memory memory) throws Exception {
        Object obj = CanonicalAbi.decode(encoded, createVariantType(), memory);
        if (obj instanceof VariantValue) {
            VariantValue variant = (VariantValue) obj;
            switch (variant.caseName()) {
                case "ok":
                    return new Ok((String) variant.data());
                case "err":
                    return new Err(((Number) variant.data()).intValue());
                default:
                    throw new IllegalArgumentException(
                            "Unknown variant case: " + variant.caseName());
            }
        }
        throw new IllegalArgumentException("Invalid decode result type");
    }

    private static VariantType createVariantType() {
        VariantType variant = new VariantType("operation-result");
        variant.addCase("ok", Optional.of(PrimitiveType.STRING));
        variant.addCase("err", Optional.of(PrimitiveType.I32));
        return variant;
    }

    static {
        PojoRegistry.registerVariant("operation-result", OperationResult.class);
    }
}
