package com.dylibso.chicory.fuzz;

import java.util.Arrays;
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

    // Extract the types from an env var
    public static InstructionTypes fromString(String values) {
        return new InstructionTypes(
                Arrays.stream(values.trim().split(","))
                        .map(
                                v -> {
                                    var res = InstructionType.byValue(v.toLowerCase());
                                    if (res == null) {
                                        throw new RuntimeException(
                                                "Cannot find a matching type for " + v);
                                    }
                                    return res;
                                })
                        .collect(Collectors.toSet()));
    }
}
