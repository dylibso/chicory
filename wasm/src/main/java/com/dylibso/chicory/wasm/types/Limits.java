package com.dylibso.chicory.wasm.types;

public class Limits {
    private final int min;
    private final int max;

    public Limits(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }
}
