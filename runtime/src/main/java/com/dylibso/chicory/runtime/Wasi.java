package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.wasm.types.ValueType.*;

import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException;
import com.dylibso.chicory.wasm.types.Value;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

public class Wasi {

    private WasiOptions options;

    public Wasi() {
        this.options = new WasiOptions();
    }

    public Wasi(WasiOptions opts) {
        this.options = opts;
    }

    public HostFunction[] toHostFunctions() {
        var functions = new HostFunction[8];

        functions[0] =
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

        functions[1] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("proc_exit: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                        },
                        "wasi_snapshot_preview1",
                        "proc_exit",
                        List.of(I32),
                        List.of());

        functions[2] =
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

        functions[3] =
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

        functions[4] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_fdfstat_get: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] {Value.i32(0)};
                        },
                        "wasi_snapshot_preview1",
                        "fd_fdstat_get",
                        List.of(I32, I32),
                        List.of(I32));

        functions[5] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("environ_get: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] {Value.i32(0)};
                        },
                        "wasi_snapshot_preview1",
                        "environ_get",
                        List.of(I32, I32, I32, I32),
                        List.of(I32));

        functions[6] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("environ_sizes_get: " + Arrays.toString(args));
                            throw new WASMRuntimeException("We don't yet support this WASI call");
                            // return new Value[] {Value.i32(0)};
                        },
                        "wasi_snapshot_preview1",
                        "environ_sizes_get",
                        List.of(I32, I32),
                        List.of(I32));

        functions[7] =
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

        return functions;
    }
}
