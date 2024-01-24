package com.dylibso.chicory.runtime.wasi;

import static com.dylibso.chicory.wasm.types.ValueType.*;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.types.Value;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

public class WasiPreview1 {
    private final Logger logger;
    private final WasiOptions options;

    public WasiPreview1(Logger logger) {
        // TODO by default everything should by blocked
        // this works now because streams are null.
        // maybe we want a more explicit way of doing this though
        this(logger, WasiOptions.builder().build());
    }

    public WasiPreview1(Logger logger, WasiOptions opts) {
        this.logger = logger;
        this.options = opts;
        logger.warn(
                "Use of WASIP1 is experimental and will only work for the simplest of use cases.");
    }

    public HostFunction adapterCloseBadfd() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("adapter_close_badfd: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "adapter_close_badfd",
                List.of(I32),
                List.of(I32));
    }

    public HostFunction adapterOpenBadfd() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("adapter_open_badfd: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "adapter_open_badfd",
                List.of(I32),
                List.of(I32));
    }

    public HostFunction argsGet() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("args_get: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "args_get",
                List.of(I32, I32),
                List.of(I32));
    }

    public HostFunction argsSizesGet() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("args_sizes_get: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "args_sizes_get",
                List.of(I32, I32),
                List.of(I32));
    }

    public HostFunction clockResGet() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("clock_res_get: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "clock_res_get",
                List.of(I32, I32),
                List.of(I32));
    }

    public HostFunction clockTimeGet() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("clock_time_get: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "clock_time_get",
                List.of(I32, I64, I32),
                List.of(I32));
    }

    public HostFunction environGet() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("environ_get: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "environ_get",
                List.of(I32, I32),
                List.of(I32));
    }

    public HostFunction environSizesGet() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("environ_sizes_get: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "environ_sizes_get",
                List.of(I32, I32),
                List.of(I32));
    }

    public HostFunction fdAdvise() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("fd_advise: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "fd_advise",
                List.of(I32, I64, I64, I32),
                List.of(I32));
    }

    public HostFunction fdAllocate() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("fd_allocate: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "fd_allocate",
                List.of(I32, I64, I64),
                List.of(I32));
    }

    public HostFunction fdClose() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("fd_close: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "fd_close",
                List.of(I32),
                List.of(I32));
    }

    public HostFunction fdDatasync() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("fd_datasync: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "fd_datasync",
                List.of(I32),
                List.of(I32));
    }

    public HostFunction fdFdstatGet() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("fd_fdstat_get: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "fd_fdstat_get",
                List.of(I32, I32),
                List.of(I32));
    }

    public HostFunction fdFdstatSetFlags() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("fd_fdstat_set_flags: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "fd_fdstat_set_flags",
                List.of(I32, I32),
                List.of(I32));
    }

    public HostFunction fdFdstatSetRights() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("fd_fdstat_set_rights: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "fd_fdstat_set_rights",
                List.of(I32, I64, I32),
                List.of(I32));
    }

    public HostFunction fdFilestatGet() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("fd_filestat_get: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "fd_filestat_get",
                List.of(I32, I32),
                List.of(I32));
    }

    public HostFunction fdFilestatSetSize() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("fd_filestat_set_size: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "fd_filestat_set_size",
                List.of(I32, I64),
                List.of(I32));
    }

    public HostFunction fdFilestatSetTimes() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("fd_filestat_set_times: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "fd_filestat_set_times",
                List.of(I32, I64, I64, I32),
                List.of(I32));
    }

    public HostFunction fdPread() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("fd_pread: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "fd_pread",
                List.of(I32, I32, I32, I64, I32),
                List.of(I32));
    }

    public HostFunction fdPrestatDirName() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("fd_prestat_dir_name: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "fd_prestat_dir_name",
                List.of(I32, I32, I32),
                List.of(I32));
    }

    public HostFunction fdPrestatGet() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("fd_prestat_get: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "fd_prestat_get",
                List.of(I32, I32),
                List.of(I32));
    }

    public HostFunction fdPwrite() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("fd_pwrite: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] { Value.i32(0) };
                },
                "wasi_snapshot_preview1",
                "fd_pwrite",
                List.of(I32, I32, I32, I64, I32),
                List.of(I32));
    }

    public HostFunction fdRead() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    var fd = args[0].asInt();
                    InputStream stream;
                    switch (fd) {
                        case 0:
                            stream = this.options.getStdin();
                            break;
                        default:
                            throw new WASMRuntimeException("We don't yet support fd " + fd);
                    }
                    if (stream == null) {
                        throw new WASMRuntimeException(
                                "The program attempted an fd_read from"
                                        + fd
                                        + "but this was not granted.");
                    }
                    var iovs = args[1].asInt();
                    var iovsLen = args[2].asInt();
                    var retPtr = args[3].asInt();
                    var bytesRead = 0;
                    Memory memory = instance.getMemory();
                    for (var i = 0; i < iovsLen; i++) {
                        var offset = i * 8;
                        var base = iovs + offset;
                        var iovBase = memory.readI32(base).asInt();
                        // TODO this isn't quite right. there is some good info here that
                        // helps understand this
                        // https://github.com/tetratelabs/wazero/blob/f72796965b8900e601ab4f2a3fa54b1d69e11bd9/imports/wasi_snapshot_preview1/fs.go#L693
                        var iovLen = memory.readI32(base + 4).asInt();
                        try {
                            var bytes = stream.readAllBytes();
                            memory.write(iovBase, bytes);
                            bytesRead += bytes.length;
                        } catch (IOException e) {
                            // TODO how to signal to guest?
                            throw new WASMRuntimeException(e);
                        }
                    }
                    memory.write(retPtr, Value.i32(bytesRead));
                    return new Value[] {Value.i32(0)};
                },
                "wasi_snapshot_preview1",
                "fd_read",
                List.of(I32, I32, I32, I32),
                List.of(I32));
    }

    public HostFunction fdReaddir() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("fd_readdir: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "fd_readdir",
                List.of(I32, I32, I32, I64, I32),
                List.of(I32));
    }

    public HostFunction fdRenumber() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("fd_renumber: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "fd_renumber",
                List.of(I32, I32),
                List.of(I32));
    }

    public HostFunction fdSeek() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("fd_seek: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] {Value.i32(0)};
                },
                "wasi_snapshot_preview1",
                "fd_seek",
                List.of(I32, I64, I32, I32),
                List.of(I32));
    }

    public HostFunction fdSync() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("fd_sync: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] {Value.i32(0)};
                },
                "wasi_snapshot_preview1",
                "fd_sync",
                List.of(I32),
                List.of(I32));
    }

    public HostFunction fdTell() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("fd_tell: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                    // return new Value[] {Value.i32(0)};
                },
                "wasi_snapshot_preview1",
                "fd_tell",
                List.of(I32, I32),
                List.of(I32));
    }

    public HostFunction fdWrite() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    var fd = args[0].asInt();
                    PrintStream stream;
                    switch (fd) {
                        case 1:
                            stream = this.options.getStdout();
                            break;
                        case 2:
                            stream = this.options.getStderr();
                            break;
                        default:
                            throw new WASMRuntimeException("We don't yet support fd " + fd);
                    }
                    if (stream == null) {
                        throw new WASMRuntimeException(
                                "The program attempted an fd_write to "
                                        + fd
                                        + "but this was not granted.");
                    }
                    var iovs = args[1].asInt();
                    var iovsLen = args[2].asInt();
                    var retPtr = args[3].asInt();
                    var bytesWritten = 0;
                    Memory memory = instance.getMemory();
                    for (var i = 0; i < iovsLen; i++) {
                        var offset = i * 8;
                        var base = iovs + offset;
                        var iovBase = memory.readI32(base).asInt();
                        var iovLen = memory.readI32(base + 4).asInt();
                        var bytes = memory.readBytes(iovBase, iovLen);
                        // TODO switch on fd
                        try {
                            stream.write(bytes);
                        } catch (IOException e) {
                            // TODO how to signal to the guest
                            throw new RuntimeException(e);
                        }
                        bytesWritten += iovLen;
                    }
                    stream.flush(); // flush it
                    memory.write(retPtr, Value.i32(bytesWritten));
                    return new Value[] {Value.i32(0)};
                },
                "wasi_snapshot_preview1",
                "fd_write",
                List.of(I32, I32, I32, I32),
                List.of(I32));
    }

    public HostFunction getAllocationState() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("get_allocation_state: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "get_allocation_state",
                List.of(),
                List.of(I32));
    }

    public HostFunction getStatePtr() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("get_state_ptr: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "get_state_ptr",
                List.of(),
                List.of(I32));
    }

    public HostFunction Memcpy() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("memcpy: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "memcpy",
                List.of(I32, I32, I32),
                List.of(I32));
    }

    public HostFunction Memove() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("memcpy: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "memmove",
                List.of(I32, I32, I32),
                List.of(I32));
    }

    public HostFunction Memset() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("memset: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "memset",
                List.of(I32, I32, I32),
                List.of(I32));
    }

    public HostFunction pathCreateDirectory() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("path_create_directory: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "path_create_directory",
                List.of(I32, I32, I32),
                List.of(I32));
    }

    public HostFunction pathFilestatGet() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("path_filestat_get: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "path_filestat_get",
                List.of(I32, I32, I32, I32, I32),
                List.of(I32));
    }

    public HostFunction pathFilestatSetTimes() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("path_filestat_set_times: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "path_filestat_set_times",
                List.of(I32, I32, I32, I32, I64, I64, I32),
                List.of(I32));
    }

    public HostFunction pathLink() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("path_link: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "path_link",
                List.of(I32, I32, I32, I32, I32, I32, I32),
                List.of(I32));
    }

    public HostFunction pathOpen() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("path_open: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "path_open",
                List.of(I32, I32, I32, I32, I32, I64, I64, I32, I32),
                List.of(I32));
    }

    public HostFunction pathReadlink() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("path_readlink: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "path_readlink",
                List.of(I32, I32, I32, I32, I32, I32),
                List.of(I32));
    }

    public HostFunction pathRemoveDirectory() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("path_remove_directory: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "path_remove_directory",
                List.of(I32, I32, I32),
                List.of(I32));
    }

    public HostFunction pathRename() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("path_rename: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "path_rename",
                List.of(I32, I32, I32, I32, I32, I32),
                List.of(I32));
    }

    public HostFunction pathSymlink() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("path_symlink: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "path_symlink",
                List.of(I32, I32, I32, I32, I32),
                List.of(I32));
    }

    public HostFunction pathUnlinkFile() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("path_unlink_file: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "path_unlink_file",
                List.of(I32, I32, I32),
                List.of(I32));
    }

    public HostFunction pollOneoff() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("poll_oneoff: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "poll_oneoff",
                List.of(I32, I32, I32, I32),
                List.of(I32));
    }

    public HostFunction procExit() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("proc_exit: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "proc_exit",
                List.of(I32),
                List.of());
    }

    public HostFunction procRaise() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("proc_raise: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "proc_raise",
                List.of(I32),
                List.of(I32));
    }

    public HostFunction randomGet() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("random_get: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "random_get",
                List.of(I32, I32),
                List.of(I32));
    }

    public HostFunction resetAdapterState() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("reset_adapter_state: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "reset_adapter_state",
                List.of(),
                List.of());
    }

    public HostFunction schedYield() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("sched_yield: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "sched_yield",
                List.of(),
                List.of(I32));
    }

    public HostFunction setAllocationState() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("set_allocation_state: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "set_allocation_state",
                List.of(I32),
                List.of());
    }

    public HostFunction setStatePtr() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("set_state_ptr: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "set_state_ptr",
                List.of(I32),
                List.of());
    }

    public HostFunction sockAccept() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("sock_accept: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "sock_accept",
                List.of(I32, I32, I32),
                List.of(I32));
    }

    public HostFunction sockRecv() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("sock_recv: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "sock_recv",
                List.of(I32, I32, I32, I32, I32, I32),
                List.of(I32));
    }

    public HostFunction sockSend() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("sock_send: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "sock_send",
                List.of(I32, I32, I32, I32, I32),
                List.of(I32));
    }

    public HostFunction sockShutdown() {
        return new HostFunction(
                (Instance instance, Value... args) -> {
                    logger.info("sock_shutdown: " + Arrays.toString(args));
                    throw new WASMRuntimeException("We don't yet support this WASI call");
                },
                "wasi_snapshot_preview1",
                "sock_shutdown",
                List.of(I32, I32),
                List.of(I32));
    }

    public HostFunction[] toHostFunctions() {
        return new HostFunction[] {
            adapterCloseBadfd(),
            adapterOpenBadfd(),
            argsGet(),
            argsSizesGet(),
            clockResGet(),
            clockTimeGet(),
            environGet(),
            environSizesGet(),
            fdAdvise(),
            fdAllocate(),
            fdClose(),
            fdDatasync(),
            fdFdstatGet(),
            fdFdstatSetFlags(),
            fdFdstatSetRights(),
            fdFilestatGet(),
            fdFilestatSetSize(),
            fdFilestatSetTimes(),
            fdPread(),
            fdPrestatDirName(),
            fdPrestatGet(),
            fdPwrite(),
            fdRead(),
            fdReaddir(),
            fdRenumber(),
            fdSeek(),
            fdSync(),
            fdTell(),
            fdWrite(),
            getAllocationState(),
            getStatePtr(),
            Memcpy(),
            Memove(),
            Memset(),
            pathCreateDirectory(),
            pathFilestatGet(),
            pathFilestatSetTimes(),
            pathLink(),
            pathOpen(),
            pathReadlink(),
            pathRemoveDirectory(),
            pathRename(),
            pathSymlink(),
            pathUnlinkFile(),
            pollOneoff(),
            procExit(),
            procRaise(),
            randomGet(),
            resetAdapterState(),
            schedYield(),
            setAllocationState(),
            setStatePtr(),
            sockAccept(),
            sockRecv(),
            sockSend(),
            sockShutdown()
        };
    }
}
