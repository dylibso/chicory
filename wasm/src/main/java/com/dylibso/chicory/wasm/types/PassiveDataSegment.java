package com.dylibso.chicory.wasm.types;

public class PassiveDataSegment extends DataSegment {
    public static final PassiveDataSegment EMPTY = new PassiveDataSegment(new byte[] {});

    public PassiveDataSegment(byte[] data) {
        super(data);
    }
}
