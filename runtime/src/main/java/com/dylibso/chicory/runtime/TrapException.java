package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.ChicoryException;

public class TrapException extends ChicoryException {
    private final int[] callStackAddresses;
    private final int[] callStackFunctionIds;

    public TrapException(String msg) {
        this(msg, new int[0], new int[0]);
    }

    public TrapException(
            String trappedOnUnreachableInstruction,
            int[] callStackFunctionIds,
            int[] callStackAddresses) {
        super(trappedOnUnreachableInstruction);
        this.callStackAddresses = callStackAddresses;
        this.callStackFunctionIds = callStackFunctionIds;
    }

    public int[] getCallStackAddresses() {
        return callStackAddresses;
    }

    public int[] getCallStackFunctionIds() {
        return callStackFunctionIds;
    }
}
