package com.dylibso.chicory.runtime;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A temporary class that gives us a little more control over the interface.
 * It allows us to assert non-nulls as well as throw stack under and overflow exceptions
 * We should replace with something more idiomatic and performant.
 */
public class MStack {
    private final Deque<Long> stack;

    public MStack() {
        this.stack = new ArrayDeque<>();
    }

    public void push(long v) {
        this.stack.push(v);
    }

    public long pop() {
        return this.stack.pollFirst();
    }

    public long peek() {
        return this.stack.peek();
    }

    public int size() {
        return this.stack.size();
    }

    @Override
    public String toString() {
        return this.stack.toString();
    }
}
