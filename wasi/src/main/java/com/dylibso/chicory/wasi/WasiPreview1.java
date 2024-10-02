package com.dylibso.chicory.wasi;

import static com.dylibso.chicory.wasi.Descriptors.DataReader;
import static com.dylibso.chicory.wasi.Descriptors.DataWriter;
import static com.dylibso.chicory.wasi.Descriptors.Descriptor;
import static com.dylibso.chicory.wasi.Descriptors.OpenDirectory;
import static com.dylibso.chicory.wasi.Descriptors.OpenFile;
import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

import com.dylibso.chicory.function.annotations.Buffer;
import com.dylibso.chicory.function.annotations.HostModule;
import com.dylibso.chicory.function.annotations.WasmExport;
import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasi.Descriptors.Directory;
import com.dylibso.chicory.wasi.Descriptors.InStream;
import com.dylibso.chicory.wasi.Descriptors.OutStream;
import com.dylibso.chicory.wasi.Descriptors.PreopenedDirectory;
import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

@HostModule("wasi_snapshot_preview1")
public final class WasiPreview1 implements Closeable {
    private final Logger logger;
    private final Random random;
    private final Clock clock;
    private final List<byte[]> arguments;
    private final List<Entry<byte[], byte[]>> environment;
    private final Descriptors descriptors = new Descriptors();

    public WasiPreview1(Logger logger) {
        // TODO by default everything should by blocked
        // this works now because streams are null.
        // maybe we want a more explicit way of doing this though
        this(logger, WasiOptions.builder().build());
    }

    public WasiPreview1(Logger logger, WasiOptions opts) {
        this.logger = requireNonNull(logger);
        this.random = opts.random();
        this.clock = opts.clock();
        this.arguments =
                opts.arguments().stream().map(value -> value.getBytes(UTF_8)).collect(toList());
        this.environment =
                opts.environment().entrySet().stream()
                        .map(
                                x ->
                                        Map.entry(
                                                x.getKey().getBytes(UTF_8),
                                                x.getValue().getBytes(UTF_8)))
                        .collect(toList());

        descriptors.allocate(new InStream(opts.stdin()));
        descriptors.allocate(new OutStream(opts.stdout()));
        descriptors.allocate(new OutStream(opts.stderr()));

        for (var entry : opts.directories().entrySet()) {
            byte[] name = entry.getKey().getBytes(UTF_8);
            descriptors.allocate(new PreopenedDirectory(name, entry.getValue()));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Logger logger;
        private WasiOptions opts;

        private Builder() {}

        public Builder withLogger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder withOpts(WasiOptions opts) {
            this.opts = opts;
            return this;
        }

        public WasiPreview1 build() {
            if (logger == null) {
                logger = new SystemLogger();
            }
            if (opts == null) {
                opts = WasiOptions.builder().build();
            }
            return new WasiPreview1(logger, opts);
        }
    }

    @Override
    public void close() {
        descriptors.closeAll();
    }

    @WasmExport
    public int adapterCloseBadfd(int fd) {
        logger.infof("adapter_close_badfd: [%s]", fd);
        throw new WASMRuntimeException("We don't yet support this WASI call: adapter_close_badfd");
    }

    @WasmExport
    public int adapterOpenBadfd(int fd) {
        logger.infof("adapter_open_badfd: [%s]", fd);
        throw new WASMRuntimeException("We don't yet support this WASI call: adapter_open_badfd");
    }

    @WasmExport
    public int argsGet(Memory memory, int argv, int argvBuf) {
        logger.infof("args_get: [%s, %s]", argv, argvBuf);
        for (byte[] argument : arguments) {
            memory.writeI32(argv, argvBuf);
            argv += 4;
            memory.write(argvBuf, argument);
            argvBuf += argument.length;
            memory.writeByte(argvBuf, (byte) 0);
            argvBuf++;
        }
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int argsSizesGet(Memory memory, int argc, int argvBufSize) {
        logger.infof("args_sizes_get: [%s, %s]", argc, argvBufSize);
        int bufSize = arguments.stream().mapToInt(x -> x.length + 1).sum();
        memory.writeI32(argc, arguments.size());
        memory.writeI32(argvBufSize, bufSize);
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int clockResGet(Memory memory, int clockId, int resultPtr) {
        logger.infof("clock_res_get: [%s, %s]", clockId, resultPtr);
        switch (clockId) {
            case WasiClockId.REALTIME:
            case WasiClockId.MONOTONIC:
                memory.writeLong(resultPtr, 1L);
                return wasiResult(WasiErrno.ESUCCESS);
            case WasiClockId.PROCESS_CPUTIME_ID:
                throw new WASMRuntimeException("We don't yet support clockid process_cputime_id");
            case WasiClockId.THREAD_CPUTIME_ID:
                throw new WASMRuntimeException("We don't yet support clockid thread_cputime_id");
            default:
                return wasiResult(WasiErrno.EINVAL);
        }
    }

    @WasmExport
    public int clockTimeGet(Memory memory, int clockId, long precision, int resultPtr) {
        logger.infof("clock_time_get: [%s, %s, %s]", clockId, precision, resultPtr);
        switch (clockId) {
            case WasiClockId.REALTIME:
                Instant now = clock.instant();
                long epochNanos = SECONDS.toNanos(now.getEpochSecond()) + now.getNano();
                memory.writeLong(resultPtr, epochNanos);
                return wasiResult(WasiErrno.ESUCCESS);
            case WasiClockId.MONOTONIC:
                memory.writeLong(resultPtr, System.nanoTime());
                return wasiResult(WasiErrno.ESUCCESS);
            case WasiClockId.PROCESS_CPUTIME_ID:
                throw new WASMRuntimeException("We don't yet support clockid process_cputime_id");
            case WasiClockId.THREAD_CPUTIME_ID:
                throw new WASMRuntimeException("We don't yet support clockid thread_cputime_id");
            default:
                return wasiResult(WasiErrno.EINVAL);
        }
    }

    @WasmExport
    public int environGet(Memory memory, int environ, int environBuf) {
        logger.infof("environ_get: [%s, %s]", environ, environBuf);
        for (Entry<byte[], byte[]> entry : environment) {
            byte[] name = entry.getKey();
            byte[] value = entry.getValue();
            byte[] data = new byte[name.length + value.length + 2];
            System.arraycopy(name, 0, data, 0, name.length);
            data[name.length] = '=';
            System.arraycopy(value, 0, data, name.length + 1, value.length);
            data[data.length - 1] = '\0';

            memory.writeI32(environ, environBuf);
            environ += 4;
            memory.write(environBuf, data);
            environBuf += data.length;
        }
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int environSizesGet(Memory memory, int environCount, int environBufSize) {
        logger.infof("environ_sizes_get: [%s, %s]", environCount, environBufSize);
        int bufSize =
                environment.stream()
                        .mapToInt(x -> x.getKey().length + x.getValue().length + 2)
                        .sum();
        memory.writeI32(environCount, environment.size());
        memory.writeI32(environBufSize, bufSize);
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int fdAdvise(int fd, long offset, long len, int advice) {
        logger.infof("fd_advise: [%s, %s, %s, %s]", fd, offset, len, advice);

        if (len < 0 || offset < 0) {
            return wasiResult(WasiErrno.EINVAL);
        }

        var descriptor = descriptors.get(fd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if ((descriptor instanceof InStream) || (descriptor instanceof OutStream)) {
            return wasiResult(WasiErrno.ESPIPE);
        }
        if (descriptor instanceof Directory) {
            return wasiResult(WasiErrno.EISDIR);
        }
        if (!(descriptor instanceof OpenFile)) {
            throw unhandledDescriptor(descriptor);
        }

        // do nothing: advise is optional, and Java does not support it
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int fdAllocate(int fd, long offset, long len) {
        logger.infof("fd_allocate: [%s, %s, %s]", fd, offset, len);

        if (len <= 0 || offset < 0) {
            return wasiResult(WasiErrno.EINVAL);
        }

        var descriptor = descriptors.get(fd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if ((descriptor instanceof InStream) || (descriptor instanceof OutStream)) {
            return wasiResult(WasiErrno.EINVAL);
        }
        if (descriptor instanceof Directory) {
            return wasiResult(WasiErrno.EISDIR);
        }
        if (!(descriptor instanceof OpenFile)) {
            throw unhandledDescriptor(descriptor);
        }

        var channel = ((OpenFile) descriptor).channel();
        try {
            long size = offset + len;
            if (size > channel.size()) {
                long position = channel.position();
                try {
                    channel.position(size - 1);
                    if (channel.write(ByteBuffer.wrap(new byte[1])) != 1) {
                        return wasiResult(WasiErrno.EIO);
                    }
                } finally {
                    channel.position(position);
                }
            }
        } catch (IOException e) {
            return wasiResult(WasiErrno.EIO);
        }
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int fdClose(int fd) {
        logger.infof("fd_close: [%s]", fd);
        Descriptor descriptor = descriptors.get(fd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }
        descriptors.free(fd);
        try {
            if (descriptor instanceof Closeable) {
                ((Closeable) descriptor).close();
            }
        } catch (IOException e) {
            return wasiResult(WasiErrno.EIO);
        }
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int fdDatasync(int fd) {
        logger.infof("fd_datasync: [%s]", fd);
        return wasiResult(fileSync(fd, false));
    }

    @WasmExport
    public int fdFdstatGet(Memory memory, int fd, int buf) {
        logger.infof("fd_fdstat_get: [%s, %s]", fd, buf);
        int flags = 0;
        int rightsBase;
        int rightsInheriting = 0;

        var descriptor = descriptors.get(fd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        WasiFileType fileType;
        if (descriptor instanceof InStream) {
            fileType = WasiFileType.CHARACTER_DEVICE;
            rightsBase = WasiRights.FD_READ;
        } else if (descriptor instanceof OutStream) {
            fileType = WasiFileType.CHARACTER_DEVICE;
            rightsBase = WasiRights.FD_WRITE;
        } else if (descriptor instanceof Directory) {
            fileType = WasiFileType.DIRECTORY;
            rightsBase = WasiRights.DIRECTORY_RIGHTS_BASE;
            rightsInheriting = rightsBase | WasiRights.FILE_RIGHTS_BASE;
        } else if (descriptor instanceof OpenFile) {
            fileType = WasiFileType.REGULAR_FILE;
            rightsBase = WasiRights.FILE_RIGHTS_BASE;
            flags = ((OpenFile) descriptor).fdFlags();
        } else {
            throw unhandledDescriptor(descriptor);
        }

        memory.write(buf, new byte[8]);
        memory.writeByte(buf, (byte) fileType.value());
        memory.writeShort(buf + 2, (short) flags);
        memory.writeLong(buf + 8, rightsBase);
        memory.writeLong(buf + 16, rightsInheriting);
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int fdFdstatSetFlags(int fd, int flags) {
        logger.infof("fd_fdstat_set_flags: [%s, %s]", fd, flags);

        var descriptor = descriptors.get(fd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if ((descriptor instanceof InStream) || (descriptor instanceof OutStream)) {
            return wasiResult(WasiErrno.EINVAL);
        }
        if ((descriptor instanceof OpenDirectory) || (descriptor instanceof PreopenedDirectory)) {
            return wasiResult(WasiErrno.ESUCCESS);
        }
        if (!(descriptor instanceof OpenFile)) {
            throw unhandledDescriptor(descriptor);
        }

        // we don't support changing flags
        if (flags != ((OpenFile) descriptor).fdFlags()) {
            return wasiResult(WasiErrno.ENOTSUP);
        }

        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int fdFdstatSetRights(int fd, long rightsBase, long rightsInheriting) {
        logger.infof("fd_fdstat_set_rights: [%s, %s, %s]", fd, rightsBase, rightsInheriting);
        throw new WASMRuntimeException("We don't yet support this WASI call: fd_fdstat_set_rights");
    }

    @WasmExport
    public int fdFilestatGet(Memory memory, int fd, int buf) {
        logger.infof("fd_filestat_get: [%s, %s]", fd, buf);

        var descriptor = descriptors.get(fd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if ((descriptor instanceof InStream) || (descriptor instanceof OutStream)) {
            Map<String, Object> attributes =
                    Map.of(
                            "dev", 0L,
                            "ino", 0L,
                            "nlink", 1L,
                            "size", 0L,
                            "lastAccessTime", FileTime.from(Instant.EPOCH),
                            "lastModifiedTime", FileTime.from(Instant.EPOCH),
                            "ctime", FileTime.from(Instant.EPOCH));
            writeFileStat(memory, buf, attributes, WasiFileType.CHARACTER_DEVICE);
            return wasiResult(WasiErrno.ESUCCESS);
        }

        Path path;
        if (descriptor instanceof OpenFile) {
            path = ((OpenFile) descriptor).path();
        } else if (descriptor instanceof OpenDirectory) {
            path = ((OpenDirectory) descriptor).path();
        } else {
            throw unhandledDescriptor(descriptor);
        }

        Map<String, Object> attributes;
        try {
            attributes = Files.readAttributes(path, "unix:*");
        } catch (UnsupportedOperationException e) {
            return wasiResult(WasiErrno.ENOTSUP);
        } catch (IOException e) {
            return wasiResult(WasiErrno.EIO);
        }

        writeFileStat(memory, buf, attributes, getFileType(attributes));
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int fdFilestatSetSize(int fd, long size) {
        logger.infof("fd_filestat_set_size: [%s, %s]", fd, size);

        var descriptor = descriptors.get(fd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if ((descriptor instanceof InStream) || (descriptor instanceof OutStream)) {
            return wasiResult(WasiErrno.EINVAL);
        }
        if (descriptor instanceof Directory) {
            return wasiResult(WasiErrno.EISDIR);
        }
        if (!(descriptor instanceof OpenFile)) {
            throw unhandledDescriptor(descriptor);
        }

        SeekableByteChannel channel = ((OpenFile) descriptor).channel();
        try {
            long position = channel.position();
            try {
                if (size <= channel.size()) {
                    channel.truncate(size);
                } else {
                    channel.position(size - 1);
                    if (channel.write(ByteBuffer.wrap(new byte[1])) != 1) {
                        return wasiResult(WasiErrno.EIO);
                    }
                }
            } finally {
                channel.position(position);
            }
        } catch (IOException e) {
            return wasiResult(WasiErrno.EIO);
        }
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int fdFilestatSetTimes(int fd, long accessTime, long modifiedTime, int fstFlags) {
        logger.infof(
                "fd_filestat_set_times: [%s, %s, %s, %s]", fd, accessTime, modifiedTime, fstFlags);

        var descriptor = descriptors.get(fd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if ((descriptor instanceof InStream) || (descriptor instanceof OutStream)) {
            return wasiResult(WasiErrno.EINVAL);
        }

        Path path;
        if (descriptor instanceof OpenFile) {
            path = ((OpenFile) descriptor).path();
        } else if (descriptor instanceof Directory) {
            path = ((Directory) descriptor).path();
        } else {
            throw unhandledDescriptor(descriptor);
        }

        return wasiResult(setFileTimes(path, modifiedTime, accessTime, fstFlags));
    }

    @WasmExport
    public int fdPread(int fd, int iovs, int iovsLen, long offset, int nreadPtr) {
        logger.infof("fd_pread: [%s, %s, %s, %s, %s]", fd, iovs, iovsLen, offset, nreadPtr);
        throw new WASMRuntimeException("We don't yet support this WASI call: fd_pread");
    }

    @WasmExport
    public int fdPrestatDirName(Memory memory, int fd, int path, int pathLen) {
        logger.infof("fd_prestat_dir_name: [%s, %s, %s]", fd, path, pathLen);
        var descriptor = descriptors.get(fd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if (!(descriptor instanceof PreopenedDirectory)) {
            return wasiResult(WasiErrno.EBADF);
        }
        byte[] name = ((PreopenedDirectory) descriptor).name();

        if (pathLen < name.length) {
            return wasiResult(WasiErrno.ENAMETOOLONG);
        }

        memory.write(path, name);
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int fdPrestatGet(Memory memory, int fd, int buf) {
        logger.infof("fd_prestat_get: [%s, %s]", fd, buf);
        var descriptor = descriptors.get(fd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if (!(descriptor instanceof PreopenedDirectory)) {
            return wasiResult(WasiErrno.EBADF);
        }
        int length = ((PreopenedDirectory) descriptor).name().length;

        memory.writeI32(buf, 0); // preopentype::dir
        memory.writeI32(buf + 4, length);
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int fdPwrite(int fd, int iovs, int iovsLen, long offset, int nwrittenPtr) {
        logger.infof("fd_pwrite: [%s, %s, %s, %s, %s]", fd, iovs, iovsLen, offset, nwrittenPtr);
        throw new WASMRuntimeException("We don't yet support this WASI call: fd_pwrite");
    }

    @WasmExport
    public int fdRead(Memory memory, int fd, int iovs, int iovsLen, int nreadPtr) {
        logger.infof("fd_read: [%s, %s, %s, %s]", fd, iovs, iovsLen, nreadPtr);
        var descriptor = descriptors.get(fd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if (descriptor instanceof OutStream) {
            return wasiResult(WasiErrno.EBADF);
        }
        if (descriptor instanceof Directory) {
            return wasiResult(WasiErrno.EISDIR);
        }
        if (!(descriptor instanceof DataReader)) {
            throw unhandledDescriptor(descriptor);
        }
        DataReader reader = (DataReader) descriptor;

        int totalRead = 0;
        for (var i = 0; i < iovsLen; i++) {
            int base = iovs + (i * 8);
            int iovBase = memory.readInt(base);
            var iovLen = memory.readInt(base + 4);
            try {
                byte[] data = new byte[iovLen];
                int read = reader.read(data);
                if (read < 0) {
                    break;
                }
                memory.write(iovBase, data, 0, read);
                totalRead += read;
                if (read < iovLen) {
                    break;
                }
            } catch (IOException e) {
                return wasiResult(WasiErrno.EIO);
            }
        }

        memory.writeI32(nreadPtr, totalRead);
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int fdReaddir(
            Memory memory, int dirFd, int buf, int bufLen, long cookie, int bufUsedPtr) {
        logger.infof("fd_readdir: [%s, %s, %s, %s, %s]", dirFd, buf, bufLen, cookie, bufUsedPtr);
        if (cookie < 0) {
            return wasiResult(WasiErrno.EINVAL);
        }

        var descriptor = descriptors.get(dirFd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        Path directoryPath;
        if (descriptor instanceof Directory) {
            directoryPath = ((Directory) descriptor).path();
        } else {
            return wasiResult(WasiErrno.ENOTDIR);
        }

        int used = 0;
        try (Stream<Path> stream = Files.list(directoryPath)) {
            Stream<Path> special =
                    Stream.of(directoryPath.resolve("."), directoryPath.resolve(".."));
            Iterator<Path> iterator = Stream.concat(special, stream).skip(cookie).iterator();
            while (iterator.hasNext()) {
                Path entryPath = iterator.next();
                byte[] name = entryPath.getFileName().toString().getBytes(UTF_8);
                cookie++;

                Map<String, Object> attributes;
                try {
                    attributes = Files.readAttributes(entryPath, "unix:*");
                } catch (UnsupportedOperationException e) {
                    return wasiResult(WasiErrno.ENOTSUP);
                } catch (NoSuchFileException e) {
                    continue;
                }

                ByteBuffer entry =
                        ByteBuffer.allocate(24 + name.length).order(ByteOrder.LITTLE_ENDIAN);
                entry.putLong(0, cookie);
                entry.putLong(8, ((Number) attributes.get("ino")).longValue());
                entry.putInt(16, name.length);
                entry.put(20, (byte) getFileType(attributes).value());
                entry.position(24);
                entry.put(name);

                int writeSize = min(entry.capacity(), bufLen - used);
                memory.write(buf + used, entry.array(), 0, writeSize);
                used += writeSize;

                if (used == bufLen) {
                    break;
                }
            }
        } catch (NotDirectoryException e) {
            return wasiResult(WasiErrno.ENOTDIR);
        } catch (NoSuchFileException e) {
            return wasiResult(WasiErrno.ENOENT);
        } catch (IOException e) {
            return wasiResult(WasiErrno.EIO);
        }

        memory.writeI32(bufUsedPtr, used);
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int fdRenumber(int from, int to) {
        logger.infof("fd_renumber: [%s, %s]", from, to);
        throw new WASMRuntimeException("We don't yet support this WASI call: fd_renumber");
    }

    @WasmExport
    public int fdSeek(Memory memory, int fd, long offset, int whence, int newOffsetPtr) {
        logger.infof("fd_seek: [%s, %s, %s, %s]", fd, offset, whence, newOffsetPtr);
        if (whence < 0 || whence > 2) {
            return wasiResult(WasiErrno.EINVAL);
        }

        var descriptor = descriptors.get(fd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if ((descriptor instanceof InStream) || (descriptor instanceof OutStream)) {
            return wasiResult(WasiErrno.ESPIPE);
        }
        if (descriptor instanceof Directory) {
            return wasiResult(WasiErrno.EISDIR);
        }
        if (!(descriptor instanceof OpenFile)) {
            throw unhandledDescriptor(descriptor);
        }
        SeekableByteChannel channel = ((OpenFile) descriptor).channel();

        long newOffset;
        try {
            switch (whence) {
                case WasiWhence.SET:
                    channel.position(offset);
                    break;
                case WasiWhence.CUR:
                    channel.position(channel.position() + offset);
                    break;
                case WasiWhence.END:
                    channel.position(channel.size() + offset);
                    break;
            }
            newOffset = channel.position();
        } catch (IllegalArgumentException e) {
            return wasiResult(WasiErrno.EINVAL);
        } catch (IOException e) {
            return wasiResult(WasiErrno.EIO);
        }

        memory.writeLong(newOffsetPtr, newOffset);
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int fdSync(int fd) {
        logger.infof("fd_sync: [%s]", fd);
        return wasiResult(fileSync(fd, true));
    }

    @WasmExport
    public int fdTell(Memory memory, int fd, int offsetPtr) {
        logger.infof("fd_tell: [%s, %s]", fd, offsetPtr);
        var descriptor = descriptors.get(fd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if ((descriptor instanceof InStream) || (descriptor instanceof OutStream)) {
            return wasiResult(WasiErrno.ESPIPE);
        }
        if (descriptor instanceof Directory) {
            return wasiResult(WasiErrno.EISDIR);
        }
        if (!(descriptor instanceof OpenFile)) {
            throw unhandledDescriptor(descriptor);
        }
        SeekableByteChannel channel = ((OpenFile) descriptor).channel();

        long offset;
        try {
            offset = channel.position();
        } catch (IOException e) {
            return wasiResult(WasiErrno.EIO);
        }

        memory.writeLong(offsetPtr, offset);
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int fdWrite(Memory memory, int fd, int iovs, int iovsLen, int nwrittenPtr) {
        logger.infof("fd_write: [%s, %s, %s, %s]", fd, iovs, iovsLen, nwrittenPtr);
        var descriptor = descriptors.get(fd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if (descriptor instanceof InStream) {
            return wasiResult(WasiErrno.EBADF);
        }
        if (descriptor instanceof Directory) {
            return wasiResult(WasiErrno.EISDIR);
        }
        if (!(descriptor instanceof DataWriter)) {
            throw unhandledDescriptor(descriptor);
        }
        DataWriter writer = (DataWriter) descriptor;

        var totalWritten = 0;
        for (var i = 0; i < iovsLen; i++) {
            var base = iovs + (i * 8);
            var iovBase = memory.readInt(base);
            var iovLen = memory.readInt(base + 4);
            var data = memory.readBytes(iovBase, iovLen);
            try {
                int written = writer.write(data);
                totalWritten += written;
                if (written < iovLen) {
                    break;
                }
            } catch (IOException e) {
                return wasiResult(WasiErrno.EIO);
            }
        }

        memory.writeI32(nwrittenPtr, totalWritten);
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int pathCreateDirectory(int dirFd, @Buffer String rawPath) {
        logger.infof("path_create_directory: [%s, \"%s\"]", dirFd, rawPath);
        var descriptor = descriptors.get(dirFd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if (!(descriptor instanceof Directory)) {
            return wasiResult(WasiErrno.ENOTDIR);
        }
        Path directory = ((Directory) descriptor).path();

        Path path = resolvePath(directory, rawPath);
        if (path == null) {
            return wasiResult(WasiErrno.EACCES);
        }

        try {
            Files.createDirectory(path);
        } catch (FileAlreadyExistsException e) {
            return wasiResult(WasiErrno.EEXIST);
        } catch (NoSuchFileException e) {
            return wasiResult(WasiErrno.ENOENT);
        } catch (IOException e) {
            return wasiResult(WasiErrno.EIO);
        }
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int pathFilestatGet(
            Memory memory, int dirFd, int lookupFlags, @Buffer String rawPath, int buf) {
        logger.infof("path_filestat_get: [%s, %s, \"%s\", %s]", dirFd, lookupFlags, rawPath, buf);
        var descriptor = descriptors.get(dirFd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if (!(descriptor instanceof Directory)) {
            return wasiResult(WasiErrno.ENOTDIR);
        }
        Path directory = ((Directory) descriptor).path();

        Path path = resolvePath(directory, rawPath);
        if (path == null) {
            return wasiResult(WasiErrno.EACCES);
        }

        LinkOption[] linkOptions = toLinkOptions(lookupFlags);

        Map<String, Object> attributes;
        try {
            attributes = Files.readAttributes(path, "unix:*", linkOptions);
        } catch (UnsupportedOperationException e) {
            return wasiResult(WasiErrno.ENOTSUP);
        } catch (NoSuchFileException e) {
            return wasiResult(WasiErrno.ENOENT);
        } catch (IOException e) {
            return wasiResult(WasiErrno.EIO);
        }

        writeFileStat(memory, buf, attributes, getFileType(attributes));

        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int pathFilestatSetTimes(
            int fd,
            int lookupFlags,
            @Buffer String rawPath,
            long accessTime,
            long modifiedTime,
            int fstFlags) {
        logger.infof(
                "path_filestat_set_times: [%s, %s, \"%s\", %s, %s, %s]",
                fd, lookupFlags, rawPath, accessTime, modifiedTime, fstFlags);

        var descriptor = descriptors.get(fd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if (!(descriptor instanceof Directory)) {
            return wasiResult(WasiErrno.ENOTDIR);
        }
        Path directory = ((Directory) descriptor).path();

        Path path = resolvePath(directory, rawPath);
        if (path == null) {
            return wasiResult(WasiErrno.EACCES);
        }

        return wasiResult(setFileTimes(path, modifiedTime, accessTime, fstFlags));
    }

    @WasmExport
    public int pathLink(
            int oldFd,
            int oldFlags,
            @Buffer String rawOldPath,
            int newFd,
            @Buffer String rawNewPath) {
        logger.infof(
                "path_link: [%s, %s, \"%s\", %s, \"%s\"]",
                oldFd, oldFlags, rawOldPath, newFd, rawNewPath);
        throw new WASMRuntimeException("We don't yet support this WASI call: path_link");
    }

    @WasmExport
    public int pathOpen(
            Memory memory,
            int dirFd,
            int lookupFlags,
            @Buffer String rawPath,
            int openFlags,
            long rightsBase,
            long rightsInheriting,
            int fdFlags,
            int fdPtr) {
        logger.infof(
                "path_open: [%s, %s, \"%s\", %s, %s, %s, %s, %s]",
                dirFd,
                lookupFlags,
                rawPath,
                openFlags,
                rightsBase,
                rightsInheriting,
                fdFlags,
                fdPtr);
        var descriptor = descriptors.get(dirFd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if (!(descriptor instanceof Directory)) {
            return wasiResult(WasiErrno.ENOTDIR);
        }
        Path directory = ((Directory) descriptor).path();

        Path path = resolvePath(directory, rawPath);
        if (path == null) {
            return wasiResult(WasiErrno.EACCES);
        }

        LinkOption[] linkOptions = toLinkOptions(lookupFlags);

        if (Files.isDirectory(path, linkOptions)) {
            int fd = descriptors.allocate(new OpenDirectory(path));
            memory.writeI32(fdPtr, fd);
            return wasiResult(WasiErrno.ESUCCESS);
        }

        if (flagSet(openFlags, WasiOpenFlags.DIRECTORY) && Files.exists(path, linkOptions)) {
            return wasiResult(WasiErrno.ENOTDIR);
        }

        Set<OpenOption> openOptions = new HashSet<>(Arrays.asList(linkOptions));

        boolean append = flagSet(fdFlags, WasiFdFlags.APPEND);
        boolean truncate = flagSet(openFlags, WasiOpenFlags.TRUNC);

        if (append && truncate) {
            return wasiResult(WasiErrno.ENOTSUP);
        }
        if (!append) {
            openOptions.add(StandardOpenOption.READ);
        }
        openOptions.add(StandardOpenOption.WRITE);

        if (flagSet(openFlags, WasiOpenFlags.CREAT)) {
            if (flagSet(openFlags, WasiOpenFlags.EXCL)) {
                openOptions.add(StandardOpenOption.CREATE_NEW);
            } else {
                openOptions.add(StandardOpenOption.CREATE);
            }
        }
        if (truncate) {
            openOptions.add(StandardOpenOption.TRUNCATE_EXISTING);
        }
        if (append) {
            openOptions.add(StandardOpenOption.APPEND);
        }
        if (flagSet(fdFlags, WasiFdFlags.SYNC)) {
            openOptions.add(StandardOpenOption.SYNC);
        }
        if (flagSet(fdFlags, WasiFdFlags.DSYNC)) {
            openOptions.add(StandardOpenOption.DSYNC);
        }
        // ignore WasiFdFlags.RSYNC and WasiFdFlags.NONBLOCK

        int fd;
        try {
            SeekableByteChannel channel = Files.newByteChannel(path, openOptions);
            fd = descriptors.allocate(new OpenFile(path, channel, fdFlags));
        } catch (FileAlreadyExistsException e) {
            return wasiResult(WasiErrno.EEXIST);
        } catch (NoSuchFileException e) {
            return wasiResult(WasiErrno.ENOENT);
        } catch (IOException e) {
            return wasiResult(WasiErrno.EIO);
        }

        memory.writeI32(fdPtr, fd);
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int pathReadlink(int dirFd, @Buffer String rawPath, int buf, int bufLen, int bufUsed) {
        logger.infof(
                "path_readlink: [%s, \"%s\", %s, %s, %s]", dirFd, rawPath, buf, bufLen, bufUsed);
        throw new WASMRuntimeException("We don't yet support this WASI call: path_readlink");
    }

    @WasmExport
    public int pathRemoveDirectory(int dirFd, @Buffer String rawPath) {
        logger.infof("path_remove_directory: [%s, \"%s\"]", dirFd, rawPath);
        var descriptor = descriptors.get(dirFd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if (!(descriptor instanceof Directory)) {
            return wasiResult(WasiErrno.ENOTDIR);
        }
        Path directory = ((Directory) descriptor).path();

        Path path = resolvePath(directory, rawPath);
        if (path == null) {
            return wasiResult(WasiErrno.EACCES);
        }

        try {
            var attributes =
                    Files.readAttributes(
                            path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isDirectory()) {
                return wasiResult(WasiErrno.ENOTDIR);
            }
            Files.delete(path);
        } catch (NoSuchFileException e) {
            return wasiResult(WasiErrno.ENOENT);
        } catch (DirectoryNotEmptyException e) {
            return wasiResult(WasiErrno.ENOTEMPTY);
        } catch (IOException e) {
            return wasiResult(WasiErrno.EIO);
        }
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int pathRename(
            int oldFd, @Buffer String oldRawPath, int newFd, @Buffer String newRawPath) {
        logger.infof("path_rename: [%s, \"%s\", %s, \"%s\"]", oldFd, oldRawPath, newFd, newRawPath);
        var oldDescriptor = descriptors.get(oldFd);
        if (oldDescriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }
        if (!(oldDescriptor instanceof Directory)) {
            return wasiResult(WasiErrno.ENOTDIR);
        }
        Path oldDirectory = ((Directory) oldDescriptor).path();

        var newDescriptor = descriptors.get(newFd);
        if (newDescriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }
        if (!(newDescriptor instanceof Directory)) {
            return wasiResult(WasiErrno.ENOTDIR);
        }
        Path newDirectory = ((Directory) newDescriptor).path();

        Path oldPath = resolvePath(oldDirectory, oldRawPath);
        if (oldPath == null) {
            return wasiResult(WasiErrno.EACCES);
        }

        Path newPath = resolvePath(newDirectory, newRawPath);
        if (newPath == null) {
            return wasiResult(WasiErrno.EACCES);
        }

        if (Files.isDirectory(oldPath) && Files.isRegularFile(newPath, LinkOption.NOFOLLOW_LINKS)) {
            return wasiResult(WasiErrno.ENOTDIR);
        }
        if (Files.isRegularFile(oldPath, LinkOption.NOFOLLOW_LINKS) && Files.isDirectory(newPath)) {
            return wasiResult(WasiErrno.EISDIR);
        }

        try {
            Files.move(
                    oldPath,
                    newPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.COPY_ATTRIBUTES);
        } catch (UnsupportedOperationException | AtomicMoveNotSupportedException e) {
            return wasiResult(WasiErrno.ENOTSUP);
        } catch (NoSuchFileException e) {
            return wasiResult(WasiErrno.ENOENT);
        } catch (DirectoryNotEmptyException e) {
            return wasiResult(WasiErrno.ENOTEMPTY);
        } catch (IOException e) {
            return wasiResult(WasiErrno.EIO);
        }
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int pathSymlink(@Buffer String oldRawPath, int dirFd, @Buffer String newRawPath) {
        logger.infof("path_symlink: [\"%s\", %s, \"%s\"]", oldRawPath, dirFd, newRawPath);
        throw new WASMRuntimeException("We don't yet support this WASI call: path_symlink");
    }

    @WasmExport
    public int pathUnlinkFile(int dirFd, @Buffer String rawPath) {
        logger.infof("path_unlink_file: [%s, \"%s\"]", dirFd, rawPath);
        var descriptor = descriptors.get(dirFd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if (!(descriptor instanceof Directory)) {
            return wasiResult(WasiErrno.ENOTDIR);
        }
        Path directory = ((Directory) descriptor).path();

        Path path = resolvePath(directory, rawPath);
        if (path == null) {
            return wasiResult(WasiErrno.EACCES);
        }

        try {
            var attributes =
                    Files.readAttributes(
                            path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attributes.isDirectory()) {
                return wasiResult(WasiErrno.EISDIR);
            }
            if (rawPath.endsWith("/")) {
                return wasiResult(WasiErrno.ENOTDIR);
            }
            Files.delete(path);
        } catch (NoSuchFileException e) {
            return wasiResult(WasiErrno.ENOENT);
        } catch (IOException e) {
            return wasiResult(WasiErrno.EIO);
        }
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int pollOneoff(int inPtr, int outPtr, int nsubscriptions, int neventsPtr) {
        logger.infof("poll_oneoff: [%s, %s, %s, %s]", inPtr, outPtr, nsubscriptions, neventsPtr);
        throw new WASMRuntimeException("We don't yet support this WASI call: poll_oneoff");
    }

    @WasmExport
    public void procExit(int code) {
        logger.infof("proc_exit: [%s]", code);
        throw new WasiExitException(code);
    }

    @WasmExport
    public int procRaise(int sig) {
        logger.infof("proc_raise: [%s]", sig);
        throw new WASMRuntimeException("We don't yet support this WASI call: proc_raise");
    }

    @WasmExport
    public int randomGet(Memory memory, int buf, int bufLen) {
        logger.infof("random_get: [%s, %s]", buf, bufLen);
        if (bufLen < 0) {
            return wasiResult(WasiErrno.EINVAL);
        }

        byte[] data = new byte[min(bufLen, 4096)];
        int written = 0;
        while (written < bufLen) {
            if (Thread.currentThread().isInterrupted()) {
                throw new ChicoryException("Thread interrupted");
            }
            int size = min(data.length, bufLen - written);
            if (size < data.length) {
                data = new byte[size];
            }
            random.nextBytes(data);
            memory.write(buf + written, data, 0, size);
            written += size;
        }
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int schedYield() {
        logger.info("sched_yield");
        // do nothing here
        return wasiResult(WasiErrno.ESUCCESS);
    }

    @WasmExport
    public int sockAccept(int sock, int fdFlags, int roFdPtr) {
        logger.infof("sock_accept: [%s, %s, %s]", sock, fdFlags, roFdPtr);
        throw new WASMRuntimeException("We don't yet support this WASI call: sock_accept");
    }

    @WasmExport
    public int sockRecv(
            int sock, int riDataPtr, int riDataLen, int riFlags, int roDataLenPtr, int roFlagsPtr) {
        logger.infof(
                "sock_recv: [%s, %s, %s, %s, %s, %s]",
                sock, riDataPtr, riDataLen, riFlags, roDataLenPtr, roFlagsPtr);
        throw new WASMRuntimeException("We don't yet support this WASI call: sock_recv");
    }

    @WasmExport
    public int sockSend(int sock, int siDataPtr, int siDataLen, int siFlags, int retDataLenPtr) {
        logger.infof(
                "sock_send: [%s, %s, %s, %s, %s]",
                sock, siDataPtr, siDataLen, siFlags, retDataLenPtr);
        throw new WASMRuntimeException("We don't yet support this WASI call: sock_send");
    }

    @WasmExport
    public int sockShutdown(int sock, int how) {
        logger.infof("sock_shutdown: [%s, %s]", sock, how);
        Descriptor descriptor = descriptors.get(sock);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }
        // sockets are not supported, so this cannot be a socket
        return wasiResult(WasiErrno.ENOTSOCK);
    }

    public HostFunction[] toHostFunctions() {
        return WasiPreview1_ModuleFactory.toHostFunctions(this);
    }

    private int wasiResult(WasiErrno errno) {
        if (errno != WasiErrno.ESUCCESS) {
            logger.infof("result = %s", errno.name());
        }
        return errno.value();
    }

    private WasiErrno setFileTimes(Path path, long modifiedTime, long accessTime, int flags) {
        boolean modifiedSet = flagSet(flags, WasiFstFlags.MTIM);
        boolean modifiedNow = flagSet(flags, WasiFstFlags.MTIM_NOW);
        boolean accessSet = flagSet(flags, WasiFstFlags.ATIM);
        boolean accessNow = flagSet(flags, WasiFstFlags.ATIM_NOW);

        if ((modifiedSet && modifiedNow) || (accessSet && accessNow)) {
            return WasiErrno.EINVAL;
        }

        FileTime lastModifiedTime = toFileTime(modifiedTime, modifiedSet, modifiedNow);
        FileTime lastAccessTime = toFileTime(accessTime, accessSet, accessNow);

        try {
            Files.getFileAttributeView(path, BasicFileAttributeView.class)
                    .setTimes(lastModifiedTime, lastAccessTime, null);
        } catch (IOException e) {
            return WasiErrno.EIO;
        }
        return WasiErrno.ESUCCESS;
    }

    private FileTime toFileTime(long time, boolean set, boolean now) {
        if (set) {
            return FileTime.from(time, NANOSECONDS);
        }
        if (now) {
            return FileTime.from(clock.instant());
        }
        return null;
    }

    private WasiErrno fileSync(int fd, boolean metadata) {
        var descriptor = descriptors.get(fd);
        if (descriptor == null) {
            return WasiErrno.EBADF;
        }

        if ((descriptor instanceof InStream)
                || (descriptor instanceof OutStream)
                || (descriptor instanceof Directory)) {
            return WasiErrno.EINVAL;
        }

        if (!(descriptor instanceof OpenFile)) {
            throw unhandledDescriptor(descriptor);
        }
        var channel = ((OpenFile) descriptor).channel();
        if (!(channel instanceof FileChannel)) {
            return WasiErrno.ENOTSUP;
        }
        var fileChannel = (FileChannel) channel;

        try {
            fileChannel.force(metadata);
        } catch (IOException e) {
            return WasiErrno.EIO;
        }
        return WasiErrno.ESUCCESS;
    }

    private static Path resolvePath(Path directory, String rawPathString) {
        Path rawPath;
        try {
            rawPath = directory.getFileSystem().getPath(rawPathString);
        } catch (InvalidPathException e) {
            return null;
        }

        if (rawPath.isAbsolute()) {
            return null;
        }

        String normalized = rawPath.normalize().toString();
        if (normalized.equals("..") || normalized.startsWith("../")) {
            return null;
        }

        return directory.resolve(normalized);
    }

    private static void writeFileStat(
            Memory memory, int buf, Map<String, Object> attributes, WasiFileType fileType) {
        memory.writeLong(buf, (long) attributes.get("dev"));
        memory.writeLong(buf + 8, ((Number) attributes.get("ino")).longValue());
        memory.write(buf + 16, new byte[8]);
        memory.writeByte(buf + 16, (byte) fileType.value());
        memory.writeLong(buf + 24, ((Number) attributes.get("nlink")).longValue());
        memory.writeLong(buf + 32, (long) attributes.get("size"));
        memory.writeLong(buf + 40, fileTimeToNanos(attributes, "lastAccessTime"));
        memory.writeLong(buf + 48, fileTimeToNanos(attributes, "lastModifiedTime"));
        memory.writeLong(buf + 56, fileTimeToNanos(attributes, "ctime"));
    }

    private static long fileTimeToNanos(Map<String, Object> attributes, String name) {
        return ((FileTime) attributes.get(name)).to(NANOSECONDS);
    }

    private static WasiFileType getFileType(Map<String, Object> attributes) {
        if ((boolean) attributes.get("isSymbolicLink")) {
            return WasiFileType.SYMBOLIC_LINK;
        }
        if ((boolean) attributes.get("isDirectory")) {
            return WasiFileType.DIRECTORY;
        }
        if ((boolean) attributes.get("isRegularFile")) {
            return WasiFileType.REGULAR_FILE;
        }
        return WasiFileType.UNKNOWN;
    }

    private static LinkOption[] toLinkOptions(int lookupFlags) {
        return flagSet(lookupFlags, WasiLookupFlags.SYMLINK_FOLLOW)
                ? new LinkOption[0]
                : new LinkOption[] {LinkOption.NOFOLLOW_LINKS};
    }

    private static boolean flagSet(long flags, long mask) {
        if (Long.bitCount(mask) != 1) {
            throw new IllegalArgumentException("mask must be a single bit");
        }
        return (flags & mask) != 0;
    }

    private static RuntimeException unhandledDescriptor(Descriptor descriptor) {
        return new WASMRuntimeException("Unhandled descriptor: " + descriptor.getClass().getName());
    }
}
