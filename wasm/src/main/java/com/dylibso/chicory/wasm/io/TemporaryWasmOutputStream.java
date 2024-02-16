package com.dylibso.chicory.wasm.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.ObjLongConsumer;

/**
 * A WASM output stream which writes to memory or a temporary file and calls a callback on close.
 */
class TemporaryWasmOutputStream extends ChannelWasmOutputStream {
    private FileChannel fc;
    private Path path;
    private final ObjLongConsumer<WasmInputStream> onClose;

    TemporaryWasmOutputStream(ObjLongConsumer<WasmInputStream> onClose) {
        this.onClose = onClose;
    }

    @Override
    FileChannel channel() throws WasmIOException {
        if (fc == null)
            try {
                path = Files.createTempFile("temp-", ".bin");
                try {
                    fc =
                            FileChannel.open(
                                    path,
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.READ,
                                    StandardOpenOption.WRITE,
                                    StandardOpenOption.TRUNCATE_EXISTING);
                } catch (IOException e) {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Throwable t) {
                        e.addSuppressed(t);
                    }
                    throw e;
                }
            } catch (IOException e) {
                throw new WasmIOException(e);
            }
        return fc;
    }

    @Override
    public void close() throws WasmIOException {
        if (fc == null) {
            super.close();
            ByteBuffer buffer = this.buffer;
            buffer.flip();
            onClose.accept(WasmInputStream.of(buffer), position);
        } else {
            try {
                try (FileChannel fc = this.fc) {
                    flush();
                    super.close();
                    fc.position(0);
                    try (WasmInputStream in = WasmInputStream.of(Channels.newInputStream(fc))) {
                        onClose.accept(in, position);
                    }
                } catch (Throwable t) {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Throwable t2) {
                        t.addSuppressed(t2);
                    }
                    throw t;
                }
                Files.deleteIfExists(path);
            } catch (IOException e) {
                throw new WasmIOException(e);
            }
        }
    }
}
