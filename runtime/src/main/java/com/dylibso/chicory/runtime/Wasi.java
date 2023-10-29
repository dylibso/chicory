package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.wasm.types.ValueType.I32;

import com.dylibso.chicory.wasm.types.Value;
import java.util.List;

public class Wasi {

    public HostFunction[] toHostFunctions() {
        var functions = new HostFunction[4];

        functions[0] =
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
                        List.of(I32, I32),
                        List.of(I32));

        functions[1] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            System.out.println("environ_sizes_get");
                            for (var arg : args) {
                                System.out.println(arg);
                            }
                            return null;
                        },
                        "wasi_snapshot_preview1",
                        "environ_sizes_get",
                        List.of(I32, I32),
                        List.of(I32));

        functions[2] =
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

        functions[3] =
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

        return functions;
    }
}
