package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.Value;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A temporary class that gives us a little more control over the interface.
 * It allows us to assert non-nulls as well as throw stack under and overflow exceptions
 * We should replace with something more idiomatic and performant.
 */
public class MStack {
    private final Deque<Value> stack;

    public MStack() {
        this.stack = new ArrayDeque<>();
    }

    public void push(Value v) {
        if (v == null) {
            throw new RuntimeException("Can't push null value onto stack");
        }
        this.stack.push(v);
    }

    public Value pop() {
        var r = this.stack.pollFirst();
        if (r == null) {
            throw new RuntimeException("Stack underflow exception");
        }
        return r;
    }

    public Value peek() {
        var r = this.stack.peek();
        if (r == null) {
            throw new RuntimeException("Stack underflow exception");
        }
        return r;
    }

    public int size() {
        return this.stack.size();
    }

    @Override
    public String toString() {
        return this.stack.toString();
    }
}
