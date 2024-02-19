package com.dylibso.chicory.wasm.types;

import java.nio.ByteBuffer;
import java.util.Objects;

public abstract class DataSegment {
    private final ByteBuffer data;

    /**
     * Construct a new instance.
     * The given buffer holds the data for this segment.
     * The data starts at the buffer's position and extends to the buffer's limit.
     * A read-only copy of the buffer object is made to encapsulate this slice, but the underlying data is not copied.
     * If the buffer is to be reused, then its contents should be copied to a new buffer before constructing the data
     * segment.
     *
     * @param data the data for this segment as a byte buffer (must not be {@code null})
     */
    protected DataSegment(ByteBuffer data) {
        this.data = Objects.requireNonNull(data).slice().asReadOnlyBuffer();
    }

    /**
     * {@return the data content of this segment}
     * The returned buffer has a position and limit which is independent of the buffer used by the data segment.
     */
    public ByteBuffer data() {
        return data.duplicate();
    }
}
