package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.wasm.types.ValueType.*;

import com.dylibso.chicory.wasm.types.Value;

import java.io.IOException;
import java.util.List;

public class Wasi {
    public Wasi() {}

    public HostFunction[] toHostFunctions() {
        var functions = new HostFunction[7];

        functions[0] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_close");
                            for (var arg : args) {
                                System.out.println(arg);
                            }
                            return null;
                        },
                        "wasi_snapshot_preview1",
                        "fd_close",
                        List.of(I32),
                        List.of(I32));

        functions[1] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("proc_exit");
                            for (var arg : args) {
                                System.out.println(arg);
                            }
                            return null;
                        },
                        "wasi_snapshot_preview1",
                        "proc_exit",
                        List.of(I32),
                        List.of());

        functions[2] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            var fd = args[0].asInt();
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
                                    System.out.write(bytes);
                                } catch (IOException e) {
                                    // TODO how to signal to the guest
                                    throw new RuntimeException(e);
                                }
                                bytesWritten += iovLen;
                            }
                            System.out.flush(); // flush it
                            memory.write(retPtr, Value.i32(bytesWritten));
                            return new Value[] { Value.i32(0) } ;
                        },
                        "wasi_snapshot_preview1",
                        "fd_write",
                        List.of(I32, I32, I32, I32),
                        List.of(I32));

        functions[3] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            var fd = args[0];
                            var offset = args[1];
                            var whence = args[2];
                            var ptr = args[3];
                            System.out.println("fd_seek");
                            for (var arg : args) {
                                System.out.println(arg);
                            }
                            memory.write(ptr.asInt(), Value.i32(100));
                            return new Value[] {Value.i32(0)};
                        },
                        "wasi_snapshot_preview1",
                        "fd_seek",
                        List.of(I32, I64, I32, I32),
                        List.of(I32));

        functions[4] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_fdstat_get");
                            for (var arg : args) {
                                System.out.println(arg);
                            }
                            return null;
                        },
                        "wasi_snapshot_preview1",
                        "fd_fdstat_get",
                        List.of(I32, I32),
                        List.of(I32));

        functions[5] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("environ_get");
                            for (var arg : args) {
                                System.out.println(arg);
                            }
                            return null;
                        },
                        "wasi_snapshot_preview1",
                        "environ_get",
                        List.of(I32, I32, I32, I32),
                        List.of(I32));

        functions[6] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("environ_get");
                            for (var arg : args) {
                                System.out.println(arg);
                            }
                            return null;
                        },
                        "wasi_snapshot_preview1",
                        "environ_sizes_get",
                        List.of(I32, I32),
                        List.of(I32));

        return functions;
    }
}
