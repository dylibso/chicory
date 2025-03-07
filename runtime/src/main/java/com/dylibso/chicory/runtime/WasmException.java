package com.dylibso.chicory.runtime;

public class WasmException extends RuntimeException {
    private final int tagIdx;
    private final long[] args;
    private final Instance instance;

    public WasmException(Instance instance, int tagIdx, long[] args) {
        this.instance = instance;
        this.tagIdx = tagIdx;
        this.args = args.clone();
        this.setStackTrace(new StackTraceElement[0]);
    }

    public Instance instance() {
        return instance;
    }

    public int tagIdx() {
        return tagIdx;
    }

    public long[] args() {
        return args;
    }
}
