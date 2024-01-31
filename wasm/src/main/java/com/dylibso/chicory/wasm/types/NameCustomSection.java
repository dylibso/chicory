package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.Parser;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * The "name" custom section.
 */
public class NameCustomSection extends CustomSection {

    private final ArrayList<String> funcNames;

    private NameCustomSection(final byte[] bytes) {
        super();
        funcNames = parseFunctionNames(bytes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String name() {
        return "name";
    }

    private ArrayList<String> parseFunctionNames(final byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);

        // Expecting function name subsection
        if (buf.get() != 1) {
            throw new IllegalArgumentException("Not a function name subsection");
        }

        // Skip the subsection length
        Parser.readVarUInt32(buf);

        // Decode name map length
        long nameMapLength = Parser.readVarUInt32(buf);

        ArrayList<String> names = new ArrayList<>(Math.toIntExact(nameMapLength));

        for (int i = 0; i < nameMapLength; i++) {
            // Skip function index
            Parser.readVarUInt32(buf);
            // Decode function name
            names.add(Parser.readName(buf));
        }

        return names;
    }

    public int functionNameCount() {
        return funcNames.size();
    }

    public String getFunctionName(int idx) {
        return funcNames.get(idx);
    }

    public static final class Builder implements CustomSection.Builder {
        private byte[] bytes;

        private Builder() {}

        public Builder withBytes(byte[] bytes) {
            this.bytes = bytes;
            return this;
        }

        public CustomSection build() {
            return new NameCustomSection(bytes);
        }
    }
}
