package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.wasm.types.ValueType.*;

import com.dylibso.chicory.wasm.types.Value;
import java.util.List;

public class Wasi {

    public HostFunction[] toHostFunctions() {
        var functions = new HostFunction[5];

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
                            System.out.println("fd_write");
                            for (var arg : args) {
                                System.out.println(arg);
                            }
                            return null;
                        },
                        "wasi_snapshot_preview1",
                        "fd_write",
                        List.of(I32, I32, I32, I32),
                        List.of(I32));

        functions[3] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("fd_seek");
                            for (var arg : args) {
                                System.out.println(arg);
                            }
                            return null;
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

        return functions;
    }
}
