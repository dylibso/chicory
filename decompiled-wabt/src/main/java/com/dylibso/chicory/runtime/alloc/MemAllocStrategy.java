package com.dylibso.chicory.runtime.alloc;

/**
 * Strategy for allocating memory buffers.
 *
 * @deprecated Memory is now allocated by page (64KB each), so custom allocation
 *             strategies are no longer used. This interface will be removed in
 *             a future release.
 */
@Deprecated
public interface MemAllocStrategy {
    int initial(int min);

    int next(int current, int target);
}
