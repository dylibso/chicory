package com.dylibso.chicory.runtime.alloc;

public final class ExactMemAllocStrategy implements MemAllocStrategy {

    public ExactMemAllocStrategy() {}

    @Override
    public int initial(int min) {
        return min;
    }

    @Override
    public int next(int current, int target) {
        return target;
    }
}
