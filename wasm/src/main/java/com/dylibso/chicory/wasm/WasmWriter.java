package com.dylibso.chicory.wasm;

import static com.dylibso.chicory.wasm.Parser.MAGIC_BYTES;
import static com.dylibso.chicory.wasm.Parser.VERSION_BYTES;
import static java.lang.Integer.toUnsignedLong;

import com.dylibso.chicory.wasm.types.RawSection;
import com.dylibso.chicory.wasm.types.SectionId;
import com.dylibso.chicory.wasm.types.UnknownCustomSection;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class WasmWriter {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    public WasmWriter() {
        out.writeBytes(MAGIC_BYTES);
        out.writeBytes(VERSION_BYTES);
    }

    public void writeSection(RawSection section) {
        writeSection(section.sectionId(), section.contents());
    }

    public void writeSection(UnknownCustomSection section) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeName(out, section.name());
        writeSection(section.sectionId(), out.toByteArray(), section.bytes());
    }

    public void writeSection(int sectionId, byte[]... contents) {
        out.write(sectionId);
        int totalLength = 0;
        for (byte[] content : contents) {
            totalLength += content.length;
        }
        writeVarUInt32(out, totalLength);
        for (byte[] content : contents) {
            out.writeBytes(content);
        }
    }

    public void writeEmptyCodeSection() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeVarUInt32(out, 0);
        writeSection(SectionId.CODE, out.toByteArray());
    }

    public byte[] bytes() {
        return out.toByteArray();
    }

    public static void writeVarUInt32(ByteArrayOutputStream out, int value) {
        long x = toUnsignedLong(value);
        while (true) {
            if (x < 0x80) {
                out.write((int) x);
                break;
            }
            out.write((int) ((x & 0x7F) | 0x80));
            x >>= 7;
        }
    }

    public static void writeName(ByteArrayOutputStream out, String name) {
        var bytes = name.getBytes(StandardCharsets.UTF_8);
        writeVarUInt32(out, bytes.length);
        out.writeBytes(bytes);
    }
}
