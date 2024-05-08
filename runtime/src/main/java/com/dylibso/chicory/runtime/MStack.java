package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.Value;
import java.util.Arrays;

/**
 * A temporary class that gives us a little more control over the interface. It allows us to assert
 * non-nulls as well as throw stack under and overflow exceptions We should replace with something
 * more idiomatic and performant.
 */
public final class MStack { // TODO: what about renaming to 'OperandStack'

    private static final int MIN_STACK_SIZE = 16;

    private Value[] arr;
    private int topIdx;

    public MStack() {
        // create empty stack from the beginning, don't allocate any memory
        arr = Value.EMPTY_VALUES;
    }

    public void push(Value value) {
        if (value == null) {
            throw new RuntimeException("Can't push null value onto the stack");
        }
        pushValue(value);
    }

    public Value pop() {
        return pollValue();
    }

    public Value peek() {
        return peekValue();
    }

    public int size() {
        return topIdx;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    private void pushValue(Value value) {
        if (topIdx == arr.length) {
            resize();
        }
        arr[topIdx] = value;
        ++topIdx;
    }

    private Value pollValue() {
        if (isEmpty()) {
            throw new RuntimeException("Stack underflow occurred");
        }
        Value retValue = arr[topIdx - 1];
        arr[topIdx - 1] = null;
        --topIdx;

        // shirk array if only 1/3 or less is occupied
        if (size() * 3 <= arr.length) {
            shrink();
        }

        return retValue;
    }

    private Value peekValue() {
        if (isEmpty()) {
            throw new RuntimeException("Stack underflow occurred");
        }
        return arr[topIdx - 1];
    }

    private void resize() {
        // just double the array size
        arr = Arrays.copyOf(arr, Math.max(MIN_STACK_SIZE, arr.length * 2));
    }

    private void shrink() {
        // it's safe to cut half of array, b/c we shrink only if
        // 1/3 or fewer elements are in use
        arr = Arrays.copyOf(arr, arr.length / 2);
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]";
        }

        StringBuilder buf = new StringBuilder(2 + size() * 16);
        buf.append("[");

        buf.append(arr[0]);

        for (int i = 1; i < size(); ++i) {
            buf.append(", ").append(arr[i]);
        }

        buf.append("]");

        return buf.toString();
    }
}
