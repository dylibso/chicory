package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.EnumMap;
import java.util.Objects;

/**
 * A reference-typed instruction.
 */
public final class RefTypedInsn extends Insn<Op.RefTyped> {
    private final ValueType type;

    private RefTypedInsn(Op.RefTyped op, ValueType type) {
        super(op, type.hashCode());
        if (!type.isReference()) {
            throw new IllegalArgumentException("Invalid reference type " + type);
        }
        this.type = type;
    }

    public ValueType type() {
        return type;
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        out.type(type);
    }

    public boolean equals(final Object obj) {
        return this == obj;
    }

    public StringBuilder toString(final StringBuilder sb) {
        return super.toString(sb).append(' ').append(type);
    }

    private static final EnumMap<Op.RefTyped, EnumMap<ValueType, RefTypedInsn>> ALL;

    public static RefTypedInsn forOpAndType(Op.RefTyped op, ValueType type) {
        Objects.requireNonNull(op, "op");
        Objects.requireNonNull(type, "type");
        if (!type.isReference()) {
            throw new IllegalArgumentException("Invalid reference type " + type);
        }
        return ALL.get(op).get(type);
    }

    static {
        EnumMap<Op.RefTyped, EnumMap<ValueType, RefTypedInsn>> map =
                new EnumMap<>(Op.RefTyped.class);
        for (Op.RefTyped refTyped : Op.RefTyped.values()) {
            EnumMap<ValueType, RefTypedInsn> innerMap = new EnumMap<>(ValueType.class);
            map.put(refTyped, innerMap);
            for (ValueType refType : ValueType.values()) {
                if (refType.isReference()) {
                    innerMap.put(refType, new RefTypedInsn(refTyped, refType));
                }
            }
        }
        ALL = map;
    }
}
