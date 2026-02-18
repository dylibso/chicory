package com.dylibso.chicory.wasm.types;

import com.dylibso.chicory.wasm.InvalidException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class TypeSection extends Section {
    private final List<RecType> types;
    private final SubType[] flattenedSubTypes;
    // For each flat index, the base index and size of its rec group
    private final int[] recGroupBase;
    private final int[] recGroupSize;

    private TypeSection(List<RecType> types) {
        super(SectionId.TYPE);
        this.types = List.copyOf(types);

        var flatList = new ArrayList<SubType>();
        var baseList = new ArrayList<Integer>();
        var sizeList = new ArrayList<Integer>();
        int base = 0;
        for (var t : this.types) {
            int groupSize = t.subTypes().length;
            for (var st : t.subTypes()) {
                flatList.add(st);
                baseList.add(base);
                sizeList.add(groupSize);
            }
            base += groupSize;
        }
        this.flattenedSubTypes = flatList.toArray(new SubType[0]);
        this.recGroupBase = baseList.stream().mapToInt(Integer::intValue).toArray();
        this.recGroupSize = sizeList.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Check if two type indices refer to canonically equivalent types.
     * This considers structural equivalence by replacing within-group
     * references with relative offsets (roll canonicalization).
     */
    public boolean canonicallyEquivalent(int idx1, int idx2) {
        if (idx1 == idx2) {
            return true;
        }
        if (idx1 < 0
                || idx1 >= flattenedSubTypes.length
                || idx2 < 0
                || idx2 >= flattenedSubTypes.length) {
            return false;
        }
        int base1 = recGroupBase[idx1];
        int size1 = recGroupSize[idx1];
        int base2 = recGroupBase[idx2];
        int size2 = recGroupSize[idx2];
        // Rec groups must have the same size
        if (size1 != size2) {
            return false;
        }
        // Position within group must be the same
        if (idx1 - base1 != idx2 - base2) {
            return false;
        }
        // All types in both rec groups must be pairwise equivalent
        // after substituting within-group refs with relative offsets
        for (int i = 0; i < size1; i++) {
            if (!subtypeCanonicallyEquals(
                    flattenedSubTypes[base1 + i],
                    base1,
                    size1,
                    flattenedSubTypes[base2 + i],
                    base2,
                    size2)) {
                return false;
            }
        }
        return true;
    }

    private boolean subtypeCanonicallyEquals(
            SubType st1, int base1, int size1, SubType st2, int base2, int size2) {
        if (st1.isFinal() != st2.isFinal()) {
            return false;
        }
        int[] supers1 = st1.typeIdx();
        int[] supers2 = st2.typeIdx();
        if (supers1.length != supers2.length) {
            return false;
        }
        for (int i = 0; i < supers1.length; i++) {
            if (!refIdxCanonicallyEquals(supers1[i], base1, size1, supers2[i], base2, size2)) {
                return false;
            }
        }
        return compTypeCanonicallyEquals(
                st1.compType(), base1, size1, st2.compType(), base2, size2);
    }

    private boolean compTypeCanonicallyEquals(
            CompType ct1, int base1, int size1, CompType ct2, int base2, int size2) {
        if (ct1.funcType() != null && ct2.funcType() != null) {
            return funcTypeCanonicallyEquals(
                    ct1.funcType(), base1, size1, ct2.funcType(), base2, size2);
        }
        if (ct1.structType() != null && ct2.structType() != null) {
            return structTypeCanonicallyEquals(
                    ct1.structType(), base1, size1, ct2.structType(), base2, size2);
        }
        if (ct1.arrayType() != null && ct2.arrayType() != null) {
            return fieldTypeCanonicallyEquals(
                    ct1.arrayType().fieldType(),
                    base1,
                    size1,
                    ct2.arrayType().fieldType(),
                    base2,
                    size2);
        }
        return false;
    }

    private boolean funcTypeCanonicallyEquals(
            FunctionType ft1, int base1, int size1, FunctionType ft2, int base2, int size2) {
        if (ft1.params().size() != ft2.params().size()
                || ft1.returns().size() != ft2.returns().size()) {
            return false;
        }
        for (int i = 0; i < ft1.params().size(); i++) {
            if (!valTypeCanonicallyEquals(
                    ft1.params().get(i), base1, size1, ft2.params().get(i), base2, size2)) {
                return false;
            }
        }
        for (int i = 0; i < ft1.returns().size(); i++) {
            if (!valTypeCanonicallyEquals(
                    ft1.returns().get(i), base1, size1, ft2.returns().get(i), base2, size2)) {
                return false;
            }
        }
        return true;
    }

    private boolean structTypeCanonicallyEquals(
            StructType st1, int base1, int size1, StructType st2, int base2, int size2) {
        if (st1.fieldTypes().length != st2.fieldTypes().length) {
            return false;
        }
        for (int i = 0; i < st1.fieldTypes().length; i++) {
            if (!fieldTypeCanonicallyEquals(
                    st1.fieldTypes()[i], base1, size1, st2.fieldTypes()[i], base2, size2)) {
                return false;
            }
        }
        return true;
    }

    private boolean fieldTypeCanonicallyEquals(
            FieldType f1, int base1, int size1, FieldType f2, int base2, int size2) {
        if (f1.mut() != f2.mut()) {
            return false;
        }
        var s1 = f1.storageType();
        var s2 = f2.storageType();
        if (s1.packedType() != null || s2.packedType() != null) {
            return Objects.equals(s1.packedType(), s2.packedType());
        }
        return valTypeCanonicallyEquals(s1.valType(), base1, size1, s2.valType(), base2, size2);
    }

    private boolean valTypeCanonicallyEquals(
            ValType v1, int base1, int size1, ValType v2, int base2, int size2) {
        if (v1.opcode() != v2.opcode()) {
            return false;
        }
        if (!v1.isReference()) {
            return true;
        }
        return refIdxCanonicallyEquals(v1.typeIdx(), base1, size1, v2.typeIdx(), base2, size2);
    }

    private boolean refIdxCanonicallyEquals(
            int idx1, int base1, int size1, int idx2, int base2, int size2) {
        // Both abstract heap types (negative) - compare directly
        if (idx1 < 0 && idx2 < 0) {
            return idx1 == idx2;
        }
        // Within-group references: use relative offset
        boolean inGroup1 = idx1 >= base1 && idx1 < base1 + size1;
        boolean inGroup2 = idx2 >= base2 && idx2 < base2 + size2;
        if (inGroup1 && inGroup2) {
            return (idx1 - base1) == (idx2 - base2);
        }
        if (inGroup1 || inGroup2) {
            return false; // one in-group, one out-group
        }
        // Both outside-group: check if they're the same or canonically equivalent
        if (idx1 == idx2) {
            return true;
        }
        return canonicallyEquivalent(idx1, idx2);
    }

    /**
     * Check if type idx1 from ts1 is canonically equivalent to type idx2 from ts2.
     * Used for cross-module import matching.
     */
    public static boolean crossModuleCanonicallyEquivalent(
            TypeSection ts1, int idx1, TypeSection ts2, int idx2) {
        if (ts1 == ts2) {
            return ts1.canonicallyEquivalent(idx1, idx2);
        }
        if (idx1 < 0
                || idx1 >= ts1.flattenedSubTypes.length
                || idx2 < 0
                || idx2 >= ts2.flattenedSubTypes.length) {
            return false;
        }
        int base1 = ts1.recGroupBase[idx1];
        int size1 = ts1.recGroupSize[idx1];
        int base2 = ts2.recGroupBase[idx2];
        int size2 = ts2.recGroupSize[idx2];
        if (size1 != size2) {
            return false;
        }
        if (idx1 - base1 != idx2 - base2) {
            return false;
        }
        for (int i = 0; i < size1; i++) {
            if (!crossModuleSubtypeCanonicallyEquals(
                    ts1,
                    ts1.flattenedSubTypes[base1 + i],
                    base1,
                    size1,
                    ts2,
                    ts2.flattenedSubTypes[base2 + i],
                    base2,
                    size2)) {
                return false;
            }
        }
        return true;
    }

    private static boolean crossModuleSubtypeCanonicallyEquals(
            TypeSection ts1,
            SubType st1,
            int base1,
            int size1,
            TypeSection ts2,
            SubType st2,
            int base2,
            int size2) {
        if (st1.isFinal() != st2.isFinal()) {
            return false;
        }
        int[] supers1 = st1.typeIdx();
        int[] supers2 = st2.typeIdx();
        if (supers1.length != supers2.length) {
            return false;
        }
        for (int i = 0; i < supers1.length; i++) {
            if (!crossModuleRefIdxCanonicallyEquals(
                    ts1, supers1[i], base1, size1, ts2, supers2[i], base2, size2)) {
                return false;
            }
        }
        return crossModuleCompTypeCanonicallyEquals(
                ts1, st1.compType(), base1, size1, ts2, st2.compType(), base2, size2);
    }

    private static boolean crossModuleCompTypeCanonicallyEquals(
            TypeSection ts1,
            CompType ct1,
            int base1,
            int size1,
            TypeSection ts2,
            CompType ct2,
            int base2,
            int size2) {
        if (ct1.funcType() != null && ct2.funcType() != null) {
            return crossModuleFuncTypeCanonicallyEquals(
                    ts1, ct1.funcType(), base1, size1, ts2, ct2.funcType(), base2, size2);
        }
        if (ct1.structType() != null && ct2.structType() != null) {
            return crossModuleStructTypeCanonicallyEquals(
                    ts1, ct1.structType(), base1, size1, ts2, ct2.structType(), base2, size2);
        }
        if (ct1.arrayType() != null && ct2.arrayType() != null) {
            return crossModuleFieldTypeCanonicallyEquals(
                    ts1,
                    ct1.arrayType().fieldType(),
                    base1,
                    size1,
                    ts2,
                    ct2.arrayType().fieldType(),
                    base2,
                    size2);
        }
        return false;
    }

    private static boolean crossModuleFuncTypeCanonicallyEquals(
            TypeSection ts1,
            FunctionType ft1,
            int base1,
            int size1,
            TypeSection ts2,
            FunctionType ft2,
            int base2,
            int size2) {
        if (ft1.params().size() != ft2.params().size()
                || ft1.returns().size() != ft2.returns().size()) {
            return false;
        }
        for (int i = 0; i < ft1.params().size(); i++) {
            if (!crossModuleValTypeCanonicallyEquals(
                    ts1,
                    ft1.params().get(i),
                    base1,
                    size1,
                    ts2,
                    ft2.params().get(i),
                    base2,
                    size2)) {
                return false;
            }
        }
        for (int i = 0; i < ft1.returns().size(); i++) {
            if (!crossModuleValTypeCanonicallyEquals(
                    ts1,
                    ft1.returns().get(i),
                    base1,
                    size1,
                    ts2,
                    ft2.returns().get(i),
                    base2,
                    size2)) {
                return false;
            }
        }
        return true;
    }

    private static boolean crossModuleStructTypeCanonicallyEquals(
            TypeSection ts1,
            StructType st1,
            int base1,
            int size1,
            TypeSection ts2,
            StructType st2,
            int base2,
            int size2) {
        if (st1.fieldTypes().length != st2.fieldTypes().length) {
            return false;
        }
        for (int i = 0; i < st1.fieldTypes().length; i++) {
            if (!crossModuleFieldTypeCanonicallyEquals(
                    ts1,
                    st1.fieldTypes()[i],
                    base1,
                    size1,
                    ts2,
                    st2.fieldTypes()[i],
                    base2,
                    size2)) {
                return false;
            }
        }
        return true;
    }

    private static boolean crossModuleFieldTypeCanonicallyEquals(
            TypeSection ts1,
            FieldType f1,
            int base1,
            int size1,
            TypeSection ts2,
            FieldType f2,
            int base2,
            int size2) {
        if (f1.mut() != f2.mut()) {
            return false;
        }
        var s1 = f1.storageType();
        var s2 = f2.storageType();
        if (s1.packedType() != null || s2.packedType() != null) {
            return Objects.equals(s1.packedType(), s2.packedType());
        }
        return crossModuleValTypeCanonicallyEquals(
                ts1, s1.valType(), base1, size1, ts2, s2.valType(), base2, size2);
    }

    private static boolean crossModuleValTypeCanonicallyEquals(
            TypeSection ts1,
            ValType v1,
            int base1,
            int size1,
            TypeSection ts2,
            ValType v2,
            int base2,
            int size2) {
        if (v1.opcode() != v2.opcode()) {
            return false;
        }
        if (!v1.isReference()) {
            return true;
        }
        return crossModuleRefIdxCanonicallyEquals(
                ts1, v1.typeIdx(), base1, size1, ts2, v2.typeIdx(), base2, size2);
    }

    private static boolean crossModuleRefIdxCanonicallyEquals(
            TypeSection ts1,
            int idx1,
            int base1,
            int size1,
            TypeSection ts2,
            int idx2,
            int base2,
            int size2) {
        if (idx1 < 0 && idx2 < 0) {
            return idx1 == idx2;
        }
        boolean inGroup1 = idx1 >= base1 && idx1 < base1 + size1;
        boolean inGroup2 = idx2 >= base2 && idx2 < base2 + size2;
        if (inGroup1 && inGroup2) {
            return (idx1 - base1) == (idx2 - base2);
        }
        if (inGroup1 || inGroup2) {
            return false;
        }
        // Both outside-group: recursively check cross-module equivalence
        return crossModuleCanonicallyEquivalent(ts1, idx1, ts2, idx2);
    }

    // https://github.com/WebAssembly/gc/blob/main/proposals/gc/MVP.md#type-definitions
    // > the number of type section entries is now the number of recursion groups rather than the
    // number of individual types.
    // types() returns the flattened list of individual types indexed by flat SubType index.
    // Non-function types (struct/array) are null.
    public FunctionType[] types() {
        var result = new FunctionType[flattenedSubTypes.length];
        for (int i = 0; i < flattenedSubTypes.length; i++) {
            result[i] = flattenedSubTypes[i].compType().funcType();
        }
        return result;
    }

    public int typeCount() {
        return types.size();
    }

    public int subTypeCount() {
        return flattenedSubTypes.length;
    }

    public FunctionType getType(int idx) {
        return getSubType(idx).compType().funcType();
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
