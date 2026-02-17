package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.InvalidException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class TypeSection extends Section {
    private final List<RecType> types;
    private final SubType[] flattenedSubTypes;

    private TypeSection(List<RecType> types) {
        super(SectionId.TYPE);
        this.types = List.copyOf(types);

        var flatList = new ArrayList<SubType>();
        for (var t : this.types) {
            for (var st : t.subTypes()) {
                flatList.add(st);
            }
        }
        this.flattenedSubTypes = flatList.toArray(new SubType[0]);
    }

    // https://github.com/WebAssembly/gc/blob/main/proposals/gc/MVP.md#type-definitions
    // > the number of type section entries is now the number of recursion groups rather than the
    // number of individual types.
    // types() returns the flattened list of individual types
    public FunctionType[] types() {
        return types.stream()
                .filter(RecType::isLegacy)
                .map(RecType::legacy)
                .toArray(FunctionType[]::new);
    }

    public int typeCount() {
        return types.size();
    }

    public int subTypeCount() {
        return flattenedSubTypes.length;
    }

    public FunctionType getType(int idx) {
        return types.get(idx).legacy();
    }

    public RecType getRecType(int idx) {
        return types.get(idx);
    }

    public SubType getSubType(int idx) {
        if (idx < 0 || idx >= flattenedSubTypes.length) {
            throw new InvalidException("unknown type " + idx);
        }
        return flattenedSubTypes[idx];
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<RecType> types = new ArrayList<>();

        private Builder() {}

        @Deprecated
        public List<FunctionType> getTypes() {
            return types.stream()
                    .filter(RecType::isLegacy)
                    .map(RecType::legacy)
                    .collect(Collectors.toList());
        }

        /**
         * Add a function type definition to this section.
         *
         * @param functionType the function type to add to this section (must not be {@code null})
         * @return the Builder
         */
        public Builder addFunctionType(FunctionType functionType) {
            Objects.requireNonNull(functionType, "functionType");
            var type =
                    RecType.builder()
                            .withSubTypes(
                                    new SubType[] {
                                        SubType.builder()
                                                .withTypeIdx(new int[] {})
                                                .withFinal(true)
                                                .withCompType(
                                                        CompType.builder()
                                                                .withFuncType(functionType)
                                                                .build())
                                                .build()
                                    })
                            .build();
            types.add(type);
            return this;
        }

        public Builder addRecType(RecType recType) {
            Objects.requireNonNull(recType, "recType");
            types.add(recType);
            return this;
        }

        public TypeSection build() {
            return new TypeSection(types);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof TypeSection)) {
            return false;
        }
        TypeSection that = (TypeSection) o;
        return Objects.equals(types, that.types);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(types);
    }
}
