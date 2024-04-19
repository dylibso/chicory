package com.dylibso.chicory.wasm.types;

import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;

import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import java.util.Arrays;
import java.util.Objects;

public class Table {
    private final ValueType elementType;
    private final Limits limits;
    private int[] refs;

    public Table(final ValueType elementType, final Limits limits) {
        this.elementType = Objects.requireNonNull(elementType, "elementType");
        if (!elementType.isReference()) {
            throw new IllegalArgumentException("Table element type must be a reference type");
        }
        this.limits = Objects.requireNonNull(limits, "limits");
        refs = new int[(int) limits.min()];
        Arrays.fill(refs, REF_NULL_VALUE);
    }

    public ValueType elementType() {
        return elementType;
    }

    public Limits limits() {
        return limits;
    }

    public int size() {
        return refs.length;
    }

    public int grow(int size, int value) {
        var oldSize = refs.length;
        var targetSize = oldSize + size;
        if (size < 0 || targetSize > limits().max()) {
            return -1;
        }
        var newRefs = Arrays.copyOf(refs, targetSize);
        Arrays.fill(newRefs, oldSize, targetSize, value);
        refs = newRefs;
        return oldSize;
    }

    public Value ref(int index) {
        int res;
        try {
            res = this.refs[index];
        } catch (IndexOutOfBoundsException e) {
            throw new ChicoryException("undefined element", e);
        }
        if (this.elementType() == ValueType.FuncRef) {
            return Value.funcRef(res);
        } else {
            return Value.externRef(res);
        }
    }

    public void setRef(int index, int value) {
        try {
            this.refs[index] = value;
        } catch (IndexOutOfBoundsException e) {
            throw new ChicoryException("out of bounds table access", e);
        }
    }

    public void reset() {
        for (int i = 0; i < refs.length; i++) {
            this.refs[i] = REF_NULL_VALUE;
        }
    }
}
