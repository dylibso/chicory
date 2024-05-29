package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;

public class FunctionSection extends Section {
    private final List<Integer> typeIndices;

    /**
     * Construct a new, empty section instance.
     */
    public FunctionSection() {
        this(new ArrayList());
    }

    /**
     * Construct a new, empty section instance.
     *
     * @param estimatedSize the estimated number of functions to reserve space for
     */
    public FunctionSection(int estimatedSize) {
        this(new ArrayList<>(estimatedSize));
    }

    private FunctionSection(ArrayList<Integer> typeIndices) {
        super(SectionId.FUNCTION);
        this.typeIndices = typeIndices;
    }

    public int getFunctionType(int idx) {
        return typeIndices.get(idx);
    }

    public int functionCount() {
        return typeIndices.size();
    }

    public FunctionType getFunctionType(int idx, TypeSection typeSection) {
        return typeSection.getType(getFunctionType(idx));
    }

    /**
     * Add a function type index to this section.
     *
     * @param typeIndex the type index to add (should be a valid index into the type section)
     * @return the index of the function whose type index was added
     */
    public int addFunctionType(int typeIndex) {
        int idx = typeIndices.size();
        typeIndices.add(typeIndex);
        return idx;
    }
}
