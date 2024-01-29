package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmOutputStream;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A block instruction which contains a sequence of nested instructions.
 */
public final class BlockInsn extends Insn<Op.Block> {
    private final InsnSeq body;
    private final BlockInsn enclosing;
    private final Type type;

    private BlockInsn(Op.Block op, InsnSeq.Cache cache, BlockInsn enclosing, Type type) {
        super(op, type.hashCode());
        this.body = new InsnSeq(this, cache);
        this.enclosing = enclosing;
        this.type = type;
    }

    BlockInsn(Op.Block op, InsnSeq.Cache cache, BlockInsn enclosing, int typeId) {
        this(op, cache, enclosing, new IdType(typeId));
    }

    BlockInsn(Op.Block op, InsnSeq.Cache cache, BlockInsn enclosing) {
        this(op, cache, enclosing, EmptyType.INSTANCE);
    }

    BlockInsn(Op.Block op, InsnSeq.Cache cache, BlockInsn enclosing, ValueType simpleType) {
        this(op, cache, enclosing, simpleTypes.get(simpleType));
    }

    public InsnSeq body() {
        return body;
    }

    /**
     * {@return the enclosing block, or <code>null</code> if this block is directly enclosed by the function itself}
     */
    public BlockInsn enclosing() {
        return enclosing;
    }

    /**
     * {@return the enclosing block with the given index, or <code>null</code> if the index refers to the function itself}
     * The index is relative to this block, so an index of zero equals this block, one equals the enclosing block, etc.
     *
     * @throws IndexOutOfBoundsException if the index points beyond the outermost enclosing block (the function itself)
     */
    public BlockInsn enclosing(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException();
        }
        BlockInsn result = this;
        while (index > 0) {
            if (result == null) {
                throw new IndexOutOfBoundsException();
            }
            result = result.enclosing();
            index--;
        }
        return result;
    }

    /**
     * {@return the block type}
     */
    public Type type() {
        return type;
    }

    /**
     * {@return the actual block type, computed from a resolver function}
     * The resolver function may, for example, be {@code typeSection::getType}.
     */
    public FunctionType computeType(IntFunction<FunctionType> typeResolver) {
        return type.compute(typeResolver);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BlockInsn && equals((BlockInsn) obj);
    }

    public boolean equals(BlockInsn other) {
        return this == other
                || super.equals(other) && body.equals(other.body) && type.equals(other.type);
    }

    /**
     * {@return the instruction hash code}
     * <em>Note:</em> this hash code is only stable if the body's instruction sequence has been {@linkplain InsnSeq#end() ended}.
     */
    @Override
    public int hashCode() {
        return super.hashCode() * 31 + body.hashCode();
    }

    public StringBuilder toString(final StringBuilder sb) {
        return super.toString(sb).append(' ').append(type);
    }

    public void writeTo(final WasmOutputStream out) throws WasmIOException {
        super.writeTo(out);
        type.writeTo(out);
        body.writeTo(out);
    }

    public abstract static class Type {
        Type() {}

        public boolean hasId() {
            return false;
        }

        public int id() {
            throw new NoSuchElementException();
        }

        public boolean isSimple() {
            return false;
        }

        public ValueType returnType() {
            throw new NoSuchElementException();
        }

        public boolean isEmpty() {
            return false;
        }

        abstract void writeTo(final WasmOutputStream out) throws WasmIOException;

        abstract FunctionType compute(final IntFunction<FunctionType> typeResolver);
    }

    /**
     * A block with a complex type (look up from the function table).
     */
    public static final class IdType extends Type {
        private final int id;

        IdType(final int id) {
            this.id = id;
        }

        public boolean hasId() {
            return true;
        }

        public int id() {
            return id;
        }

        void writeTo(final WasmOutputStream out) throws WasmIOException {
            out.u31(id);
        }

        FunctionType compute(final IntFunction<FunctionType> typeResolver) {
            return typeResolver.apply(id);
        }

        public boolean equals(final Object obj) {
            return obj instanceof IdType && equals((IdType) obj);
        }

        public boolean equals(final IdType other) {
            return this == other || other != null && id == other.id;
        }

        public int hashCode() {
            return id;
        }
    }

    /**
     * A block with a simple type (accepts nothing, returns one value).
     */
    public static final class SimpleType extends Type {
        private final ValueType returnType;

        SimpleType(final ValueType returnType) {
            this.returnType = returnType;
        }

        public boolean isSimple() {
            return true;
        }

        public ValueType returnType() {
            return returnType;
        }

        void writeTo(final WasmOutputStream out) throws WasmIOException {
            out.type(returnType);
        }

        FunctionType compute(final IntFunction<FunctionType> typeResolver) {
            return FunctionType.returning(returnType);
        }

        public boolean equals(final Object obj) {
            return obj instanceof SimpleType && equals((SimpleType) obj);
        }

        public boolean equals(final SimpleType other) {
            return this == other || other != null && returnType == other.returnType;
        }

        public int hashCode() {
            return returnType.hashCode();
        }
    }

    private static final Map<ValueType, SimpleType> simpleTypes =
            Stream.of(ValueType.values())
                    .collect(Collectors.toUnmodifiableMap(Function.identity(), SimpleType::new));

    /**
     * A block with an empty type.
     */
    public static final class EmptyType extends Type {
        private EmptyType() {}

        static final EmptyType INSTANCE = new EmptyType();

        public boolean isEmpty() {
            return true;
        }

        void writeTo(final WasmOutputStream out) throws WasmIOException {
            out.rawByte(0x40);
        }

        FunctionType compute(final IntFunction<FunctionType> typeResolver) {
            return FunctionType.empty();
        }

        public boolean equals(final Object obj) {
            // use identity
            return super.equals(obj);
        }

        public int hashCode() {
            // use identity
            return super.hashCode();
        }
    }
}
