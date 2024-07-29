package com.dylibso.chicory.runtime;

import java.util.Arrays;

/**
 * A temporary class that gives us a little more control over the interface.
 * It allows us to assert non-nulls as well as throw stack under and overflow exceptions
 * We should replace with something more idiomatic and performant.
 */
public class MStack {
    // TODO: we switch to a different data structure here if we validate that we don't need types at
    // all at runtime
    private long[] stack;
    private int count;
    private int limit;

    public MStack() {
        this.stack = new long[8]; // Arbitrary default
        this.count = 0;
        this.limit = this.stack.length - 1;
    }

    public void push(long v) {
        if (count >= limit) {
            // super naive approach
            var newLimit = limit * 2;
            this.stack = Arrays.copyOf(this.stack, newLimit);
            this.limit = newLimit;
        }
        this.stack[count] = v;
        count++;
    }

    public long pop() {
        count--;
        var r = this.stack[count];
        return r;
    }

    public int popInt() {
        return (int) pop();
    }

    public byte popByte() {
        return (byte) (pop() & 0xff);
    }

    public float popFloat() {
        return Float.intBitsToFloat(popInt());
    }

    public double popDouble() {
        return Double.longBitsToDouble(pop());
    }

    public long peek() {
        var r = this.stack[count - 1];
        return r;
    }

    public int size() {
        return count;
    }

    public String toString() {
        return this.stack.toString();
    }
}
