package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.Objects;

public class CodeSection extends Section {
    private final ArrayList<FunctionBody> functionBodies;

    /**
     * Construct a new, empty section instance.
     */
    public CodeSection() {
        this(new ArrayList<>());
    }

    /**
     * Construct a new, empty section instance.
     *
     * @param estimatedSize the estimated number of functions to reserve space for
     */
    public CodeSection(int estimatedSize) {
        this(new ArrayList<>(estimatedSize));
    }

    private CodeSection(ArrayList<FunctionBody> functionBodies) {
        super(SectionId.CODE);
        this.functionBodies = functionBodies;
    }

    public FunctionBody[] functionBodies() {
        return functionBodies.toArray(FunctionBody[]::new);
    }

    public int functionBodyCount() {
        return functionBodies.size();
    }

    public FunctionBody getFunctionBody(int idx) {
        return functionBodies.get(idx);
    }

    /**
     * Add a function body to this section.
     *
     * @param functionBody the function body to add to this section (must not be {@code null})
     * @return the index of the newly-added function body
     */
    public int addFunctionBody(FunctionBody functionBody) {
        Objects.requireNonNull(functionBody, "functionBody");
        int idx = functionBodies.size();
        functionBodies.add(functionBody);
        return idx;
    }
}
