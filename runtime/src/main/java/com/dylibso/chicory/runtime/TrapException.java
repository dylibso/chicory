package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.ChicoryException;

public class TrapException extends ChicoryException {

    public TrapException(String trappedOnUnreachableInstruction) {
        super(trappedOnUnreachableInstruction);
    }
}
