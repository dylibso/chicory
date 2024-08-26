package com.dylibso.chicory.wasi;

enum WasiFileType {
    UNKNOWN,
    BLOCK_DEVICE,
    CHARACTER_DEVICE,
    DIRECTORY,
    REGULAR_FILE,
    SOCKET_DGRAM,
    SOCKET_STREAM,
    SYMBOLIC_LINK;

    @SuppressWarnings("EnumOrdinal")
    public int value() {
        return ordinal();
    }
}
