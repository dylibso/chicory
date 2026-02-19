package com.dylibso.chicory.wasm;

import static com.dylibso.chicory.wasm.Parser.MAGIC_BYTES;
import static com.dylibso.chicory.wasm.Parser.VERSION_BYTES;
import static java.lang.Integer.toUnsignedLong;

import com.dylibso.chicory.wasm.types.RawSection;
import java.io.ByteArrayOutputStream;

public final class WasmWriter {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    public WasmWriter() {
        out.writeBytes(MAGIC_BYTES);
        out.writeBytes(VERSION_BYTES);
    }

    public void writeSection(RawSection section) {
        writeSection(section.sectionId(), section.contents());
    }

    public void writeSection(int sectionId, byte[] contents) {
        out.write(sectionId);
        writeVarUInt32(out, contents.length);
        out.writeBytes(contents);
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
}
