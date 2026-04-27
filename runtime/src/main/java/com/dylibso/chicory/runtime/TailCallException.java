package com.dylibso.chicory.runtime;

public final class TailCallException extends RuntimeException {

    private final int funcId;
    private final long[] args;

    private TailCallException(int funcId, long[] args) {
        super(null, null, true, false);
        this.funcId = funcId;
        this.args = args;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    public static TailCallException of(int funcId, long[] args) {
        return new TailCallException(funcId, args);
    }

    public int funcId() {
        return funcId;
    }

    public long[] args() {
        return args;
    }
}
