package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.Parser;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The "name" custom section.
 */
public class NameCustomSection extends CustomSection {

    private final List<String> funcNames;

    /**
     * Construct a new instance.
     *
     * @param size the size of the section
     * @param bytes the byte content of the section
     */
    public NameCustomSection(final long size, final byte[] bytes) {
        super(size);
        funcNames = parseFunctionNames(bytes);
    }

    public String name() {
        return "name";
    }

    private List<String> parseFunctionNames(final byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);

        List<String> names = new ArrayList<>();

        // Expecting function name subsection
        if (buf.get() != 1) {
            throw new IllegalArgumentException("Not a function name subsection");
        }

        // Skip the subsection length
        Parser.readVarUInt32(buf);

        // Decode name map length
        long nameMapLength = Parser.readVarUInt32(buf);
        for (int i = 0; i < nameMapLength; i++) {
            // Skip function index
            Parser.readVarUInt32(buf);
            // Decode function name
            names.add(Parser.readName(buf));
        }

        return Collections.unmodifiableList(names);
    }

    public List<String> functionNames() {
        return funcNames;
    }
}
