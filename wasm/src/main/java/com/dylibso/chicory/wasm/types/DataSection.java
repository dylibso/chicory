package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DataSection extends Section {
    private final ArrayList<DataSegment> dataSegments;

    /**
     * Construct a new, empty section instance.
     */
    public DataSection() {
        this(new ArrayList<>());
    }

    /**
     * Construct a new, empty section instance.
     *
     * @param estimatedSize the estimated number of data segments to reserve space for
     */
    public DataSection(int estimatedSize) {
        this(new ArrayList<>(estimatedSize));
    }

    private DataSection(ArrayList<DataSegment> dataSegments) {
        super(SectionId.DATA);
        this.dataSegments = dataSegments;
    }

    public DataSegment[] dataSegments() {
        return dataSegments.toArray(DataSegment[]::new);
    }

    public int dataSegmentCount() {
        return dataSegments.size();
    }

    public DataSegment getDataSegment(int idx) {
        return dataSegments.get(idx);
    }

    /**
     * Add a data segment definition to this section.
     *
     * @param dataSegment the data segment to add to this section (must not be {@code null})
     * @return the index of the newly-added data segment
     */
    public int addDataSegment(DataSegment dataSegment) {
        Objects.requireNonNull(dataSegment, "dataSegment");
        int idx = dataSegments.size();
        dataSegments.add(dataSegment);
        return idx;
    }

    public void readFrom(final WasmInputStream in) throws WasmIOException {
        var dataSegmentCount = in.u31();
        dataSegments.ensureCapacity(dataSegments.size() + dataSegmentCount);

        for (var i = 0; i < dataSegmentCount; i++) {
            var mode = in.u32();
            if (mode == 0) {
                var offset = Parser.parseExpression(in);
                ByteBuffer buf = readDataBytes(in);
                addDataSegment(new ActiveDataSegment(List.of(offset), buf));
            } else if (mode == 1) {
                addDataSegment(new PassiveDataSegment(readDataBytes(in)));
            } else if (mode == 2) {
                var memoryId = in.u31();
                var offset = Parser.parseExpression(in);
                ByteBuffer buf = readDataBytes(in);
                addDataSegment(new ActiveDataSegment(memoryId, List.of(offset), buf));
            } else {
                throw new ChicoryException("Failed to parse data segment with data mode: " + mode);
            }
        }
    }

    private static ByteBuffer readDataBytes(final WasmInputStream in) {
        int size = in.u31();
        ByteBuffer buf;
        if (size > 1024) {
            // greater than 1KB; allocate off-heap
            buf = ByteBuffer.allocateDirect(size);
        } else {
            buf = ByteBuffer.allocate(size);
        }
        in.rawBytes(buf);
        return buf.flip();
    }
}
