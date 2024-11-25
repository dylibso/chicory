package com.dylibso.chicory.runtime;

import java.util.Arrays;

public class MStack {
    public static final int MIN_CAPACITY = 8;

    private int count;
    private long[] elements;

    public MStack() {
        this.elements = new long[MIN_CAPACITY];
    }

    private void increaseCapacity() {
        final int newCapacity = elements.length << 1;

        final long[] array = new long[newCapacity];
        System.arraycopy(elements, 0, array, 0, elements.length);

        elements = array;
    }

    // internal use only!
    public long[] array() {
        return elements;
    }

    public long[] slice(int start, int len) {
        return Arrays.copyOfRange(elements, start, len);
    }

    public void push(long v) {
        elements[count] = v;
        count++;

        if (count == elements.length) {
            increaseCapacity();
        }
    }

    public void pushAll(long... vv) {
        for (long v : vv) {
            push(v);
        }
    }

    public long pop() {
        count--;
        return elements[count];
    }

    public long peek() {
        return elements[count - 1];
    }

    public int size() {
        return count;
    }
}
