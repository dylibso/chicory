package com.dylibso.chicory.runtime;

/**
 * A temporary class that gives us a little more control over the interface.
 * It allows us to assert non-nulls as well as throw stack under and overflow exceptions
 * We should replace with something more idiomatic and performant.
 */
public class MStack {
    public static final int MIN_CAPACITY = 8;

    private int count;
    private long[] elements;

    private void increaseCapacity() {
        final int newCapacity = elements.length << 1;

        final long[] array = new long[newCapacity];
        System.arraycopy(elements, 0, array, 0, elements.length);

        elements = array;
    }

    public MStack() {
        this.elements = new long[MIN_CAPACITY];
    }

    public void push(long v) {
        elements[count] = v;
        count++;

        if (count == elements.length) {
            increaseCapacity();
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
