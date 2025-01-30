package com.dylibso.chicory.runtime.alloc;

public interface MemAllocStrategy {
    int initial(int min);

    int next(int current, int target);
}
