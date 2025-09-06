package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TypeSection extends Section {
    private final List<RecType> types;
    private final List<FunctionType> funcTypes;

    private TypeSection(List<RecType> recTypes) {
        super(SectionId.TYPE);
        List<FunctionType> funcs = new ArrayList<>();
        // TODO: this should be simplified ...
        // doing it this way to keep a bit of compatibility and progress more incrementally
        for (var t : recTypes) {
            if (t instanceof RecursiveType) {
                var recType = (RecursiveType) t;
                if (recType.getSubType() != null) {
                    var subType = (SubType) recType.getSubType();
                    if (subType.getCompType() != null) {
                        var compType = (CompType) subType.getCompType();
                        if (compType.getFunctionType() != null) {
                            funcs.add(compType.getFunctionType());
                        }
                    }
                }
            }
        }
        this.types = List.copyOf(recTypes);
        this.funcTypes = List.copyOf(funcs);
    }

    public FunctionType[] types() {
        return funcTypes.toArray(new FunctionType[0]);
    }

    // TODO: decide if we want to keep backward compat or not ...
    public int rawTypesCount() {
        return types.size();
    }

    public RecType getRawType(int idx) {
        return types.get(idx);
    }

    public int typeCount() {
        return funcTypes.size();
    }

    public FunctionType getType(int idx) {
        return funcTypes.get(idx);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<RecType> types = new ArrayList<>();

        private Builder() {}

        public List<RecType> getTypes() {
            return types;
        }

        public Builder addRecType(RecType recType) {
            Objects.requireNonNull(recType, "recType");
            types.add(recType);
            return this;
        }

        // TODO: restore me if we want compatibility
        //        /**
        //         * Add a function type definition to this section.
        //         *
        //         * @param functionType the function type to add to this section (must not be
        // {@code null})
        //         * @return the Builder
        //         */
        //        public Builder addFunctionType(FunctionType functionType) {
        //            Objects.requireNonNull(functionType, "functionType");
        //            types.add(functionType);
        //            return this;
        //        }

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
        return Objects.equals(funcTypes, that.funcTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(funcTypes);
    }
}
