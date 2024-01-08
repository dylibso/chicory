package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.Value;
import java.util.Stack;

/**
 * A temporary class that gives us a little more control over the interface.
 * It allows us to assert non-nulls as well as throw stack under and overflow exceptions
 * We should replace with something more idiomatic and performant.
 */
public class MStack {
    private final Stack<Value> stack;

    public MStack() {
        this.stack = new Stack<>();
    }

    private StackFrame unwindFrame;

    public void setRestoreFrame(StackFrame frame) {
        this.unwindFrame = frame;
    }

    public void push(Value v) {
        if (v == null) throw new RuntimeException("Can't push null value onto stack");
        this.stack.push(v);
    }

    public Value pop() {
        var r = this.stack.pop();
        if (unwindFrame != null) {
            unwindFrame.stackBefore.push(r);
        }
        if (r == null) throw new RuntimeException("Stack underflow exception");
        return r;
    }

    public Value peek() {
        var r = this.stack.peek();
        if (r == null) throw new RuntimeException("Stack underflow exception");
        return r;
    }

    public int size() {
        return this.stack.size();
    }

    public String toString() {
        return this.stack.toString();
    }
}
