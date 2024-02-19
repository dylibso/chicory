package com.dylibso.chicory.wasm.types;

import java.nio.ByteBuffer;

public final class PassiveDataSegment extends DataSegment {
    public static final PassiveDataSegment EMPTY =
            new PassiveDataSegment(ByteBuffer.allocateDirect(0));

    public PassiveDataSegment(ByteBuffer data) {
        super(data);
    }
}
