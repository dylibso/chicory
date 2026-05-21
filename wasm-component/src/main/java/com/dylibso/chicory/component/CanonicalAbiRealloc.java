package com.dylibso.chicory.component;

import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Memory;
import java.nio.charset.StandardCharsets;

/**
 * Manages cabi_realloc calls for allocating memory in the guest for dynamically-sized data.
 *
 * <p>The canonical ABI requires that the host calls the guest's canonical_abi_realloc function
 * when allocating new memory. This function signature is:
 *
 * <pre>
 * func canonical_abi_realloc(original_ptr: i32, original_size: i32, alignment: i32, new_size: i32) -> i32
 * </pre>
 *
 * Returns a pointer to newly allocated memory in the guest's linear memory.
 */
public class CanonicalAbiRealloc {

    /**
     * Allocate memory in the guest and write a string.
     *
     * @param value String to write
     * @param realloc Export function for canonical_abi_realloc
     * @param memory Guest's linear memory
     * @return (ptr, len) pair encoded as single long
     */
    public static long allocateString(String value, ExportFunction realloc, Memory memory) {
        if (value == null) {
            return 0; // null string is (0, 0)
        }

        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 0) {
            return 0; // empty string is (0, 0) after encoding
        }

        // Call cabi_realloc(0, 0, 1, len)
        long[] result = realloc.apply(0, 0, 1, bytes.length);
        int ptr = (int) result[0];

        // Write the string bytes to guest memory
        memory.write(ptr, bytes);

        // Encode as (ptr << 32) | len
        long len = bytes.length & 0xFFFFFFFFL;
        return (((long) ptr) << 32) | len;
    }

    /**
     * Allocate memory for a list and write element pointers.
     *
     * @param elementCount Number of elements
     * @param elementSizeBytes Size of each element in bytes
     * @param realloc Export function for canonical_abi_realloc
     * @param memory Guest's linear memory
     * @return Pointer to allocated memory
     */
    public static int allocateList(
            int elementCount, int elementSizeBytes, ExportFunction realloc, Memory memory) {
        if (elementCount == 0) {
            return 0;
        }

        int totalSize = elementCount * elementSizeBytes;

        // Call cabi_realloc(0, 0, alignment, size)
        // Use natural alignment (power of 2 matching element size)
        int alignment = Integer.highestOneBit(elementSizeBytes);
        long[] result = realloc.apply(0, 0, alignment, totalSize);

        return (int) result[0];
    }

    /**
     * Reallocate existing guest memory (for growing buffers).
     *
     * @param currentPtr Current pointer
     * @param currentSize Current size in bytes
     * @param newSize New size in bytes
     * @param alignment Alignment requirement
     * @param realloc Export function for canonical_abi_realloc
     * @return New pointer after reallocation
     */
    public static int reallocate(
            int currentPtr, int currentSize, int newSize, int alignment, ExportFunction realloc) {
        long[] result = realloc.apply(currentPtr, currentSize, alignment, newSize);
        return (int) result[0];
    }

    /**
     * Get the canonical_abi_realloc export from a component instance.
     *
     * @param instance The component instance
     * @return The realloc function, or null if not exported
     */
    public static ExportFunction getReallocFunction(com.dylibso.chicory.runtime.Instance instance) {
        return instance.export("canonical_abi_realloc");
    }
}
