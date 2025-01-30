package com.dylibso.chicory.runtime.alloc;

public final class OneToOneMemAllocStrategy implements MemAllocStrategy {

    public OneToOneMemAllocStrategy() {}

    @Override
    public int initial(int min) {
        return min;
    }

    @Override
    public int next(int current, int target) {
        return target;
    }
}
