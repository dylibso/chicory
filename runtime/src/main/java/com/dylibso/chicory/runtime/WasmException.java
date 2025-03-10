package com.dylibso.chicory.runtime;

public class WasmException extends RuntimeException {
    private final int tagIdx;
    private final long[] args;

    public WasmException(int tagIdx, long[] args) {
        this.tagIdx = tagIdx;
        this.args = args.clone();
    }

    public int tagIdx() {
        return tagIdx;
    }

    public long[] args() {
        return args;
    }
}
