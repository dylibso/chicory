package com.dylibso.chicory.wasm.types;

public class MemoryLimits {
    private static int MAX_PAGES = (int) Math.pow(2, 16);
    private int initial;
    private int maximum;

    public MemoryLimits(int initial, Integer maximum) {
        this.initial = initial;
        if (maximum == null) {
            this.maximum = MAX_PAGES;
        } else {
            this.maximum = maximum;
        }
    }

    public int getInitial() {
        return initial;
    }

    public int getMaximum() {
        return maximum;
    }
}
