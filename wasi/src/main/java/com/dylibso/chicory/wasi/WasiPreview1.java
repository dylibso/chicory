package com.dylibso.chicory.wasi;

import static com.dylibso.chicory.wasi.Descriptors.DataReader;
import static com.dylibso.chicory.wasi.Descriptors.DataWriter;
import static com.dylibso.chicory.wasi.Descriptors.Descriptor;
import static com.dylibso.chicory.wasi.Descriptors.OpenDirectory;
import static com.dylibso.chicory.wasi.Descriptors.OpenFile;
import static com.dylibso.chicory.wasm.types.ValueType.I32;
import static com.dylibso.chicory.wasm.types.ValueType.I64;
import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasi.Descriptors.Directory;
import com.dylibso.chicory.wasi.Descriptors.InStream;
import com.dylibso.chicory.wasi.Descriptors.OutStream;
import com.dylibso.chicory.wasi.Descriptors.PreopenedDirectory;
import com.dylibso.chicory.wasm.types.Value;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

public class WasiPreview1 implements AutoCloseable {

    public static final String MODULE_NAME = "wasi_snapshot_preview1";

    private final Logger logger;
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

    public static class Builder {
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

    private Value[] adapterCloseBadfd(Instance instance, Value... args) {
        logger.info("adapter_close_badfd: " + Arrays.toString(args));
        throw new WASMRuntimeException("We don't yet support this WASI call: adapter_close_badfd");
        // return new Value[] { Value.i32(0) };
    }

    private Value[] adaptedOpenBadfd(Instance instance, Value... args) {
        logger.info("adapter_open_badfd: " + Arrays.toString(args));
        throw new WASMRuntimeException("We don't yet support this WASI call: adapter_open_badfd");
        // return new Value[] { Value.i32(0) };
    }

    private Value[] argsGet(Instance instance, Value... args) {
        logger.info("args_get: " + Arrays.toString(args));
        int argv = args[0].asInt();
        int argvBuf = args[1].asInt();

        Memory memory = instance.memory();
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

    private Value[] argsSizesGet(Instance instance, Value... args) {
        logger.info("args_sizes_get: " + Arrays.toString(args));
        int argc = args[0].asInt();
        int argvBufSize = args[1].asInt();

        int bufSize = arguments.stream().mapToInt(x -> x.length + 1).sum();
        Memory memory = instance.memory();
        memory.writeI32(argc, arguments.size());
        memory.writeI32(argvBufSize, bufSize);
        return wasiResult(WasiErrno.ESUCCESS);
    }

    private Value[] clockResGet(Instance instance, Value... args) {
        logger.info("clock_res_get: " + Arrays.toString(args));
        int clockId = args[0].asInt();
        int resultPtr = args[1].asInt();

        Memory memory = instance.memory();
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

    private Value[] clockTimeGet(Instance instance, Value... args) {
        logger.info("clock_time_get: " + Arrays.toString(args));
        int clockId = args[0].asInt();
        long precision = args[1].asLong();
        int resultPtr = args[2].asInt();

        Memory memory = instance.memory();
        switch (clockId) {
            case WasiClockId.REALTIME:
                Instant now = Instant.now();
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

    private Value[] environGet(Instance instance, Value... args) {
        logger.info("environ_get: " + Arrays.toString(args));
        int environ = args[0].asInt();
        int environBuf = args[1].asInt();

        Memory memory = instance.memory();
        for (var entry : environment) {
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

    private Value[] environSizesGet(Instance instance, Value... args) {
        logger.info("environ_sizes_get: " + Arrays.toString(args));
        int environCount = args[0].asInt();
        int environBufSize = args[1].asInt();

        int bufSize =
                environment.stream()
                        .mapToInt(x -> x.getKey().length + x.getValue().length + 2)
                        .sum();
        Memory memory = instance.memory();
        memory.writeI32(environCount, environment.size());
        memory.writeI32(environBufSize, bufSize);
        return wasiResult(WasiErrno.ESUCCESS);
    }

    private Value[] fdAdvise(Instance instance, Value... args) {
        logger.info("fd_advise: " + Arrays.toString(args));
        throw new WASMRuntimeException("We don't yet support this WASI call: fd_advise");
        // return new Value[] { Value.i32(0) };
    }

    private Value[] fdAllocate(Instance instance, Value... args) {
        logger.info("fd_allocate: " + Arrays.toString(args));
        throw new WASMRuntimeException("We don't yet support this WASI call: fd_allocate");
        // return new Value[] { Value.i32(0) };
    }

    private Value[] fdClose(Instance instance, Value... args) {
        logger.info("fd_close: " + Arrays.toString(args));
        int fd = args[0].asInt();

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

    private Value[] fdDatasync(Instance instance, Value... args) {
        logger.info("fd_datasync: " + Arrays.toString(args));
        throw new WASMRuntimeException("We don't yet support this WASI call: fd_datasync");
        // return new Value[] { Value.i32(0) };
    }

    private Value[] fdFdstatGet(Instance instance, Value... args) {
        logger.info("fd_fdstat_get: " + Arrays.toString(args));
        int fd = args[0].asInt();
        int buf = args[1].asInt();
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

        Memory memory = instance.memory();
        memory.write(buf, new byte[8]);
        memory.writeByte(buf, (byte) fileType.ordinal());
        memory.writeShort(buf + 2, (short) flags);
        memory.writeLong(buf + 8, rightsBase);
        memory.writeLong(buf + 16, rightsInheriting);
        return wasiResult(WasiErrno.ESUCCESS);
    }

    private Value[] fdFdstatSetFlags(Instance instance, Value... args) {
        logger.info("fd_fdstat_set_flags: " + Arrays.toString(args));
        int fd = args[0].asInt();
        int flags = args[1].asInt();

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

    private Value[] fdFdstatSetRights(Instance instance, Value... args) {
        logger.info("fd_fdstat_set_rights: " + Arrays.toString(args));
        throw new WASMRuntimeException(
                "We don't yet support this WASI call: fd_fdstat_set_rightsn");
        // return new Value[] { Value.i32(0) };
    }

    private Value[] fdFilestatGet(Instance instance, Value... args) {
        logger.info("fd_filestat_get: " + Arrays.toString(args));
        int fd = args[0].asInt();
        int buf = args[1].asInt();

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
            writeFileStat(instance.memory(), buf, attributes, WasiFileType.CHARACTER_DEVICE);
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

        writeFileStat(instance.memory(), buf, attributes, getFileType(attributes));
        return wasiResult(WasiErrno.ESUCCESS);
    }

    private Value[] fdFilestatSetSize(Instance instance, Value... args) {
        logger.info("fd_filestat_set_size: " + Arrays.toString(args));
        throw new WASMRuntimeException("We don't yet support this WASI call: fd_filestat_set_size");
        // return new Value[] { Value.i32(0) };
    }

    private Value[] fdFilestatSetTimes(Instance instance, Value... args) {
        logger.info("fd_filestat_set_times: " + Arrays.toString(args));
        throw new WASMRuntimeException(
                "We don't yet support this WASI call: fd_filestat_set_times");
        // return new Value[] { Value.i32(0) };
    }

    private Value[] fdPread(Instance instance, Value... args) {
        logger.info("fd_pread: " + Arrays.toString(args));
        throw new WASMRuntimeException("We don't yet support this WASI call: fd_pread");
        // return new Value[] { Value.i32(0) };
    }

    private Value[] fdPrestatDirName(Instance instance, Value... args) {
        logger.info("fd_prestat_dir_name: " + Arrays.toString(args));
        int fd = args[0].asInt();
        int path = args[1].asInt();
        int pathLen = args[2].asInt();

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

        instance.memory().write(path, name);
        return wasiResult(WasiErrno.ESUCCESS);
    }

    private Value[] fdPrestatGet(Instance instance, Value... args) {
        logger.info("fd_prestat_get: " + Arrays.toString(args));
        int fd = args[0].asInt();
        int buf = args[1].asInt();

        var descriptor = descriptors.get(fd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if (!(descriptor instanceof PreopenedDirectory)) {
            return wasiResult(WasiErrno.EBADF);
        }
        int length = ((PreopenedDirectory) descriptor).name().length;

        Memory memory = instance.memory();
        memory.writeI32(buf, 0); // preopentype::dir
        memory.writeI32(buf + 4, length);
        return wasiResult(WasiErrno.ESUCCESS);
    }

    private Value[] fdPwrite(Instance instance, Value... args) {
        logger.info("fd_pwrite: " + Arrays.toString(args));
        throw new WASMRuntimeException("We don't yet support this WASI call: fd_pwrite");
        // return new Value[] { Value.i32(0) };
    }

    private Value[] fdRead(Instance instance, Value... args) {
        logger.info("fd_read: " + Arrays.toString(args));
        var fd = args[0].asInt();
        var iovs = args[1].asInt();
        var iovsLen = args[2].asInt();
        var nreadPtr = args[3].asInt();

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
        Memory memory = instance.memory();
        for (var i = 0; i < iovsLen; i++) {
            int base = iovs + (i * 8);
            int iovBase = memory.readI32(base).asInt();
            var iovLen = memory.readI32(base + 4).asInt();
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

    private Value[] fdReaddir(Instance instance, Value... args) {
        logger.info("fd_readdir: " + Arrays.toString(args));
        int dirFd = args[0].asInt();
        int buf = args[1].asInt();
        int bufLen = args[2].asInt();
        long cookie = args[3].asLong();
        int bufUsedPtr = args[4].asInt();

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

        Memory memory = instance.memory();
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
                entry.put(20, (byte) getFileType(attributes).ordinal());
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

    private Value[] fdRenumber(Instance instance, Value... args) {
        logger.info("fd_renumber: " + Arrays.toString(args));
        throw new WASMRuntimeException("We don't yet support this WASI call: fd_renumber");
    }

    private Value[] fdSeek(Instance instance, Value... args) {
        logger.info("fd_seek: " + Arrays.toString(args));
        int fd = args[0].asInt();
        long offset = args[1].asLong();
        int whence = args[2].asInt();
        int newOffsetPtr = args[3].asInt();

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

        instance.memory().writeLong(newOffsetPtr, newOffset);
        return wasiResult(WasiErrno.ESUCCESS);
    }

    private Value[] fdSync(Instance instance, Value... args) {
        logger.info("fd_sync: " + Arrays.toString(args));
        throw new WASMRuntimeException("We don't yet support this WASI call: fd_sync");
        // return new Value[] {Value.i32(0)};
    }

    private Value[] fdTell(Instance instance, Value... args) {
        logger.info("fd_tell: " + Arrays.toString(args));
        int fd = args[0].asInt();
        int offsetPtr = args[1].asInt();

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

        instance.memory().writeLong(offsetPtr, offset);
        return wasiResult(WasiErrno.ESUCCESS);
    }

    private Value[] fdWrite(Instance instance, Value... args) {
        logger.info("fd_write: " + Arrays.toString(args));
        var fd = args[0].asInt();
        var iovs = args[1].asInt();
        var iovsLen = args[2].asInt();
        var nwrittenPtr = args[3].asInt();

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
        Memory memory = instance.memory();
        for (var i = 0; i < iovsLen; i++) {
            var base = iovs + (i * 8);
            var iovBase = memory.readI32(base).asInt();
            var iovLen = memory.readI32(base + 4).asInt();
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

    private Value[] pathCreateDirectory(Instance instance, Value... args) {
        logger.info("path_create_directory: " + Arrays.toString(args));
        int dirFd = args[0].asInt();
        int pathPtr = args[1].asInt();
        int pathLen = args[2].asInt();

        var descriptor = descriptors.get(dirFd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if (!(descriptor instanceof Directory)) {
            return wasiResult(WasiErrno.ENOTDIR);
        }
        Path directory = ((Directory) descriptor).path();

        String rawPath = instance.memory().readString(pathPtr, pathLen);
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

    private Value[] pathFilestatGet(Instance instance, Value... args) {
        logger.info("path_filestat_get: " + Arrays.toString(args));
        int dirFd = args[0].asInt();
        int lookupFlags = args[1].asInt();
        int pathPtr = args[2].asInt();
        int pathLen = args[3].asInt();
        int buf = args[4].asInt();

        var descriptor = descriptors.get(dirFd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if (!(descriptor instanceof Directory)) {
            return wasiResult(WasiErrno.ENOTDIR);
        }
        Path directory = ((Directory) descriptor).path();

        Memory memory = instance.memory();
        String rawPath = memory.readString(pathPtr, pathLen);
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

    private Value[] pathFilestatSetTimes(Instance instance, Value... args) {
        logger.info("path_filestat_set_times: " + Arrays.toString(args));
        throw new WASMRuntimeException(
                "We don't yet support this WASI call: path_filestat_set_times");
    }

    private Value[] pathLink(Instance instance, Value... args) {
        logger.info("path_link: " + Arrays.toString(args));
        throw new WASMRuntimeException("We don't yet support this WASI call: path_link");
    }

    private Value[] pathOpen(Instance instance, Value... args) {
        logger.info("path_open: " + Arrays.toString(args));
        int dirFd = args[0].asInt();
        int lookupFlags = args[1].asInt();
        int pathPtr = args[2].asInt();
        int pathLen = args[3].asInt();
        int openFlags = args[4].asInt();
        long rightsBase = args[5].asLong();
        long rightsInheriting = args[6].asLong();
        int fdFlags = args[7].asInt();
        int fdPtr = args[8].asInt();

        var descriptor = descriptors.get(dirFd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if (!(descriptor instanceof Directory)) {
            return wasiResult(WasiErrno.ENOTDIR);
        }
        Path directory = ((Directory) descriptor).path();

        Memory memory = instance.memory();
        String rawPath = memory.readString(pathPtr, pathLen);
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

    private Value[] pathReadlink(Instance instance, Value... args) {
        logger.info("path_readlink: " + Arrays.toString(args));
        int dirFd = args[0].asInt();
        int pathPtr = args[1].asInt();
        int pathLen = args[2].asInt();
        int buf = args[3].asInt();
        int bufLen = args[4].asInt();
        int bufUsed = args[5].asInt();

        Memory memory = instance.memory();
        String rawPath = memory.readString(pathPtr, pathLen);

        return wasiResult(WasiErrno.EINVAL);
    }

    private Value[] pathRemoveDirectory(Instance instance, Value... args) {
        logger.info("path_remove_directory: " + Arrays.toString(args));
        int dirFd = args[0].asInt();
        int pathPtr = args[1].asInt();
        int pathLen = args[2].asInt();

        var descriptor = descriptors.get(dirFd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if (!(descriptor instanceof Directory)) {
            return wasiResult(WasiErrno.ENOTDIR);
        }
        Path directory = ((Directory) descriptor).path();

        String rawPath = instance.memory().readString(pathPtr, pathLen);
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

    private Value[] pathRename(Instance instance, Value... args) {
        logger.info("path_rename: " + Arrays.toString(args));
        int oldFd = args[0].asInt();
        int oldPathPtr = args[1].asInt();
        int oldPathLen = args[2].asInt();
        int newFd = args[3].asInt();
        int newPathPtr = args[4].asInt();
        int newPathLen = args[5].asInt();

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

        String oldRawPath = instance.memory().readString(oldPathPtr, oldPathLen);
        Path oldPath = resolvePath(oldDirectory, oldRawPath);
        if (oldPath == null) {
            return wasiResult(WasiErrno.EACCES);
        }

        String newRawPath = instance.memory().readString(newPathPtr, newPathLen);
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

    private Value[] pathSymlink(Instance instance, Value... args) {
        logger.info("path_symlink: " + Arrays.toString(args));
        throw new WASMRuntimeException("We don't yet support this WASI call: path_symlink");
    }

    private Value[] pathUnlinkFile(Instance instance, Value... args) {
        logger.info("path_unlink_file: " + Arrays.toString(args));
        int dirFd = args[0].asInt();
        int pathPtr = args[1].asInt();
        int pathLen = args[2].asInt();

        var descriptor = descriptors.get(dirFd);
        if (descriptor == null) {
            return wasiResult(WasiErrno.EBADF);
        }

        if (!(descriptor instanceof Directory)) {
            return wasiResult(WasiErrno.ENOTDIR);
        }
        Path directory = ((Directory) descriptor).path();

        String rawPath = instance.memory().readString(pathPtr, pathLen);
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

    private Value[] pollOneoff(Instance instance, Value... args) {
        logger.info("poll_oneoff: " + Arrays.toString(args));
        throw new WASMRuntimeException("We don't yet support this WASI call: poll_oneoff");
    }

    private Value[] procExit(Instance instance, Value... args) {
        logger.info("proc_exit: " + Arrays.toString(args));
        int code = args[0].asInt();
        throw new WasiExitException(code);
    }

    private Value[] procRaise(Instance instance, Value... args) {
        logger.info("proc_raise: " + Arrays.toString(args));
        throw new WASMRuntimeException("We don't yet support this WASI call: proc_raise");
    }

    private Value[] randomGet(Instance instance, Value... args) {
        logger.info("random_get: " + Arrays.toString(args));
        int buf = args[0].asInt();
        int bufLen = args[1].asInt();

        if (bufLen < 0) {
            return wasiResult(WasiErrno.EINVAL);
        }
        if (bufLen >= 100_000) {
            throw new WASMRuntimeException("random_get: bufLen must be < 100_000");
        }

        byte[] data = new byte[bufLen];
        new SecureRandom().nextBytes(data);
        instance.memory().write(buf, data);
        return wasiResult(WasiErrno.ESUCCESS);
    }

    private Value[] resetAdapterState(Instance instance, Value... args) {
        logger.info("reset_adapter_state: " + Arrays.toString(args));
        throw new WASMRuntimeException("We don't yet support this WASI call: reset_adapter_state");
    }

    private Value[] schedYield(Instance instance, Value... args) {
        logger.info("sched_yield: " + Arrays.toString(args));
        // do nothing here
        return wasiResult(WasiErrno.ESUCCESS);
    }

    private Value[] setAllocationState(Instance instance, Value... args) {
        logger.info("set_allocation_state: " + Arrays.toString(args));
        throw new WASMRuntimeException("We don't yet support this WASI call: set_allocation_state");
    }

    private Value[] setStatePtr(Instance instance, Value... args) {
        logger.info("set_state_ptr: " + Arrays.toString(args));
        throw new WASMRuntimeException("We don't yet support this WASI call: set_state_ptr");
    }

    private Value[] sockAccept(Instance instance, Value... args) {
        logger.info("sock_accept: " + Arrays.toString(args));
        throw new WASMRuntimeException("We don't yet support this WASI call: sock_accept");
    }

    private Value[] sockRecv(Instance instance, Value... args) {
        logger.info("sock_recv: " + Arrays.toString(args));
        throw new WASMRuntimeException("We don't yet support this WASI call: sock_recv");
    }

    private Value[] sockSend(Instance instance, Value... args) {
        logger.info("sock_send: " + Arrays.toString(args));
        throw new WASMRuntimeException("We don't yet support this WASI call: sock_send");
    }

    private Value[] sockShutdown(Instance instance, Value... args) {
        logger.info("sock_shutdown: " + Arrays.toString(args));
        throw new WASMRuntimeException("We don't yet support this WASI call: sock_shutdown");
    }

    private static void writeFileStat(
            Memory memory, int buf, Map<String, Object> attributes, WasiFileType fileType) {
        memory.writeLong(buf, (long) attributes.get("dev"));
        memory.writeLong(buf + 8, ((Number) attributes.get("ino")).longValue());
        memory.write(buf + 16, new byte[8]);
        memory.writeByte(buf + 16, (byte) fileType.ordinal());
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

    public static HostModule toHostModule() {
        var i32_t = List.of(I32);
        var i32_i32_t = List.of(I32, I32);
        var i32_i32_i32_t = List.of(I32, I32, I32);
        var i32_i32_i32_i32_t = List.of(I32, I32, I32, I32);
        return HostModule.builder(MODULE_NAME)
                .withFunctionSignature("adapter_close_badfd", i32_t, i32_t)
                .withFunctionSignature("adapter_open_badfd", i32_t, i32_t)
                .withFunctionSignature("args_get", i32_i32_t, i32_t)
                .withFunctionSignature("args_sizes_get", i32_i32_t, i32_t)
                .withFunctionSignature("clock_res_get", i32_i32_t, i32_t)
                .withFunctionSignature("clock_time_get", List.of(I32, I64, I32), i32_t)
                .withFunctionSignature("environ_get", i32_i32_t, i32_t)
                .withFunctionSignature("environ_sizes_get", i32_i32_t, i32_t)
                .withFunctionSignature("fd_advise", List.of(I32, I64, I64, I32), i32_t)
                .withFunctionSignature("fd_allocate", List.of(I32, I64, I64), i32_t)
                .withFunctionSignature("fd_close", i32_t, i32_t)
                .withFunctionSignature("fd_datasync", i32_t, i32_t)
                .withFunctionSignature("fd_fdstat_get", i32_i32_t, i32_t)
                .withFunctionSignature("fd_fdstat_set_flags", i32_i32_t, i32_t)
                .withFunctionSignature("fd_fdstat_set_rights", List.of(I32, I64, I32), i32_t)
                .withFunctionSignature("fd_filestat_get", i32_i32_t, i32_t)
                .withFunctionSignature("fd_filestat_set_size", List.of(I32, I64), i32_t)
                .withFunctionSignature("fd_filestat_set_times", List.of(I32, I64, I64, I32), i32_t)
                .withFunctionSignature("fd_pread", List.of(I32, I32, I32, I64, I32), i32_t)
                .withFunctionSignature("fd_prestat_dir_name", i32_i32_i32_t, i32_t)
                .withFunctionSignature("fd_prestat_get", i32_i32_t, i32_t)
                .withFunctionSignature("fd_pwrite", List.of(I32, I32, I32, I64, I32), i32_t)
                .withFunctionSignature("fd_read", i32_i32_i32_i32_t, i32_t)
                .withFunctionSignature("fd_readdir", List.of(I32, I32, I32, I64, I32), i32_t)
                .withFunctionSignature("fd_renumber", i32_i32_t, i32_t)
                .withFunctionSignature("fd_seek", List.of(I32, I64, I32, I32), i32_t)
                .withFunctionSignature("fd_sync", i32_t, i32_t)
                .withFunctionSignature("fd_tell", i32_i32_t, i32_t)
                .withFunctionSignature("fd_write", i32_i32_i32_i32_t, i32_t)
                .withFunctionSignature("path_create_directory", i32_i32_i32_t, i32_t)
                .withFunctionSignature("path_filestat_get", List.of(I32, I32, I32, I32, I32), i32_t)
                .withFunctionSignature(
                        "path_filestat_set_times",
                        List.of(I32, I32, I32, I32, I64, I64, I32),
                        i32_t)
                .withFunctionSignature(
                        "path_link", List.of(I32, I32, I32, I32, I32, I32, I32), i32_t)
                .withFunctionSignature(
                        "path_open", List.of(I32, I32, I32, I32, I32, I64, I64, I32, I32), i32_t)
                .withFunctionSignature(
                        "path_readlink", List.of(I32, I32, I32, I32, I32, I32), i32_t)
                .withFunctionSignature("path_remove_directory", i32_i32_i32_t, i32_t)
                .withFunctionSignature("path_rename", List.of(I32, I32, I32, I32, I32, I32), i32_t)
                .withFunctionSignature("path_symlink", List.of(I32, I32, I32, I32, I32), i32_t)
                .withFunctionSignature("path_unlink_file", i32_i32_i32_t, i32_t)
                .withFunctionSignature("poll_oneoff", i32_i32_i32_i32_t, i32_t)
                .withFunctionSignature("proc_exit", i32_t, List.of())
                .withFunctionSignature("proc_raise", i32_t, i32_t)
                .withFunctionSignature("random_get", i32_i32_t, i32_t)
                .withFunctionSignature("reset_adapter_state", List.of(), List.of())
                .withFunctionSignature("sched_yield", List.of(), i32_t)
                .withFunctionSignature("set_allocation_state", i32_t, List.of())
                .withFunctionSignature("set_state_ptr", i32_t, List.of())
                .withFunctionSignature("sock_accept", i32_i32_i32_t, i32_t)
                .withFunctionSignature("sock_recv", List.of(I32, I32, I32, I32, I32, I32), i32_t)
                .withFunctionSignature("sock_send", List.of(I32, I32, I32, I32, I32), i32_t)
                .withFunctionSignature("sock_shutdown", i32_i32_t, i32_t)
                .build();
    }

    public static HostModuleInstance instance(HostModule hostModule, WasiPreview1 inst) {
        return HostModuleInstance.builder(hostModule)
                .bind("adapter_close_badfd", inst::adapterCloseBadfd)
                .bind("adapter_open_badfd", inst::adaptedOpenBadfd)
                .bind("args_get", inst::argsGet)
                .bind("args_sizes_get", inst::argsSizesGet)
                .bind("clock_res_get", inst::clockResGet)
                .bind("clock_time_get", inst::clockTimeGet)
                .bind("environ_get", inst::environGet)
                .bind("environ_sizes_get", inst::environSizesGet)
                .bind("fd_advise", inst::fdAdvise)
                .bind("fd_allocate", inst::fdAllocate)
                .bind("fd_close", inst::fdClose)
                .bind("fd_datasync", inst::fdDatasync)
                .bind("fd_fdstat_get", inst::fdFdstatGet)
                .bind("fd_fdstat_set_flags", inst::fdFdstatSetFlags)
                .bind("fd_fdstat_set_rights", inst::fdFdstatSetRights)
                .bind("fd_filestat_get", inst::fdFilestatGet)
                .bind("fd_filestat_set_size", inst::fdFilestatSetSize)
                .bind("fd_filestat_set_times", inst::fdFilestatSetTimes)
                .bind("fd_pread", inst::fdPread)
                .bind("fd_prestat_dir_name", inst::fdPrestatDirName)
                .bind("fd_prestat_get", inst::fdPrestatGet)
                .bind("fd_pwrite", inst::fdPwrite)
                .bind("fd_read", inst::fdRead)
                .bind("fd_readdir", inst::fdReaddir)
                .bind("fd_renumber", inst::fdRenumber)
                .bind("fd_seek", inst::fdSeek)
                .bind("fd_sync", inst::fdSync)
                .bind("fd_tell", inst::fdTell)
                .bind("fd_write", inst::fdWrite)
                .bind("path_create_directory", inst::pathCreateDirectory)
                .bind("path_filestat_get", inst::pathFilestatGet)
                .bind("path_filestat_set_times", inst::pathFilestatSetTimes)
                .bind("path_link", inst::pathLink)
                .bind("path_open", inst::pathOpen)
                .bind("path_readlink", inst::pathReadlink)
                .bind("path_remove_directory", inst::pathRemoveDirectory)
                .bind("path_rename", inst::pathRename)
                .bind("path_symlink", inst::pathSymlink)
                .bind("path_unlink_file", inst::pathUnlinkFile)
                .bind("poll_oneoff", inst::pollOneoff)
                .bind("proc_exit", inst::procExit)
                .bind("proc_raise", inst::procRaise)
                .bind("random_get", inst::randomGet)
                .bind("reset_adapter_state", inst::resetAdapterState)
                .bind("sched_yield", inst::schedYield)
                .bind("set_allocation_state", inst::setAllocationState)
                .bind("set_state_ptr", inst::setStatePtr)
                .bind("sock_accept", inst::sockAccept)
                .bind("sock_recv", inst::sockRecv)
                .bind("sock_send", inst::sockSend)
                .bind("sock_shutdown", inst::sockShutdown)
                .onClose(inst::close)
                .build();
    }

    private Value[] wasiResult(WasiErrno errno) {
        if (errno != WasiErrno.ESUCCESS) {
            logger.info("result = " + errno.name());
        }
        return new Value[] {Value.i32(errno.ordinal())};
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
}
