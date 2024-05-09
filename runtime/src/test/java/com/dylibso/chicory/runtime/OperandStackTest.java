package com.dylibso.chicory.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dylibso.chicory.wasm.types.Value;
import org.junit.jupiter.api.Test;

public class OperandStackTest {

    @Test
    public void push() {
        var stack = new OperandStack();

        assertEquals(0, stack.size());

        stack.push(Value.i32(5));
        stack.push(Value.i32(10));

        assertEquals(2, stack.size());
    }

    @Test
    public void pushWithResizing() {
        var stack = new OperandStack();

        for (int i = 0; i < 30; ++i) {
            stack.push(Value.i32(i));
        }

        assertEquals(30, stack.size());
    }

    @Test
    public void pushNullValueThrowsException() {
        var stack = new OperandStack();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> stack.push(null));

        assertEquals("Can't push null value onto stack", ex.getMessage());
    }

    @Test
    public void pushWithPeekAndPoll() {
        var stack = new OperandStack();

        for (int i = 0; i < 10; ++i) {
            stack.push(Value.i32(i));
        }

        for (int it = 0; it < 5; ++it) {
            assertEquals(Value.i32(9), stack.peek());
        }

        for (int i = 9; i >= 1; --i) {
            assertEquals(Value.i32(i), stack.pop());
        }

        assertEquals(Value.i32(0), stack.peek());
        assertEquals(Value.i32(0), stack.pop());
    }

    @Test
    public void peekFromEmptyStackThrowsException() {
        var stack = new OperandStack();

        RuntimeException ex = assertThrows(RuntimeException.class, stack::peek);

        assertEquals("Stack underflow exception", ex.getMessage());
    }

    @Test
    public void popFromEmptyStackThrowsException() {
        var stack = new OperandStack();

        stack.push(Value.TRUE);
        stack.push(Value.FALSE);

        stack.pop();
        stack.pop();

        RuntimeException ex = assertThrows(RuntimeException.class, stack::pop);

        assertEquals("Stack underflow exception", ex.getMessage());
    }
}
