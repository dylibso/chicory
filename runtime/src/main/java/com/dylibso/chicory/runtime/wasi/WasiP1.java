package com.dylibso.chicory.runtime.wasi;

import static com.dylibso.chicory.wasm.types.ValueType.*;

import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.types.Value;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

public class WasiP1 {
    private static final System.Logger LOGGER = System.getLogger(WasiP1.class.getName());

    private WasiOptions options;

    public WasiP1() {
        // TODO by default everything should by blocked
        // this works now because streams are null.
        // maybe we want a more explicit way of doing this though
        this(new WasiOptions());
    }

    public WasiP1(WasiOptions opts) {
        this.options = opts;
        LOGGER.log(
                System.Logger.Level.WARNING,
                "Use of WASIP1 is experimental and will only work for the simplest of use cases.");
    }

    public HostFunction[] toHostFunctions() {
        var fidx = 55;
        var functions = new HostFunction[fidx + 1];

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("adapter_close_badfd: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] { Value.i32(0) };
                        },
                        "wasi_snapshot_preview1",
                        "adapter_close_badfd",
                        List.of(I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("adapter_open_badfd: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] { Value.i32(0) };
                        },
                        "wasi_snapshot_preview1",
                        "adapter_open_badfd",
                        List.of(I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("args_get: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] { Value.i32(0) };
                        },
                        "wasi_snapshot_preview1",
                        "args_get",
                        List.of(I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("args_sizes_get: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] { Value.i32(0) };
                        },
                        "wasi_snapshot_preview1",
                        "args_sizes_get",
                        List.of(I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("clock_res_get: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] { Value.i32(0) };
                        },
                        "wasi_snapshot_preview1",
                        "clock_res_get",
                        List.of(I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("clock_time_get: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] { Value.i32(0) };
                        },
                        "wasi_snapshot_preview1",
                        "clock_time_get",
                        List.of(I32, I64, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("environ_get: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] { Value.i32(0) };
                        },
                        "wasi_snapshot_preview1",
                        "environ_get",
                        List.of(I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("environ_sizes_get: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] { Value.i32(0) };
                        },
                        "wasi_snapshot_preview1",
                        "environ_sizes_get",
                        List.of(I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_advise: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] { Value.i32(0) };
                        },
                        "wasi_snapshot_preview1",
                        "fd_advise",
                        List.of(I32, I64, I64, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_allocate: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] { Value.i32(0) };
                        },
                        "wasi_snapshot_preview1",
                        "fd_allocate",
                        List.of(I32, I64, I64),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_close: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] { Value.i32(0) };
                        },
                        "wasi_snapshot_preview1",
                        "fd_close",
                        List.of(I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_datasync: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] { Value.i32(0) };
                        },
                        "wasi_snapshot_preview1",
                        "fd_datasync",
                        List.of(I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_fdstat_get: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] { Value.i32(0) };
                        },
                        "wasi_snapshot_preview1",
                        "fd_fdstat_get",
                        List.of(I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_fdstat_set_flags: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] { Value.i32(0) };
                        },
                        "wasi_snapshot_preview1",
                        "fd_fdstat_set_flags",
                        List.of(I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_fdstat_set_rights: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] { Value.i32(0) };
                        },
                        "wasi_snapshot_preview1",
                        "fd_fdstat_set_rights",
                        List.of(I32, I64, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_filestat_set_size: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] { Value.i32(0) };
                        },
                        "wasi_snapshot_preview1",
                        "fd_filestat_set_size",
                        List.of(I32, I64),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_filestat_set_times: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] { Value.i32(0) };
                        },
                        "wasi_snapshot_preview1",
                        "fd_filestat_set_times",
                        List.of(I32, I64, I64, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_pread: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] { Value.i32(0) };
                        },
                        "wasi_snapshot_preview1",
                        "fd_pread",
                        List.of(I32, I32, I32, I64, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_prestat_dir_name: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] { Value.i32(0) };
                        },
                        "wasi_snapshot_preview1",
                        "fd_prestat_dir_name",
                        List.of(I32, I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_prestat_get: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] { Value.i32(0) };
                        },
                        "wasi_snapshot_preview1",
                        "fd_prestat_get",
                        List.of(I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_pwrite: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] { Value.i32(0) };
                        },
                        "wasi_snapshot_preview1",
                        "fd_pwrite",
                        List.of(I32, I32, I32, I64, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
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

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_readdir: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "fd_readdir",
                        List.of(I32, I32, I32, I64, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_renumber: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "fd_renumber",
                        List.of(I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_seek: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] {Value.i32(0)};
                        },
                        "wasi_snapshot_preview1",
                        "fd_seek",
                        List.of(I32, I64, I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_sync: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] {Value.i32(0)};
                        },
                        "wasi_snapshot_preview1",
                        "fd_sync",
                        List.of(I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_tell: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] {Value.i32(0)};
                        },
                        "wasi_snapshot_preview1",
                        "fd_tell",
                        List.of(I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
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

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("get_allocation_state: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "get_allocation_state",
                        List.of(),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("get_state_ptr: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "get_state_ptr",
                        List.of(),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("memcpy: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "memcpy",
                        List.of(I32, I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("memcpy: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "memmove",
                        List.of(I32, I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("memset: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "memset",
                        List.of(I32, I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("path_create_directory: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "path_create_directory",
                        List.of(I32, I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("path_filestat_get: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "path_filestat_get",
                        List.of(I32, I32, I32, I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("path_filestat_set_times: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "path_filestat_set_times",
                        List.of(I32, I32, I32, I32, I64, I64, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("path_link: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "path_link",
                        List.of(I32, I32, I32, I32, I32, I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("path_open: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "path_open",
                        List.of(I32, I32, I32, I32, I32, I64, I64, I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("path_readlink: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "path_readlink",
                        List.of(I32, I32, I32, I32, I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("path_remove_directory: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "path_remove_directory",
                        List.of(I32, I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("path_rename: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "path_rename",
                        List.of(I32, I32, I32, I32, I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("path_symlink: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "path_symlink",
                        List.of(I32, I32, I32, I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("path_unlink_file: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "path_unlink_file",
                        List.of(I32, I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("poll_oneoff: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "poll_oneoff",
                        List.of(I32, I32, I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("proc_exit: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "proc_exit",
                        List.of(I32),
                        List.of());

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("proc_exit: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "proc_exit",
                        List.of(I32),
                        List.of());

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("proc_raise: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "proc_raise",
                        List.of(I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("random_get: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "random_get",
                        List.of(I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("reset_adapter_state: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "reset_adapter_state",
                        List.of(),
                        List.of());

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("sched_yield: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "sched_yield",
                        List.of(),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("set_allocation_state: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "set_allocation_state",
                        List.of(I32),
                        List.of());

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("set_state_ptr: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "set_state_ptr",
                        List.of(I32),
                        List.of());

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("sock_accept: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "sock_accept",
                        List.of(I32, I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("sock_recv: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "sock_recv",
                        List.of(I32, I32, I32, I32, I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("sock_send: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "sock_send",
                        List.of(I32, I32, I32, I32, I32),
                        List.of(I32));

        functions[fidx--] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("sock_shutdown: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "sock_shutdown",
                        List.of(I32, I32),
                        List.of(I32));

        // assert fidx == -1;

        return functions;
    }
}
