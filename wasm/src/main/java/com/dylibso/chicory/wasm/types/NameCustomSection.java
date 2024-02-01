package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.Parser;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;

/**
 * The "name" custom section.
 */
public class NameCustomSection extends CustomSection {

    private final ArrayList<String> funcNames;

    /**
     * Construct a new, empty section instance.
     */
    public NameCustomSection() {
        super();
        funcNames = new ArrayList<>();
    }

    /**
     * Construct a new instance.
     *
     * @param bytes the byte content of the section
     */
    public NameCustomSection(final byte[] bytes) {
        super();
        funcNames = parseFunctionNames(bytes);
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

    /**
     * Add a function name to this section.
     *
     * @param functionName the function name to add to this section (must not be {@code null})
     * @return the index of the newly-added function name
     */
    public int addFunctionName(String functionName) {
        Objects.requireNonNull(functionName, "functionName");
        int idx = funcNames.size();
        funcNames.add(functionName);
        return idx;
    }

    public int functionNameCount() {
        return funcNames.size();
    }

    public String getFunctionName(int idx) {
        return funcNames.get(idx);
    }
}
