package com.dylibso.chicory.wasm.types;

public class MemoryLimits {
    private int initial;
    private Integer maximum;

    public MemoryLimits(int initial, Integer maximum) {
        this.initial = initial;
        this.maximum = maximum;
    }

    public int getInitial() {
        return initial;
    }

    public Integer getMaximum() {
        return maximum;
    }
}
