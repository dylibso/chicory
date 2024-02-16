package com.dylibso.chicory.wasm.io;

import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 *
 */
class FileChannelWasmOutputStream extends ChannelWasmOutputStream {
    private final FileChannel channel;

    FileChannelWasmOutputStream(final FileChannel channel) {
        this.channel = channel;
    }

    FileChannel channel() {
        return channel;
    }

    @Override
    public void close() throws WasmIOException {
        try {
            flush();
        } catch (WasmIOException e) {
            try {
                channel.close();
            } catch (Throwable t) {
                e.addSuppressed(t);
            }
            throw e;
        }
        try {
            channel.close();
        } catch (IOException e) {
            throw new WasmIOException(e);
        }
    }
}
