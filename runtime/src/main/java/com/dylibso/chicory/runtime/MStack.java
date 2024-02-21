package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.Arrays;
import java.util.List;

/**
 * The machine stack, which is type-safe and dynamically grows as needed.
 */
public final class MStack {

    /**
     * The canonical type stack.
     */
    private byte[] types;

    private int depth;

    /**
     * The scalar value substack.
     */
    private int[] words;

    private int wordDepth;

    /**
     * The reference value substack.
     */
    private Ref[] refs;

    private int refDepth;

    /**
     * The stack protection base.
     */
    private int base;

    /**
     * Construct a new instance.
     */
    public MStack() {
        this(256);
    }

    /**
     * Construct a new instance.
     *
     * @param estimatedSize the estimated stack size (hint)
     */
    public MStack(int estimatedSize) {
        estimatedSize = Math.max(4, estimatedSize);
        types = new byte[estimatedSize];
        words = new int[estimatedSize];
        refs = new Ref[estimatedSize];
    }

    /**
     * Get and set the stack protection base.
     * Attempting to pop values below this base depth will result in an underflow exception.
     * The base must be between 0 and the current stack depth (inclusive).
     *
     * @param newBase the new stack protection base
     * @return the previous stack protection base
     */
    public int setProtectionBase(int newBase) {
        if (newBase < 0 || newBase > depth) {
            throw new IllegalArgumentException(
                    "Stack protection base must be between 0 and the current depth");
        }
        try {
            return base;
        } finally {
            base = newBase;
        }
    }

    public void push(Value v) {
        if (v == null) throw new RuntimeException("Can't push null value onto stack");
        switch (v.type()) {
            case F64:
                pushF64(v.asDouble());
                break;
            case F32:
                pushF32(v.asFloat());
                break;
            case I64:
                pushI64(v.asLong());
                break;
            case I32:
                pushI32(v.asInt());
                break;
            case V128:
                throw new UnsupportedOperationException();
            case FuncRef:
                if (v.asFuncRef() == Value.REF_NULL_VALUE) {
                    pushFuncRef(null);
                } else {
                    // todo: this is not correct with respect to imports, host funcs, etc.
                    pushFuncRef(new FuncRef(null, v.asFuncRef()));
                }
                break;
            case ExternRef:
                if (v.asExtRef() == Value.REF_NULL_VALUE) {
                    pushExternRef(null);
                } else {
                    // todo: replace with proper mechanism
                    pushExternRef(new ExternRef(CompatRefValue.values.get(v.asExtRef())));
                }
                break;
            default:
                // impossible
                throw new IllegalStateException();
        }
    }

    /**
     * Push a zero value of the given type on to the stack.
     *
     * @param type the value type (must not be {@code null})
     */
    public void pushZero(ValueType type) {
        switch (type) {
            case F64:
                pushF64(0);
                break;
            case F32:
                pushF32(0);
                break;
            case I64:
                pushI64(0);
                break;
            case I32:
                pushI32(0);
                break;
            case V128:
                pushV128(0, 0);
                break;
            case FuncRef:
                pushFuncRef(null);
                break;
            case ExternRef:
                pushExternRef(null);
                break;
            default:
                // impossible
                throw new IllegalStateException();
        }
    }

    /**
     * Push a value of type {@link ValueType#I32 I32} on to the stack.
     *
     * @param val the value to push
     */
    public void pushI32(int val) {
        pushTypeOnly(ValueType.I32);
        pushWord(val);
    }

    /**
     * Push a boolean value on to the stack as a value of type {@link ValueType#I32 I32}.
     *
     * @param val the value to push
     */
    public void pushI32(boolean val) {
        pushI32(val ? 1 : 0);
    }

    /**
     * Push a value of type {@link ValueType#I64 I64} on to the stack.
     *
     * @param val the value to push
     */
    public void pushI64(long val) {
        pushTypeOnly(ValueType.I64);
        pushWord(val);
        pushWord(val >>> 32);
    }

    /**
     * Push a value of type {@link ValueType#F32 F32} on to the stack.
     *
     * @param val the value to push
     */
    public void pushF32(float val) {
        pushTypeOnly(ValueType.F32);
        pushWord(Float.floatToRawIntBits(val));
    }

    /**
     * Push a value of type {@link ValueType#F64 F64} on to the stack.
     *
     * @param val the value to push
     */
    public void pushF64(double val) {
        pushTypeOnly(ValueType.F64);
        long bits = Double.doubleToRawLongBits(val);
        pushWord(bits);
        pushWord(bits >>> 32);
    }

    /**
     * Push a value of type {@link ValueType#V128 V128} on to the stack.
     *
     * @param low the low-order 64 bits of the value to push
     * @param high the high-order 64 bits of the value to push
     */
    public void pushV128(long low, long high) {
        pushTypeOnly(ValueType.V128);
        pushWord(low);
        pushWord(low >>> 32);
        pushWord(high);
        pushWord(high >>> 32);
    }

    /**
     * Push a value of type {@link ValueType#FuncRef FuncRef} on to the stack.
     *
     * @param ref the value to push, or {@code null} to push the {@code null} value
     */
    public void pushFuncRef(FuncRef ref) {
        pushTypeOnly(ValueType.FuncRef);
        pushRef(ref);
    }

    /**
     * Push a value of type {@link ValueType#ExternRef ExternRef} on to the stack.
     *
     * @param ref the value to push, or {@code null} to push the {@code null} value
     */
    public void pushExternRef(ExternRef ref) {
        pushTypeOnly(ValueType.ExternRef);
        pushRef(ref);
    }

    /**
     * Check that the type of the top {@code n} stack entries matches the given list.
     *
     * @param types the list of types (must not be {@code null})
     * @return {@code true} if the types match, or {@code false} if the types do not match
     */
    public boolean checkTypes(List<ValueType> types) {
        int cnt = types.size();
        int start = depth - cnt;
        if (start < base) {
            throw underflow();
        }
        for (int i = 0; i < cnt; i++) {
            if (!idToType(this.types[start + i]).equals(types.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Unwind the stack to the given depth.
     *
     * @param depth the depth to unwind to
     */
    public void resetDepth(int depth) {
        if (base > depth || depth > this.depth) {
            throw underflow();
        }
        drop(this.depth - depth);
    }

    /**
     * Drop some values from the top of the stack.
     *
     * @param count the number of values to drop
     */
    public void drop(int count) {
        drop(count, 0);
    }

    /**
     * Drop some values from the stack.
     *
     * @param dropCount the number of values to drop
     * @param skip the number of values to skip before dropping values
     */
    public void drop(int dropCount, int skip) {
        if (dropCount < 0 || skip < 0) {
            throw new IllegalArgumentException("dropCount and skip must not be less than 0");
        }
        if (dropCount == 0) {
            // do nothing
            return;
        }
        if (depth - dropCount - skip < base) {
            throw underflow();
        }
        // compute the number of entries to skip on each substack
        int skipWords = 0;
        int skipRefs = 0;
        for (int i = 0; i < skip; i++) {
            ValueType tos = idToType(types[depth - i - 1]);
            skipWords += wordCount(tos);
            skipRefs += refCount(tos);
        }
        // compute the number of entries to drop on each substack
        int dropWords = 0;
        int dropRefs = 0;
        for (int i = 0; i < dropCount; i++) {
            ValueType tos = idToType(types[depth - i - skip - 1]);
            dropWords += wordCount(tos);
            dropRefs += refCount(tos);
        }
        if (skip > 0) {
            // preserve the top N value types
            System.arraycopy(types, depth - skip, types, depth - dropCount - skip, skip);
            // words
            System.arraycopy(
                    words,
                    wordDepth - skipWords,
                    words,
                    wordDepth - dropWords - skipWords,
                    skipWords);
            // refs
            System.arraycopy(
                    refs, refDepth - skipRefs, refs, refDepth - dropRefs - skipRefs, skipRefs);
        }
        // clear refs for GC
        Arrays.fill(refs, refDepth - dropRefs, refDepth, null);
        // finally, shrink the stacks
        depth -= dropCount;
        wordDepth -= dropWords;
        refDepth -= dropRefs;
    }

    public Value pop() {
        switch (peekType()) {
            case F64:
                return Value.f64(Double.doubleToRawLongBits(popF64()));
            case F32:
                return Value.f32(Float.floatToRawIntBits(popF32()));
            case I64:
                return Value.i64(popI64());
            case I32:
                return Value.i32(Integer.toUnsignedLong(popI32()));
            case V128:
                throw new UnsupportedOperationException();
            case FuncRef:
                FuncRef funcRef = popFuncRef();
                if (funcRef == null) {
                    return Value.FUNCREF_NULL;
                } else {
                    // todo: this is not correct with respect to imports, host funcs, etc.
                    return Value.funcRef(funcRef.index());
                }
            case ExternRef:
                ExternRef externRef = popExternRef();
                if (externRef == null) {
                    return Value.EXTREF_NULL;
                } else if (externRef.value() instanceof CompatRefValue) {
                    // todo: replace with proper mechanism
                    return Value.externRef(((CompatRefValue) externRef.value()).value());
                } else {
                    throw new UnsupportedOperationException("converting extref to compat extref");
                }
            default:
                // impossible
                throw new IllegalStateException();
        }
    }

    /**
     * Pop a value of type {@link ValueType#I32 I32} from the stack.
     *
     * @return the popped value
     */
    public int popI32() {
        popTypeOnly(ValueType.I32);
        return words[--wordDepth];
    }

    /**
     * Pop a value of type {@link ValueType#I64 I64} from the stack.
     *
     * @return the popped value
     */
    public long popI64() {
        popTypeOnly(ValueType.I64);
        long val = (long) words[--wordDepth] << 32L;
        val |= Integer.toUnsignedLong(words[--wordDepth]);
        return val;
    }

    /**
     * Pop a value of type {@link ValueType#F32 F32} from the stack.
     *
     * @return the popped value
     */
    public float popF32() {
        popTypeOnly(ValueType.F32);
        return Float.intBitsToFloat(words[--wordDepth]);
    }

    /**
     * Pop a value of type {@link ValueType#F64 F64} from the stack.
     *
     * @return the popped value
     */
    public double popF64() {
        popTypeOnly(ValueType.F64);
        long val = (long) words[--wordDepth] << 32L;
        val |= Integer.toUnsignedLong(words[--wordDepth]);
        return Double.longBitsToDouble(val);
    }

    /**
     * Pop a value of type {@link ValueType#V128 V128} from the stack, and return the low-order 64 bits of the value.
     * Top pop a full {@code V128} value, use in combination with {@link #peekV128High()}.
     *
     * @return the low-order 64 bits of the popped value
     * @see #peekV128High()
     */
    public long popV128Low() {
        popTypeOnly(ValueType.V128);
        wordDepth -= 2;
        long low = (long) words[--wordDepth] << 32L;
        low |= Integer.toUnsignedLong(words[--wordDepth]);
        return low;
    }

    /**
     * Pop a value of type {@link ValueType#ExternRef ExternRef} from the stack.
     *
     * @return the popped value (possibly {@code null})
     */
    public ExternRef popExternRef() {
        popTypeOnly(ValueType.ExternRef);
        int idx = --refDepth;
        ExternRef ref = (ExternRef) refs[idx];
        // for GC
        refs[idx] = null;
        return ref;
    }

    /**
     * Pop a value of type {@link ValueType#FuncRef FuncRef} from the stack.
     *
     * @return the popped value (possibly {@code null})
     */
    public FuncRef popFuncRef() {
        popTypeOnly(ValueType.FuncRef);
        int idx = --refDepth;
        FuncRef ref = (FuncRef) refs[idx];
        // for GC
        refs[idx] = null;
        return ref;
    }

    public Value peek() {
        switch (peekType()) {
            case F64:
                return Value.f64(Double.doubleToRawLongBits(peekF64()));
            case F32:
                return Value.f32(Float.floatToRawIntBits(peekF32()));
            case I64:
                return Value.i64(peekI64());
            case I32:
                return Value.i32(Integer.toUnsignedLong(peekI32()));
            case V128:
                throw new UnsupportedOperationException();
            case FuncRef:
                FuncRef funcRef = peekFuncRef();
                // todo: this is not correct with respect to imports, host funcs, etc.
                return Value.funcRef(funcRef.index());
            case ExternRef:
                ExternRef externRef = peekExternRef();
                if (externRef == null) {
                    return Value.EXTREF_NULL;
                } else if (externRef.value() instanceof CompatRefValue) {
                    // todo: replace with proper mechanism
                    return Value.externRef(((CompatRefValue) externRef.value()).value());
                } else {
                    throw new UnsupportedOperationException("converting extref to compat extref");
                }
            default:
                // impossible
                throw new IllegalStateException();
        }
    }

    /**
     * {@return the type of the value on the top of the stack}
     */
    public ValueType peekType() {
        int size = this.depth;
        if (size == base) {
            throw underflow();
        }
        return idToType(types[size - 1]);
    }

    /**
     * {@return the value on the top of the stack}
     * The value must be of type {@link ValueType#I32 I32}.
     */
    public int peekI32() {
        expectType(ValueType.I32);
        return words[wordDepth - 1];
    }

    /**
     * {@return the value on the top of the stack}
     * The value must be of type {@link ValueType#I64 I64}.
     */
    public long peekI64() {
        expectType(ValueType.I64);
        long val = (long) words[wordDepth - 1] << 32L;
        val |= Integer.toUnsignedLong(words[wordDepth - 2]);
        return val;
    }

    /**
     * {@return the value on the top of the stack}
     * The value must be of type {@link ValueType#F32 F32}.
     */
    public float peekF32() {
        expectType(ValueType.F32);
        return Float.intBitsToFloat(words[wordDepth - 1]);
    }

    /**
     * {@return the value on the top of the stack}
     * The value must be of type {@link ValueType#F64 F64}.
     */
    public double peekF64() {
        expectType(ValueType.F64);
        long val = (long) words[wordDepth - 1] << 32L;
        val |= Integer.toUnsignedLong(words[wordDepth - 2]);
        return Double.longBitsToDouble(val);
    }

    /**
     * {@return the high-order 64 bits of the value on the top of the stack}
     * The value must be of type {@link ValueType#V128 V128}.
     *
     * @see #peekV128Low()
     * @see #popV128Low()
     */
    public long peekV128High() {
        expectType(ValueType.V128);
        long hi = (long) words[wordDepth - 1] << 32L;
        hi |= Integer.toUnsignedLong(words[wordDepth - 2]);
        return hi;
    }

    /**
     * {@return the low-order 64 bits of the value on the top of the stack}
     * The value must be of type {@link ValueType#V128 V128}.
     *
     * @see #peekV128High()
     * @see #popV128Low()
     */
    public long peekV128Low() {
        expectType(ValueType.V128);
        long hi = (long) words[wordDepth - 3] << 32L;
        hi |= Integer.toUnsignedLong(words[wordDepth - 4]);
        return hi;
    }

    /**
     * {@return the value on the top of the stack}
     * The value must be of type {@link ValueType#ExternRef ExternRef}.
     */
    public ExternRef peekExternRef() {
        expectType(ValueType.ExternRef);
        return (ExternRef) refs[refDepth - 1];
    }

    /**
     * {@return the value on the top of the stack}
     * The value must be of type {@link ValueType#FuncRef FuncRef}.
     */
    public FuncRef peekFuncRef() {
        expectType(ValueType.FuncRef);
        return (FuncRef) refs[refDepth - 1];
    }

    /**
     * Duplicate the value on the top of the stack.
     */
    public void dup() {
        switch (peekType()) {
            case F64:
                pushF64(peekF64());
                break;
            case F32:
                pushF32(peekF32());
                break;
            case I64:
                pushI64(peekI64());
                break;
            case I32:
                pushI32(peekI32());
                break;
            case V128:
                pushV128(peekV128Low(), peekV128High());
                break;
            case FuncRef:
                pushFuncRef(peekFuncRef());
                break;
            case ExternRef:
                pushExternRef(peekExternRef());
                break;
            default:
                // not possible
                throw new IllegalStateException();
        }
    }

    /**
     * {@return the current stack depth}
     */
    public int depth() {
        return depth;
    }

    /**
     * {@return a string representation of the current state of the stack}
     */
    public String toString() {
        if (depth == 0) {
            return "[]";
        }
        // start from the bottom of the stack, and work our way upwards
        StringBuilder sb = new StringBuilder(depth * 8);
        sb.append('[');
        int t = 0, w = 0, r = 0;
        for (; ; ) {
            if (0 < t && t < depth) {
                sb.append(',');
            }
            if (t == depth) {
                sb.append(']');
                return sb.toString();
            }
            ValueType tos = idToType(types[t++]);
            sb.append(tos).append('=');
            switch (tos) {
                case F64:
                    {
                        long val = Integer.toUnsignedLong(words[w++]);
                        val |= (long) words[w++] << 32;
                        sb.append(Double.longBitsToDouble(val));
                        break;
                    }
                case F32:
                    sb.append(Float.intBitsToFloat(words[w++]));
                    break;
                case I64:
                    {
                        long val = Integer.toUnsignedLong(words[w++]);
                        val |= (long) words[w++] << 32;
                        sb.append(val);
                        break;
                    }
                case I32:
                    sb.append(words[w++]);
                    break;
                case V128:
                    {
                        long hi = Integer.toUnsignedLong(words[w++]);
                        hi |= (long) words[w++] << 32;
                        long low = Integer.toUnsignedLong(words[w++]);
                        low |= (long) words[w++] << 32;
                        sb.append(Long.toHexString(hi)).append(':').append(Long.toHexString(low));
                        break;
                    }
                case FuncRef:
                case ExternRef:
                    sb.append(refs[r++]);
                    break;
            }
            if (t > 50 && t < depth - 50) {
                sb.append("...");
                t = depth - 50;
            }
        }
    }

    // test harness methods

    int wordDepth() {
        return wordDepth;
    }

    int refDepth() {
        return refDepth;
    }

    // internal methods

    private void expectType(ValueType expected) {
        int size = this.depth;
        if (size == base) {
            throw underflow();
        }
        ValueType tos = idToType(types[size - 1]);
        if (tos != expected) {
            throw new ChicoryException(
                    "Stack type mismatch; expected " + expected + " but was " + tos);
        }
    }

    private static ValueType idToType(final byte id) {
        return ValueType.values.get(id & 0xff);
    }

    private static byte typeToId(final ValueType valueType) {
        return (byte) valueType.ordinal();
    }

    private void popTypeOnly(ValueType expected) {
        expectType(expected);
        depth--;
    }

    private void pushTypeOnly(ValueType valueType) {
        pushTypeId(typeToId(valueType));
    }

    /**
     * {@return the number of stack words for the given data type}
     */
    private static int wordCount(ValueType type) {
        // todo: java 17 switch expression
        switch (type) {
            case F64:
            case I64:
                return 2;
            case F32:
            case I32:
                return 1;
            case V128:
                return 4;
            default:
                return 0;
        }
    }

    /**
     * {@return the number of reference entries for the given data type}
     */
    private static int refCount(ValueType type) {
        // todo: java 17 switch expression
        switch (type) {
            case FuncRef:
            case ExternRef:
                return 1;
            default:
                return 0;
        }
    }

    private void pushTypeId(int id) {
        int idx = depth++;
        try {
            types[idx] = (byte) id;
        } catch (ArrayIndexOutOfBoundsException ignored) {
            assert idx == types.length;
            types = Arrays.copyOf(types, idx + (idx >> 1));
            types[idx] = (byte) id;
        }
    }

    // convenience method
    private void pushWord(long word) {
        pushWord((int) word);
    }

    private void pushWord(int word) {
        int idx = wordDepth++;
        try {
            words[idx] = word;
        } catch (ArrayIndexOutOfBoundsException ignored) {
            assert idx == words.length;
            words = Arrays.copyOf(words, idx + (idx >> 1));
            words[idx] = word;
        }
    }

    private void pushRef(Ref ref) {
        int idx = refDepth++;
        try {
            refs[idx] = ref;
        } catch (ArrayIndexOutOfBoundsException ignored) {
            assert idx == refs.length;
            refs = Arrays.copyOf(refs, idx + (idx >> 1));
            refs[idx] = ref;
        }
    }

    private static RuntimeException underflow() {
        return new RuntimeException("Stack underflow exception");
    }
}
