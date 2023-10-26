package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.Parser;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class NameSection extends CustomSection {

    private List<String> funcNames;

    public NameSection(CustomSection sec) {
        super(sec.getSectionId(), sec.getSectionSize());
        this.setBytes(sec.getBytes());
        funcNames = parseFunctionNames();
    }

    private List<String> parseFunctionNames() {
        ByteBuffer buf = ByteBuffer.wrap(this.getBytes());

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

        return names;
    }

    public List<String> getFunctionNames() {
        return funcNames;
    }
}
