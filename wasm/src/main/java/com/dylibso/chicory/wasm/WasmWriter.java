package com.dylibso.chicory.wasm;

import static com.dylibso.chicory.wasm.Parser.MAGIC_BYTES;
import static com.dylibso.chicory.wasm.Parser.VERSION_BYTES;
import static java.lang.Integer.toUnsignedLong;

import com.dylibso.chicory.wasm.types.RawSection;
import java.io.ByteArrayOutputStream;

/** Utility class to write Wasm modules. */
public final class WasmWriter {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    /** Creates a new WasmWriter. */
    public WasmWriter() {
        out.writeBytes(MAGIC_BYTES);
        out.writeBytes(VERSION_BYTES);
    }

    /**
     * Writes a section.
     *
     * @param section the section to write
     */
    public void writeSection(RawSection section) {
        writeSection(section.sectionId(), section.contents());
    }

    /**
     * Writes a section with the given ID and contents.
     *
     * @param sectionId the ID of the section
     * @param contents the byte contents of the section
     */
    public void writeSection(int sectionId, byte[] contents) {
        out.write(sectionId);
        writeVarUInt32(out, contents.length);
        out.writeBytes(contents);
    }

    /**
     * Returns the written Wasm module as a byte array.
     *
     * @return the byte array representing the Wasm module
     */
    public byte[] bytes() {
        return out.toByteArray();
    }

    /**
     * Writes a variable-length unsigned 32-bit integer (VarUInt32) to the output stream.
     *
     * @param out the output stream to write to
     * @param value the integer value to write
     */
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
