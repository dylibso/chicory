package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.exceptions.ChicoryException;

import java.util.List;
import java.util.Stack;

public class TrapException extends ChicoryException {
    private Stack<StackFrame> callStack;

    public TrapException(String msg, Stack<StackFrame> callStack) {
        super(msg);
    }

    public List<StackFrame> getCallStack() {
        return callStack;
    }
}
