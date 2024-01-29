package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An instruction that operates on a list of types.
 */
public final class TypesInsn extends Insn<Op.Types> implements Cacheable {
    private final List<ValueType> types;

    private TypesInsn(Op.Types op, List<ValueType> types, boolean ignored) {
        super(op, types.hashCode());
        this.types = types;
    }

    TypesInsn(Op.Types op, List<ValueType> types) {
        this(op, List.copyOf(types), true);
    }

    public List<ValueType> types() {
        return types;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TypesInsn && equals((TypesInsn) obj);
    }

    public boolean equals(TypesInsn other) {
        return this == other || super.equals(other) && types == other.types;
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.typeVec(types);
    }

    public StringBuilder toString(final StringBuilder sb) {
        super.toString(sb);
        for (ValueType type : types) {
            sb.append(' ').append(type);
        }
        return sb;
    }

    private static final EnumMap<Op.Types, Map<ValueType, TypesInsn>> ONE_TYPE;

    /**
     * Return a cached instruction instance for single-typed cases.
     *
     * @param op the operation (must not be {@code null})
     * @param type the value type (must not be {@code null})
     * @return the cached instruction (not {@code null})
     */
    public static TypesInsn forOpAndType(Op.Types op, ValueType type) {
        Objects.requireNonNull(op, "op");
        Objects.requireNonNull(type, "type");
        return ONE_TYPE.get(op).get(type);
    }

    static {
        EnumMap<Op.Types, Map<ValueType, TypesInsn>> map = new EnumMap<>(Op.Types.class);
        HashMap<ValueType, TypesInsn> subMap = new HashMap<>();
        for (Op.Types op : Op.Types.values()) {
            subMap.clear();
            for (ValueType type : ValueType.values()) {
                subMap.put(type, new TypesInsn(op, List.of(type)));
            }
            map.put(op, Map.copyOf(subMap));
        }
        ONE_TYPE = map;
    }
}
