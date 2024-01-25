package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.Objects;

public class TypeSection extends Section {
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
}
