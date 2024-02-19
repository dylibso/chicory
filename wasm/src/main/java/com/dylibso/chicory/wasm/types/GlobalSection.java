package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GlobalSection extends Section {
    private final ArrayList<Global> globals;

    /**
     * Construct a new, empty section instance.
     */
    public GlobalSection() {
        this(new ArrayList<>());
    }

    /**
     * Construct a new, empty section instance.
     *
     * @param estimatedSize the estimated number of globals to reserve space for
     */
    public GlobalSection(int estimatedSize) {
        this(new ArrayList<>(estimatedSize));
    }

    private GlobalSection(ArrayList<Global> globals) {
        super(SectionId.GLOBAL);
        this.globals = globals;
    }

    public Global[] globals() {
        return globals.toArray(Global[]::new);
    }

    public int globalCount() {
        return globals.size();
    }

    public Global getGlobal(int idx) {
        return globals.get(idx);
    }

    /**
     * Add a global variable definition to this section.
     *
     * @param global the global to add to this section (must not be {@code null})
     * @return the index of the newly-added global
     */
    public int addGlobal(Global global) {
        Objects.requireNonNull(global, "global");
        int idx = globals.size();
        globals.add(global);
        return idx;
    }

    public void readFrom(final WasmInputStream in) throws WasmIOException {
        var globalCount = in.u31();
        globals.ensureCapacity(globals.size() + globalCount);

        // Parse individual globals
        for (int i = 0; i < globalCount; i++) {
            var valueType = ValueType.forId(in.u8());
            var mutabilityType = MutabilityType.forId(in.u8());
            var init = Parser.parseExpression(in);
            addGlobal(new Global(valueType, mutabilityType, List.of(init)));
        }
    }
}
