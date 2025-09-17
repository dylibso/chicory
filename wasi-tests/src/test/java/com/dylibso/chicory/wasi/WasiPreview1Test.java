package com.dylibso.chicory.wasi;

import static com.dylibso.chicory.wasi.Files.copyDirectory;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createDirectory;
import static java.nio.file.Files.createSymbolicLink;
import static java.nio.file.Files.writeString;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.runtime.ByteBufferMemory;
import com.dylibso.chicory.wasm.types.MemoryLimits;
import io.roastedroot.zerofs.Configuration;
import io.roastedroot.zerofs.ZeroFs;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
public class WasiPreview1Test {

    @Test
    public void wasiRandom() {
        var seed = 0x12345678;
        var wasi =
                WasiPreview1.builder()
                        .withOptions(WasiOptions.builder().withRandom(new Random(seed)).build())
                        .build();

        var memory = new ByteBufferMemory(new MemoryLimits(8, 8));
        assertEquals(0, wasi.randomGet(memory, 0, 123_456));
        assertEquals(0, wasi.randomGet(memory, 222_222, 87_654));

        var random = new Random(seed);
        byte[] first = new byte[123_456];
        random.nextBytes(first);
        byte[] second = new byte[87_654];
        random.nextBytes(second);

        assertArrayEquals(first, memory.readBytes(0, 123_456));
        assertArrayEquals(second, memory.readBytes(222_222, 87_654));
    }

    @Test
    public void wasiPositionedWriteWithAppendShouldFail() throws IOException {
        try (var fs = newZeroFs()) {
            var dir = "fs-tests.dir";
            Path source =
                    new File("../wasi-testsuite/tests/c/testsuite/wasm32-wasip1")
                            .toPath()
                            .resolve(dir);
            Path target = fs.getPath(dir);
            copyDirectory(source, target);

            try (var wasi = wasiWithDirectory(target.toString(), target)) {
                var memory = new ByteBufferMemory(new MemoryLimits(1));

                int fdPtr = 0;
                int result =
                        wasi.pathOpen(
                                memory,
                                3,
                                WasiLookupFlags.SYMLINK_FOLLOW,
                                "pwrite.cleanup",
                                WasiOpenFlags.CREAT,
                                0,
                                0,
                                WasiFdFlags.APPEND,
                                fdPtr);
                assertEquals(WasiErrno.ESUCCESS.value(), result);

                int fd = memory.readInt(fdPtr);
                assertEquals(4, fd);

                result = wasi.fdPwrite(memory, fd, 0, 0, 0, 0);
                assertEquals(WasiErrno.ENOTSUP.value(), result);
            }
        }
    }

    @Test
    public void wasiReadLink() throws IOException {
        try (var fs = newZeroFs()) {
            Path root = fs.getPath("test");
            createDirectory(root);
            createSymbolicLink(root.resolve("abc"), fs.getPath("xyz"));
            try (var wasi = wasiWithDirectory(root.toString(), root)) {
                var memory = new ByteBufferMemory(new MemoryLimits(1));
                int bufPtr = 0;
                int bufUsedPtr = 16;

                int result = wasi.pathReadlink(memory, 3, "abc", bufPtr, 16, bufUsedPtr);
                assertEquals(WasiErrno.ESUCCESS.value(), result);

                int length = memory.readInt(bufUsedPtr);
                assertEquals(3, length);
                String name = memory.readString(bufPtr, length);
                assertEquals("xyz", name);
            }
        }
    }

    @Test
    public void wasiPollOneoffNoSubscriptions() {
        var wasi = WasiPreview1.builder().build();
        var memory = new ByteBufferMemory(new MemoryLimits(0));
        int result = wasi.pollOneoff(memory, 0, 0, 0, 0);
        assertEquals(WasiErrno.EINVAL.value(), result);
    }

    @Test
    public void wasiPollOneoffStdinNoData() {
        var stdin = new ByteArrayInputStream("".getBytes(UTF_8));
        var wasiOpts = WasiOptions.builder().withStdin(stdin).build();
        var wasi = WasiPreview1.builder().withOptions(wasiOpts).build();
        var memory = new ByteBufferMemory(new MemoryLimits(1));
        int nsubscriptions = 2;
        int neventsPtr = 0;
        int inPtr = 4;
        int outPtr = inPtr + nsubscriptions * 48;
        int in = inPtr;
        memory.writeLong(in, 0x8888_7777_6666_5555L); // userdata
        memory.writeByte(in + 8, WasiEventType.CLOCK);
        memory.writeI32(in + 16, WasiClockId.REALTIME);
        memory.writeLong(in + 16 + 8, MILLISECONDS.toNanos(200));
        memory.writeLong(in + 16 + 16, 0); // precision
        memory.writeShort(in + 16 + 24, (short) 0);
        in += 48;
        memory.writeLong(in, 0xAAAA_BBBB_CCCC_DDDDL); // userdata
        memory.writeByte(in + 8, WasiEventType.FD_READ);
        memory.writeI32(in + 16, 0); // fd
        int result = wasi.pollOneoff(memory, inPtr, outPtr, nsubscriptions, neventsPtr);
        assertEquals(WasiErrno.ESUCCESS.value(), result);
        assertEquals(1, memory.readInt(neventsPtr));
        assertEquals(0x8888_7777_6666_5555L, memory.readLong(outPtr));
        assertEquals(WasiErrno.ESUCCESS.value(), memory.readShort(outPtr + 8));
        assertEquals(WasiEventType.CLOCK, memory.read(outPtr + 10));
    }

    @Test
    public void wasPollOneoffStdinWithData() {
        var stdin = new ByteArrayInputStream("Hello, World!".getBytes(UTF_8));
        var wasiOpts = WasiOptions.builder().withStdin(stdin).build();
        var wasi = WasiPreview1.builder().withOptions(wasiOpts).build();
        var memory = new ByteBufferMemory(new MemoryLimits(1));
        int nsubscriptions = 1;
        int neventsPtr = 0;
        int inPtr = 4;
        int outPtr = inPtr + nsubscriptions * 48;
        int in = inPtr;
        memory.writeLong(in, 0x8888_7777_6666_5555L); // userdata
        memory.writeByte(in + 8, WasiEventType.FD_READ);
        memory.writeI32(in + 16, 0); // fd
        int result = wasi.pollOneoff(memory, inPtr, outPtr, nsubscriptions, neventsPtr);
        assertEquals(WasiErrno.ESUCCESS.value(), result);
        assertEquals(1, memory.readInt(neventsPtr));
        assertEquals(0x8888_7777_6666_5555L, memory.readLong(outPtr));
        assertEquals(WasiErrno.ESUCCESS.value(), memory.readShort(outPtr + 8));
        assertEquals(WasiEventType.FD_READ, memory.read(outPtr + 10));
    }

    @Test
    public void wasiPollOneoffRegularFile() {
        try (var fs = newZeroFs()) {
            Path root = fs.getPath("test");
            createDirectory(root);
            var file = root.resolve("hello.txt");
            writeString(file, "Hello, World!");
            try (var wasi = wasiWithDirectory(root.toString(), root)) {
                var memory = new ByteBufferMemory(new MemoryLimits(1));
                int fdPtr = 1024;
                int result = wasi.pathOpen(memory, 3, 0, "hello.txt", 0, 0, 0, 0, fdPtr);
                assertEquals(WasiErrno.ESUCCESS.value(), result);
                int fd = memory.readInt(fdPtr);
                assertEquals(4, fd);

                int nsubscriptions = 2;
                int neventsPtr = 0;
                int inPtr = 4;
                int outPtr = inPtr + nsubscriptions * 48;
                int in = inPtr;
                memory.writeLong(in, 0x8888_7777_6666_5555L); // userdata
                memory.writeByte(in + 8, WasiEventType.FD_READ);
                memory.writeI32(in + 16, fd);
                in += 48;
                memory.writeLong(in, 0xAAAA_BBBB_CCCC_DDDDL); // userdata
                memory.writeByte(in + 8, WasiEventType.FD_WRITE);
                memory.writeI32(in + 16, fd);
                result = wasi.pollOneoff(memory, inPtr, outPtr, nsubscriptions, neventsPtr);
                assertEquals(WasiErrno.ESUCCESS.value(), result);
                assertEquals(2, memory.readInt(neventsPtr));
                int out = outPtr;
                assertEquals(0x8888_7777_6666_5555L, memory.readLong(out));
                assertEquals(WasiErrno.ESUCCESS.value(), memory.readShort(out + 8));
                assertEquals(WasiEventType.FD_READ, memory.read(out + 10));
                out += 32;
                assertEquals(0xAAAA_BBBB_CCCC_DDDDL, memory.readLong(out));
                assertEquals(WasiErrno.ESUCCESS.value(), memory.readShort(out + 8));
                assertEquals(WasiEventType.FD_WRITE, memory.read(out + 10));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void wasiPollOneoffClockAbstime() {
        long deadline = System.nanoTime() + MILLISECONDS.toNanos(25);
        var wasi = WasiPreview1.builder().build();
        var memory = new ByteBufferMemory(new MemoryLimits(1));
        int nsubscriptions = 1;
        int neventsPtr = 0;
        int inPtr = 4;
        int outPtr = inPtr + nsubscriptions * 48;
        int in = inPtr;
        memory.writeLong(in, 0x8888_7777_6666_5555L); // userdata
        memory.writeByte(in + 8, WasiEventType.CLOCK);
        memory.writeI32(in + 16, WasiClockId.MONOTONIC);
        memory.writeLong(in + 16 + 8, deadline);
        memory.writeLong(in + 16 + 16, 0); // precision
        memory.writeShort(in + 16 + 24, (short) WasiSubClockFlags.SUBSCRIPTION_CLOCK_ABSTIME);
        int result = wasi.pollOneoff(memory, inPtr, outPtr, nsubscriptions, neventsPtr);
        assertEquals(WasiErrno.ESUCCESS.value(), result);
        assertEquals(1, memory.readInt(neventsPtr));
        assertEquals(0x8888_7777_6666_5555L, memory.readLong(outPtr));
        assertEquals(WasiErrno.ESUCCESS.value(), memory.readShort(outPtr + 8));
        assertEquals(WasiEventType.CLOCK, memory.read(outPtr + 10));
        assertTrue(System.nanoTime() >= deadline);
    }

    private static FileSystem newZeroFs() {
        return ZeroFs.newFileSystem(
                Configuration.unix().toBuilder().setAttributeViews("unix").build());
    }

    private static WasiPreview1 wasiWithDirectory(String guest, Path host) {
        var options = WasiOptions.builder().withDirectory(guest, host).build();
        return WasiPreview1.builder().withOptions(options).build();
    }
}
