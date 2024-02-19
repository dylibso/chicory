package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmInputStream;
import java.util.Arrays;
import java.util.Objects;

public final class FunctionSection extends Section {
    private int[] typeIndices;
    private int count;

    /**
     * Construct a new, empty section instance.
     */
    public FunctionSection() {
        this(new int[8]);
    }

    /**
     * Construct a new, empty section instance.
     *
     * @param estimatedSize the estimated number of functions to reserve space for
     */
    public FunctionSection(int estimatedSize) {
        this(new int[Math.max(8, estimatedSize)]);
    }

    private FunctionSection(int[] typeIndices) {
        super(SectionId.FUNCTION);
        this.typeIndices = typeIndices;
        count = 0;
    }

    public int functionCount() {
        return count;
    }

    public int getFunctionType(int idx) {
        Objects.checkIndex(idx, count);
        return typeIndices[idx];
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
        int count = this.count;
        if (count == typeIndices.length) {
            ensureCapacity(count + (count >> 1));
        }
        typeIndices[count] = typeIndex;
        this.count = count + 1;
        return count;
    }

    private void ensureCapacity(int capacity) {
        if (capacity >= typeIndices.length) {
            typeIndices = Arrays.copyOf(typeIndices, capacity);
        }
    }

    public void readFrom(final WasmInputStream in) throws WasmIOException {
        var functionCount = in.u31();
        ensureCapacity(count + functionCount);

        // Parse individual functions in the function section
        for (int i = 0; i < functionCount; i++) {
            var typeIndex = in.u31();
            addFunctionType(typeIndex);
        }
    }
}
