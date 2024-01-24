package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import java.util.Collection;
import java.util.List;

public class TrapException extends ChicoryException {
    private final List<StackFrame> callStack;

    public TrapException(String msg, Collection<StackFrame> callStack) {
        super(msg);
        this.callStack = List.copyOf(callStack);
    }

    public List<StackFrame> callStack() {
        return callStack;
    }
}
