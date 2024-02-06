package com.dylibso.chicory.fuzz;

import java.util.Set;
import java.util.stream.Collectors;

public class InstructionTypes {

    private final Set<InstructionType> types;

    public InstructionTypes(Set<InstructionType> types) {
        this.types = types;
    }

    public InstructionTypes(InstructionType... types) {
        this.types = Set.of(types);
    }

    @Override
    public String toString() {
        // TODO: NOT TESTED!
        return types.stream().map(x -> x.value()).collect(Collectors.joining(","));
    }
}
