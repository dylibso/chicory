package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmInputStream;
import java.util.ArrayList;
import java.util.Objects;

public final class TypeSection extends Section {
    private final ArrayList<FunctionType> types;

    /**
     * Construct a new, empty section instance.
     */
    public TypeSection() {
        this(new ArrayList<>());
    }

    /**
     * Construct a new, empty section instance.
     *
     * @param estimatedSize the estimated number of types to reserve space for
     */
    public TypeSection(int estimatedSize) {
        this(new ArrayList<>(estimatedSize));
    }

    private TypeSection(ArrayList<FunctionType> types) {
        super(SectionId.TYPE);
        this.types = types;
    }

    public FunctionType[] types() {
        return types.toArray(FunctionType[]::new);
    }

    public int typeCount() {
        return types.size();
    }

    public FunctionType getType(int idx) {
        return types.get(idx);
    }

    /**
     * Add a function type definition to this section.
     *
     * @param functionType the function type to add to this section (must not be {@code null})
     * @return the index of the newly-added function type
     */
    public int addFunctionType(FunctionType functionType) {
        Objects.requireNonNull(functionType, "functionType");
        int idx = types.size();
        types.add(functionType);
        return idx;
    }

    public void readFrom(final WasmInputStream in) throws WasmIOException {
        var typeCount = in.u31();
        types.ensureCapacity(types.size() + typeCount);

        // Parse individual types in the type section
        for (int i = 0; i < typeCount; i++) {
            var form = in.u8();

            if (form != 0x60) {
                throw new RuntimeException(
                        "We don't support non func types. Form "
                                + String.format("0x%02X", form)
                                + " was given but we expected 0x60");
            }

            // Parse function types (form = 0x60)
            var paramCount = in.u16();
            var params = new ValueType[paramCount];

            // Parse parameter types
            for (int j = 0; j < paramCount; j++) {
                params[j] = ValueType.forId(in.u8());
            }

            var returnCount = in.u16();
            var returns = new ValueType[returnCount];

            // Parse return types
            for (int j = 0; j < returnCount; j++) {
                returns[j] = ValueType.forId(in.u8());
            }

            addFunctionType(FunctionType.of(params, returns));
        }
    }
}
