package com.dylibso.chicory.wasi;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

final class Descriptors {
    private final List<Descriptor> descriptors = new ArrayList<>();
    private final NavigableSet<Integer> freeFds = new TreeSet<>();

    public Descriptor get(int fd) {
        if (fd < 0 || fd >= descriptors.size()) {
            return null;
        }
        return descriptors.get(fd);
    }

    public int allocate(Descriptor descriptor) {
        Integer fd = freeFds.pollFirst();
        if (fd != null) {
            descriptors.set(fd, descriptor);
            return fd;
        }
        descriptors.add(descriptor);
        return descriptors.size() - 1;
    }

    public void free(int fd) {
        descriptors.set(fd, null);
        freeFds.add(fd);
    }

    public void set(int fd, Descriptor descriptor) {
        descriptors.set(fd, descriptor);
    }

    public void closeAll() {
        RuntimeException exception = null;
        for (var descriptor : descriptors) {
            try {
                if (descriptor instanceof Closeable) {
                    ((Closeable) descriptor).close();
                }
            } catch (Throwable t) {
                if (exception == null) {
                    exception = new RuntimeException();
                }
                exception.addSuppressed(t);
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    interface Descriptor {}

    interface DataReader {
        int read(byte[] data) throws IOException;
    }

    interface DataWriter {
        int write(byte[] data) throws IOException;
    }

    interface Directory {
        Path path();
    }

    static final class InStream implements Descriptor, DataReader {
        private final InputStream in;

        public InStream(InputStream in) {
            this.in = requireNonNull(in);
        }

        @Override
        public int read(byte[] data) throws IOException {
            return in.read(data);
        }

        public int available() throws IOException {
            return in.available();
        }
    }

    static final class OutStream implements Descriptor, DataWriter {
        private final OutputStream out;

        public OutStream(OutputStream out) {
            this.out = requireNonNull(out);
        }

        @Override
        public int write(byte[] data) throws IOException {
            out.write(data);
            return data.length;
        }
    }

    static final class PreopenedDirectory implements Descriptor, Directory {
        private final byte[] name;
        private final Path path;

        public PreopenedDirectory(byte[] name, Path path) {
            this.name = requireNonNull(name);
            this.path = requireNonNull(path);
        }

        public byte[] name() {
            return name;
        }

        @Override
        public Path path() {
            return path;
        }
    }

    static final class OpenDirectory implements Descriptor, Directory {
        private final Path path;

        public OpenDirectory(Path path) {
            this.path = requireNonNull(path);
        }

        @Override
        public Path path() {
            return path;
        }
    }

    static final class OpenFile implements Descriptor, Closeable, DataReader, DataWriter {
        private final Path path;
        private final FileChannel channel;
        private final int fdFlags;
        private final long rights;

        public OpenFile(Path path, FileChannel channel, int fdFlags, long rights) {
            this.path = requireNonNull(path);
            this.channel = requireNonNull(channel);
            this.fdFlags = fdFlags;
            this.rights = rights;
        }

        public Path path() {
            return path;
        }

        public FileChannel channel() {
            return channel;
        }

        public int fdFlags() {
            return fdFlags;
        }

        public long rights() {
            return rights;
        }

        @Override
        public int read(byte[] data) throws IOException {
            return channel.read(ByteBuffer.wrap(data));
        }

        public int read(byte[] data, long position) throws IOException {
            return channel.read(ByteBuffer.wrap(data), position);
        }

        @Override
        public int write(byte[] data) throws IOException {
            return channel.write(ByteBuffer.wrap(data));
        }

        public int write(byte[] data, long position) throws IOException {
            return channel.write(ByteBuffer.wrap(data), position);
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }
    }
}
