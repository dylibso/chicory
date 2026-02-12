package com.dylibso.chicory.runtime.alloc;

/**
 * Default memory allocation strategy that doubles the buffer size on growth.
 *
 * @deprecated Memory is now allocated by page (64KB each), so custom allocation
 *             strategies are no longer used.
 */
@Deprecated
public final class DefaultMemAllocStrategy implements MemAllocStrategy {
    private final int max;

    public DefaultMemAllocStrategy(int max) {
        this.max = max;
    }

    @Override
    public int initial(int min) {
        return min;
    }

    @Override
    public int next(int current, int target) {
        int next = (current <= 0) ? target : current;
        while (next < target && next < max) {
            next = next << 1;
            if (next < 0) {
                return max;
            }
        }
        return Math.min(max, next);
    }
}
