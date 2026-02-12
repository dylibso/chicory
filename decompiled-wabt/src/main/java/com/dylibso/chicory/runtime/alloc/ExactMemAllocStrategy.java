package com.dylibso.chicory.runtime.alloc;

/**
 * Memory allocation strategy that allocates exactly the requested size.
 *
 * @deprecated Memory is now allocated by page (64KB each), so custom allocation
 *             strategies are no longer used.
 */
@Deprecated
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
