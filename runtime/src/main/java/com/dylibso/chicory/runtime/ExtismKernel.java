package com.dylibso.chicory.runtime;

import static com.dylibso.chicory.wasm.types.Value.*;

import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.io.File;
import java.util.HashMap;
import java.util.List;

public class ExtismKernel {
    private static final String KERNEL_PATH = "/Users/ben/dylibso/extism/runtime/src/extism-runtime.wasm";
    private static final String IMPORT_MODULE_NAME = "extism:host/env";
    private Instance kernel;
    private Memory memory;
    private final ExportFunction alloc;
    private final ExportFunction free;
    private final ExportFunction length;
    private final ExportFunction loadU8;
    private final ExportFunction loadU64;
    private final ExportFunction inputLoadU8;
    private final ExportFunction inputLoadU64;
    private final ExportFunction storeU8;
    private final ExportFunction storeU64;
    private final ExportFunction inputSet;
    private final ExportFunction inputLen;
    private final ExportFunction inputOffset;
    private final ExportFunction outputLen;
    private final ExportFunction outputOffset;
    private final ExportFunction outputSet;
    private final ExportFunction reset;
    private final ExportFunction errorSet;
    private final ExportFunction errorGet;
    private final ExportFunction memoryBytes;

    public ExtismKernel() {
        kernel = Module.build(new File(KERNEL_PATH)).instantiate();
        memory = kernel.getMemory();
        alloc = kernel.getExport("alloc");
        free = kernel.getExport("free");
        length = kernel.getExport("length");
        loadU8 = kernel.getExport("load_u8");
        loadU64 = kernel.getExport("load_u64");
        inputLoadU8 = kernel.getExport("input_load_u8");
        inputLoadU64 = kernel.getExport("input_load_u64");
        storeU8 = kernel.getExport("store_u8");
        storeU64 = kernel.getExport("store_u64");
        inputSet = kernel.getExport("input_set");
        inputLen = kernel.getExport("input_length");
        inputOffset = kernel.getExport("input_offset");
        outputLen = kernel.getExport("output_length");
        outputOffset = kernel.getExport("output_offset");
        outputSet = kernel.getExport("output_set");
        reset = kernel.getExport("reset");
        errorSet = kernel.getExport("error_set");
        errorGet = kernel.getExport("error_get");
        memoryBytes = kernel.getExport("memory_bytes");
    }

    public void setInput(byte[] input) {
        var ptr = alloc.apply(i64(input.length))[0];
        memory.put(ptr.asInt(), input);
        inputSet.apply(ptr, i64(input.length));
    }

    public byte[] getOutput() {
        var ptr = outputOffset.apply()[0];
        var len = outputLen.apply()[0];
        return memory.getBytes(ptr.asInt(), len.asInt());
    }

    public HostFunction[] toHostFunctions() {
        var hostFunctions = new HostFunction[22];
        int count = 0;

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("Alloc " + args);
                            return alloc.apply(args);
                        },
                        IMPORT_MODULE_NAME,
                        "alloc",
                        List.of(ValueType.I64),
                        List.of(ValueType.I64));

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("Free " + args);
                            return free.apply(args);
                        },
                        IMPORT_MODULE_NAME,
                        "free",
                        List.of(ValueType.I64),
                        List.of());

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("Length " + args);
                            return length.apply(args);
                        },
                        IMPORT_MODULE_NAME,
                        "length",
                        List.of(ValueType.I64),
                        List.of(ValueType.I64));

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("LoadU8 " + args);
                            return loadU8.apply(args);
                        },
                        IMPORT_MODULE_NAME,
                        "load_u8",
                        List.of(ValueType.I64),
                        List.of(ValueType.I32));

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("LoadU64 " + args);
                            return loadU64.apply(args);
                        },
                        IMPORT_MODULE_NAME,
                        "load_u64",
                        List.of(ValueType.I64),
                        List.of(ValueType.I64));

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("InputLoadU8 " + args);
                            return inputLoadU8.apply(args);
                        },
                        IMPORT_MODULE_NAME,
                        "input_load_u8",
                        List.of(ValueType.I64),
                        List.of(ValueType.I32));

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("InputLoadU64 " + args);
                            return inputLoadU64.apply(args);
                        },
                        IMPORT_MODULE_NAME,
                        "input_load_u64",
                        List.of(ValueType.I64),
                        List.of(ValueType.I64));

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("StoreU8 " + args);
                            return storeU8.apply(args);
                        },
                        IMPORT_MODULE_NAME,
                        "store_u8",
                        List.of(ValueType.I64, ValueType.I32),
                        List.of());

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("StoreU64 " + args);
                            return storeU64.apply(args);
                        },
                        IMPORT_MODULE_NAME,
                        "store_u64",
                        List.of(ValueType.I64, ValueType.I64),
                        List.of());

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("InputSet " + args);
                            return inputSet.apply(args);
                        },
                        IMPORT_MODULE_NAME,
                        "input_set",
                        List.of(ValueType.I64, ValueType.I64),
                        List.of());

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("InputLen " + args);
                            return inputLen.apply(args);
                        },
                        IMPORT_MODULE_NAME,
                        "input_length",
                        List.of(),
                        List.of(ValueType.I64));

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("InputOffset " + args);
                            return inputOffset.apply(args);
                        },
                        IMPORT_MODULE_NAME,
                        "input_offset",
                        List.of(),
                        List.of(ValueType.I64));

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("OutputSet " + args);
                            return outputSet.apply(args);
                        },
                        IMPORT_MODULE_NAME,
                        "output_set",
                        List.of(),
                        List.of(ValueType.I64));

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("OutputLen " + args);
                            return outputLen.apply(args);
                        },
                        IMPORT_MODULE_NAME,
                        "output_length",
                        List.of(),
                        List.of(ValueType.I64));

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("OutputOffset " + args);
                            return outputOffset.apply(args);
                        },
                        IMPORT_MODULE_NAME,
                        "output_offset",
                        List.of(),
                        List.of(ValueType.I64));

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("Reset " + args);
                            return reset.apply(args);
                        },
                        IMPORT_MODULE_NAME,
                        "reset",
                        List.of(),
                        List.of());

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("ErrorSet " + args);
                            return errorSet.apply(args);
                        },
                        IMPORT_MODULE_NAME,
                        "error_set",
                        List.of(ValueType.I64),
                        List.of());

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("ErrorGet " + args);
                            return errorGet.apply(args);
                        },
                        IMPORT_MODULE_NAME,
                        "error_get",
                        List.of(),
                        List.of(ValueType.I64));

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("MemoryBytes " + args);
                            return memoryBytes.apply(args);
                        },
                        IMPORT_MODULE_NAME,
                        "memory_bytes",
                        List.of(),
                        List.of(ValueType.I64));

        var vars = new HashMap<String, byte[]>();

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("_var_get " + args);
                            //                    var keyLen = Length.apply(args[0])[0];
                            //                    var key = memory.getString(args[0].asInt(),
                            // keyLen.asInt());
                            //                    var value = vars.get(key);
                            return new Value[] {i64(0)};
                        },
                        IMPORT_MODULE_NAME,
                        "var_get",
                        List.of(ValueType.I64, ValueType.I64),
                        List.of(ValueType.I64));

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("_var_set" + args);
                            //                    var keyLen = Length.apply(args[0])[0];
                            //                    var key = memory.getString(args[0].asInt(),
                            // keyLen.asInt());
                            //                    var value = vars.get(key);
                            return null;
                        },
                        IMPORT_MODULE_NAME,
                        "var_set",
                        List.of(ValueType.I64, ValueType.I64),
                        List.of());

        hostFunctions[count++] =
                new HostFunction(
                        (Memory memory, Value... args) -> {
                            // System.out.println("_config_get" + args);
                            //                    var keyLen = Length.apply(args[0])[0];
                            //                    var key = memory.getString(args[0].asInt(),
                            // keyLen.asInt());
                            //                    var value = vars.get(key);
                            return new Value[] {i64(0)};
                        },
                        IMPORT_MODULE_NAME,
                        "config_get",
                        List.of(ValueType.I64),
                        List.of(ValueType.I64));

        return hostFunctions;
    }
}
