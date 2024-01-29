package com.dylibso.chicory.wasm.op;

import static com.dylibso.chicory.wasm.op.Ops.*;

import com.dylibso.chicory.wasm.io.WasmInputStream;
import com.dylibso.chicory.wasm.io.WasmOutputStream;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A sequence of instructions.
 * Once the sequence is ended, either by explicitly calling {@link #end()}
 * or by adding an {@link Ops#end end} instruction,
 * no more instructions may be added.
 * Instruction sequences are each distinct even if they contain identical instructions.
 */
public final class InsnSeq implements Iterable<Insn<?>> {
    private static final int DEFAULT_ESTIMATED_SIZE = 10;
    private static final int[] NO_CATCH = new int[0];

    // contents
    private final BlockInsn block;
    private List<Insn<?>> instructions;

    // configuration
    private boolean constantOnly = false;
    private boolean allowElse = false;
    private boolean allowCatch = false;

    // state
    private boolean ended = false;
    private int elseIdx = -1;
    private int catchAllIdx = -1;
    private int[] catchIndexes = NO_CATCH;
    private Cache cache;

    private InsnSeq(BlockInsn block, Cache cache, int estimatedSize) {
        this.block = block;
        this.cache = Objects.requireNonNull(cache, "cache");
        instructions = new ArrayList<>(estimatedSize);
    }

    InsnSeq(BlockInsn block, Cache cache) {
        this(block, cache, DEFAULT_ESTIMATED_SIZE);
    }

    /**
     * Construct a new instance.
     *
     * @param cache the instruction cache to use (must not be {@code null})
     * @param estimatedSize the estimated instruction count
     */
    public InsnSeq(Cache cache, int estimatedSize) {
        this(null, cache, estimatedSize);
    }

    /**
     * Construct a new instance.
     *
     * @param estimatedSize the estimated number of instructions, not including {@code end}
     */
    public InsnSeq(int estimatedSize) {
        this(new Cache(estimatedSize), estimatedSize);
    }

    /**
     * Construct a new instance.
     *
     * @param cache the instruction cache to use (must not be {@code null})
     */
    public InsnSeq(Cache cache) {
        this(cache, DEFAULT_ESTIMATED_SIZE);
    }

    /**
     * Construct a new instance.
     */
    public InsnSeq() {
        this(DEFAULT_ESTIMATED_SIZE);
    }

    /**
     * Only allow constant instructions for the remainder of this sequence.
     */
    public void constantOnly() {
        this.constantOnly = true;
    }

    /**
     * Allow at most one {@code else} instruction in this sequence.
     */
    public void allowElse() {
        this.allowElse = true;
    }

    /**
     * Allow {@code catch} instructions and at most one {@code catch_all} instruction in this sequence.
     */
    public void allowCatch() {
        this.allowCatch = true;
    }

    /**
     * {@return the enclosing block, or <code>null</code> if there is none}
     */
    public BlockInsn block() {
        return block;
    }

    public void writeTo(final WasmOutputStream out) {
        Objects.requireNonNull(out, "buf");
        instructions.forEach(i -> i.writeTo(out));
        // end is always implicit
        out.op(end);
    }

    public void readFrom(final WasmInputStream in) {
        Objects.requireNonNull(in, "in");
        Op op;
        do {
            op = in.op();
            op.readFrom(in, this);
        } while (op != end);
    }

    public static void skip(final WasmInputStream in) {
        Objects.requireNonNull(in, "in");
        Op op;
        do {
            op = in.op();
            op.skip(in);
        } while (op != end);
    }

    // todo: make this a flag on Op?
    private static final Set<Op> CONSTANT_OPS =
            Set.of(
                    i32.const_,
                    i64.const_,
                    f32.const_,
                    f64.const_,
                    v128.const_,
                    ref.null_,
                    ref.func,
                    // note: this one is only valid if the given global has constant mutability
                    global.get,
                    end);

    private <O extends Op, I extends Insn<O>> I add(I insn) {
        Objects.requireNonNull(insn, "insn");
        O op = insn.op();
        if (ended) {
            throw new IllegalStateException("Add after end");
        } else if (op == end) {
            end();
        } else if (constantOnly && !CONSTANT_OPS.contains(op)) {
            throw notAllowed(op);
        } else if (op == else_) {
            if (allowElse && elseIdx == -1) {
                // there can be only one
                elseIdx = instructions.size();
                instructions.add(insn);
            } else {
                throw notAllowed(op);
            }
        } else if (op == catch_) {
            if (allowCatch && catchAllIdx == -1) {
                instructions.add(insn);
                // multiple allowed, max one per tag
                int tag = ((TagInsn) insn).tag();
                int idx = findCatch(tag);
                if (idx < 0) {
                    // OK, insert it
                    idx = -idx - 1;
                    int oldLen = catchIndexes.length;
                    int[] newCatch = Arrays.copyOf(catchIndexes, oldLen + 2);
                    // make a spot
                    int ai = idx << 1;
                    System.arraycopy(newCatch, ai, newCatch, ai + 2, oldLen - ai - 1);
                    // the catch tag
                    newCatch[ai] = tag;
                    // the index of the instruction
                    newCatch[ai + 1] = instructions.size() - 1;
                    catchIndexes = newCatch;
                } else {
                    // duplicate tag
                    throw notAllowed(op);
                }
            } else {
                throw notAllowed(op);
            }
        } else if (op == catch_all) {
            if (allowCatch && catchAllIdx == -1) {
                // no more allowed
                catchAllIdx = instructions.size();
                instructions.add(insn);
            } else {
                throw notAllowed(op);
            }
        } else if (insn instanceof Cacheable) {
            instructions.add(getCached(insn));
        } else {
            instructions.add(insn);
        }
        return insn;
    }

    private static IllegalArgumentException notAllowed(final Op insn) {
        return new IllegalArgumentException("Instruction `" + insn + "` is not allowed here");
    }

    /**
     * End this sequence, if it is not already ended.
     * The instruction list will become immutable and the cache is discarded.
     *
     * @return this sequence (not {@code null})
     */
    public InsnSeq end() {
        if (!ended) {
            ended = true;
            instructions = List.copyOf(instructions);
            cache = null;
        }
        return this;
    }

    public AtomicMemoryAccessInsn add(Op.AtomicMemoryAccess op, int memory, int offset) {
        return add(new AtomicMemoryAccessInsn(op, memory, offset));
    }

    /**
     * Add a block instruction with the given type ID.
     * The given consumer should populate the sub-block; once it returns, the block will be ended.
     * To add a block which is not immediately terminated, use {@link #add(Insn)}.
     *
     * @param op the operation (must not be {@code null})
     * @param typeId the block type ID
     * @param insnConsumer the instruction builder (must not be {@code null})
     * @return the newly-added instruction (not {@code null})
     */
    public BlockInsn add(Op.Block op, int typeId, Consumer<BlockInsn> insnConsumer) {
        return add(op, new BlockInsn(op, cache, block, typeId), insnConsumer);
    }

    /**
     * Add a block instruction with the given simple type.
     * The given consumer should populate the sub-block; once it returns, the block will be ended.
     * To add a block which is not immediately terminated, use {@link #add(Insn)}.
     *
     * @param op the operation (must not be {@code null})
     * @param simpleType the block return type (must not be {@code null})
     * @param insnConsumer the instruction builder (must not be {@code null})
     * @return the newly-added instruction (not {@code null})
     */
    public BlockInsn add(Op.Block op, ValueType simpleType, Consumer<BlockInsn> insnConsumer) {
        return add(op, new BlockInsn(op, cache, block, simpleType), insnConsumer);
    }

    /**
     * Add a block instruction with an empty type.
     * The given consumer should populate the sub-block; once it returns, the block will be ended.
     * To add a block which is not immediately terminated, use {@link #add(Insn)}.
     *
     * @param op the operation (must not be {@code null})
     * @param insnConsumer the instruction builder (must not be {@code null})
     * @return the newly-added instruction (not {@code null})
     */
    public BlockInsn add(Op.Block op, Consumer<BlockInsn> insnConsumer) {
        return add(op, new BlockInsn(op, cache, block), insnConsumer);
    }

    private BlockInsn add(Op.Block op, BlockInsn insn, Consumer<BlockInsn> insnConsumer) {
        if (op == if_) {
            insn.body().allowElse();
        } else if (op == try_) {
            insn.body().allowCatch();
        }
        insnConsumer.accept(insn);
        insn.body().end();
        return add(insn);
    }

    /**
     * Add a label-indexed branch instruction.
     *
     * @param op the operation (must not be {@code null})
     * @param target the target block index
     * @return the newly-added instruction (not {@code null})
     */
    public BranchInsn add(Op.Branch op, int target) {
        return add(new BranchInsn(op, target));
    }

    /**
     * Add a branch instruction to an enclosing block.
     *
     * @param op the operation (must not be {@code null})
     * @param target the target block instruction (must not be {@code null} and must enclose this block)
     * @return the newly-added instruction (not {@code null})
     */
    public BranchInsn add(Op.Branch op, BlockInsn target) {
        int idx = 0;
        while (target != block) {
            if (target == null) {
                throw new IllegalArgumentException("Target does not refer to an enclosing block");
            }
            idx++;
            target = target.body().block;
        }
        return add(op, idx);
    }

    /**
     * Add a constant instruction.
     *
     * @param op the operation (must not be {@code null})
     * @param val the constant value
     * @return the newly-added instruction (not {@code null})
     */
    public ConstF32Insn add(Op.ConstF32 op, float val) {
        return add(new ConstF32Insn(op, val));
    }

    /**
     * Add a constant instruction.
     *
     * @param op the operation (must not be {@code null})
     * @param val the constant value
     * @return the newly-added instruction (not {@code null})
     */
    public ConstF64Insn add(Op.ConstF64 op, double val) {
        return add(new ConstF64Insn(op, val));
    }

    /**
     * Add a constant instruction.
     *
     * @param op the operation (must not be {@code null})
     * @param val the constant value
     * @return the newly-added instruction (not {@code null})
     */
    public ConstI32Insn add(Op.ConstI32 op, int val) {
        return add(new ConstI32Insn(op, val));
    }

    /**
     * Add a constant instruction.
     *
     * @param op the operation (must not be {@code null})
     * @param val the constant value
     * @return the newly-added instruction (not {@code null})
     */
    public ConstI64Insn add(Op.ConstI64 op, long val) {
        return add(new ConstI64Insn(op, val));
    }

    /**
     * Add a constant instruction.
     *
     * @param op the operation (must not be {@code null})
     * @param low the low 64 bits of the constant value (the high bits are set to zero)
     * @return the newly-added instruction (not {@code null})
     */
    public ConstV128Insn add(Op.ConstV128 op, long low) {
        return add(new ConstV128Insn(op, low, 0));
    }

    /**
     * Add a constant instruction.
     *
     * @param op the operation (must not be {@code null})
     * @param low the low 64 bits of the constant value
     * @param high the high 64 bits of the constant value
     * @return the newly-added instruction (not {@code null})
     */
    public ConstV128Insn add(Op.ConstV128 op, long low, long high) {
        return add(new ConstV128Insn(op, low, high));
    }

    public DataInsn add(Op.Data op, int segment) {
        return add(new DataInsn(op, segment));
    }

    public ElementAndTableInsn add(Op.ElementAndTable op, int element, int table) {
        return add(new ElementAndTableInsn(op, element, table));
    }

    public ElementInsn add(Op.Element op, int element) {
        return add(new ElementInsn(op, element));
    }

    public ExceptionInsn add(Op.Exception op, int target) {
        return add(new ExceptionInsn(op, target));
    }

    public ExceptionInsn add(Op.Exception op, BlockInsn target) {
        int idx = 0;
        while (target != block) {
            if (target == null) {
                throw new IllegalArgumentException("Target does not refer to an enclosing block");
            }
            idx++;
            target = target.body().block;
        }
        if (target.op() != try_) {
            throw new IllegalArgumentException(
                    "The target of an exception instruction must be a `try` instruction");
        }
        return add(op, idx);
    }

    public FuncInsn add(Op.Func op, int func) {
        return add(new FuncInsn(op, func));
    }

    public GlobalInsn add(Op.Global op, int global) {
        return add(new GlobalInsn(op, global));
    }

    public LaneInsn add(Op.Lane op, int laneIdx) {
        return add(new LaneInsn(op, laneIdx));
    }

    public LocalInsn add(Op.Local op, int local) {
        return add(new LocalInsn(op, local));
    }

    public MemoryInsn add(Op.Memory op) {
        return add(new MemoryInsn(op, 0));
    }

    public MemoryInsn add(Op.Memory op, int memory) {
        return add(new MemoryInsn(op, memory));
    }

    public MemoryAccessInsn add(Op.MemoryAccess op, int memory, int offset, int alignment) {
        return add(new MemoryAccessInsn(op, memory, offset, alignment));
    }

    public MemoryAccessLaneInsn add(
            Op.MemoryAccessLane op, int memory, int offset, int alignment, int laneIdx) {
        return add(new MemoryAccessLaneInsn(op, memory, offset, alignment, laneIdx));
    }

    public MemoryAndDataInsn add(Op.MemoryAndData op, int memory, int data) {
        return add(new MemoryAndDataInsn(op, memory, data));
    }

    public MemoryToMemoryInsn add(Op.MemoryToMemory op, int dest, int src) {
        return add(new MemoryToMemoryInsn(op, dest, src));
    }

    public MemoryToMemoryInsn add(Op.MemoryToMemory op) {
        return add(new MemoryToMemoryInsn(op, 0, 0));
    }

    public MultiBranchInsn add(Op.MultiBranch op, int[] targets, int defaultTarget) {
        return add(new MultiBranchInsn(op, targets, defaultTarget));
    }

    public RefTypedInsn add(Op.RefTyped op, ValueType type) {
        return add(RefTypedInsn.forOpAndType(op, type));
    }

    /**
     * Add a simple instruction.
     * If the instruction is {@link Ops#end end}, the sequence will be {@linkplain #end() ended}.
     *
     * @param op the op to add (must not be {@code null})
     * @return the newly-added instruction (not {@code null})
     */
    public SimpleInsn add(Op.Simple op) {
        return add(SimpleInsn.forOp(op));
    }

    public TableInsn add(Op.Table op, int table) {
        return add(new TableInsn(op, table));
    }

    public TableAndFuncTypeInsn add(Op.TableAndFuncType op, int table, int funcType) {
        return add(new TableAndFuncTypeInsn(op, table, funcType));
    }

    public TableToTableInsn add(Op.TableToTable op, int table1, int table2) {
        return add(new TableToTableInsn(op, table1, table2));
    }

    public TagInsn add(Op.Tag op, int tag) {
        return add(new TagInsn(op, tag));
    }

    public TypesInsn add(Op.Types op, ValueType type) {
        return add(TypesInsn.forOpAndType(op, type));
    }

    public TypesInsn add(Op.Types op, ValueType... types) {
        Objects.requireNonNull(op, "op");
        Objects.requireNonNull(types, "types");
        if (types.length == 1) {
            return add(op, types[0]);
        } else {
            return add(op, List.of(types));
        }
    }

    public TypesInsn add(Op.Types op, List<ValueType> types) {
        Objects.requireNonNull(op, "op");
        Objects.requireNonNull(types, "types");
        if (types.size() == 1) {
            return add(op, types.get(0));
        } else {
            return add(new TypesInsn(op, types));
        }
    }

    /**
     * {@return the cache in use by this sequence}
     * @throws IllegalStateException if the sequence is already {@linkplain #end() ended}
     */
    public Cache cache() {
        if (ended) {
            throw new IllegalStateException("The sequence is already ended");
        }
        return cache;
    }

    /**
     * {@return the index of the <code>else</code> instruction, or -1 if there is none}
     */
    public int elseIndex() {
        return elseIdx;
    }

    /**
     * {@return the index of the <code>catch_all</code> instruction, or -1 if there is none}
     */
    public int catchAllIndex() {
        return catchAllIdx;
    }

    /**
     * {@return the index of the <code>catch</code> instruction having the given tag, or -1 if there is none}
     */
    public int catchIndexForTag(int tag) {
        int res = findCatch(tag);
        return res < 0 ? -1 : catchIndexForNumber(res);
    }

    /**
     * {@return the number of <code>catch</code> instructions in this sequence}
     * Use the result with {@link #catchTagForNumber(int)} and {@link #catchIndexForNumber(int)}
     * to iterate the {@code catch} instructions.
     */
    public int catchCount() {
        return catchIndexes.length >>> 1;
    }

    /**
     * {@return the catch tag for the given catch number}
     * The catch number must be in the range {@code [0, catchCount())}.
     */
    public int catchTagForNumber(int id) throws IndexOutOfBoundsException {
        return catchIndexes[id << 1];
    }

    /**
     * {@return the catch instruction index for the given catch number}
     * The catch number must be in the range {@code [0, catchCount())}.
     */
    public int catchIndexForNumber(int id) throws IndexOutOfBoundsException {
        return catchIndexes[(id << 1) + 1];
    }

    /**
     * Find the index of a {@code catch} instruction using a fast binary search.
     *
     * @param tag the tag to search for
     * @return the index of the tag, or the insertion point (as {@code -idx - 1}) if the tag is not found
     */
    private int findCatch(int tag) {
        // the low end of the search region
        int lowIdx = 0;
        // the high end of the search region
        int highIdx = (catchIndexes.length >>> 1) - 1;

        while (lowIdx <= highIdx) {
            // somewhere in the middle...
            int midIdx = lowIdx + highIdx >>> 1;
            int foundTag = catchIndexes[midIdx << 1];

            if (foundTag < tag) {
                lowIdx = midIdx + 1;
            } else if (foundTag > tag) {
                highIdx = midIdx - 1;
            } else {
                // found it, return the pair index
                return midIdx;
            }
        }
        // negative index indicates not found
        return -lowIdx - 1 >>> 1;
    }

    @SuppressWarnings("unchecked")
    private <I extends Insn<?>> I getCached(final I instruction) {
        Cache cache = cache();
        I cached = (I) cache.map.get(instruction);
        if (cached != null) {
            return cached;
        }
        cache.map.put(instruction, instruction);
        return instruction;
    }

    @Override
    public Iterator<Insn<?>> iterator() {
        Iterator<Insn<?>> iterator = instructions.iterator();
        return new Iterator<Insn<?>>() {
            boolean endDelivered;

            @Override
            public boolean hasNext() {
                return iterator.hasNext() || ended && !endDelivered;
            }

            @Override
            public Insn<?> next() {
                if (iterator.hasNext()) {
                    return iterator.next();
                } else if (ended && !endDelivered) {
                    endDelivered = true;
                    return SimpleInsn.end;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }

    @Override
    public void forEach(Consumer<? super Insn<?>> action) {
        instructions.forEach(action);
        if (ended) {
            action.accept(SimpleInsn.end);
        }
    }

    public void forEachRecursive(Consumer<? super Insn<?>> action) {
        for (Insn<?> insn : this) {
            action.accept(insn);
            if (insn instanceof BlockInsn) {
                BlockInsn bi = (BlockInsn) insn;
                bi.body().forEachRecursive(action);
            }
        }
    }

    /**
     * {@return the hash code of the instruction list}
     * The sequence should be {@linkplain #end ended} to avoid unexpected results.
     */
    public int hashCode() {
        return instructions.hashCode();
    }

    public boolean equals(final Object obj) {
        return obj instanceof InsnSeq && equals((InsnSeq) obj);
    }

    public boolean equals(final InsnSeq other) {
        return this == other || other != null && instructions.equals(other.instructions);
    }

    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public StringBuilder toString(StringBuilder sb) {
        for (Insn<?> insn : this) {
            insn.toString(sb);
            if (insn instanceof BlockInsn) {
                sb.append(" { ... }");
            }
            sb.append('\n');
        }
        return sb;
    }

    /**
     * {@return the list of instructions}
     * The block must be {@linkplain #end() ended}.
     * This list does <em>not</em> include the terminating {@link Ops#end end} instruction.
     *
     * @throws IllegalStateException if the block is not yet ended
     */
    public List<Insn<?>> instructions() {
        if (!ended) {
            throw new IllegalStateException("Block was not yet ended");
        }
        return instructions;
    }

    /**
     * Populate this instruction sequence from the given buffer.
     *
     * @param in the stream to parse from (must not be {@code null})
     */
    public void parseFrom(final WasmInputStream in) {
        Op op;
        do {
            op = in.op();
            op.readFrom(in, this);
        } while (op != end);
    }

    /**
     * An instruction cache, used while building the sequence.
     */
    public static final class Cache {
        final HashMap<Insn<?>, Insn<?>> map;

        /**
         * Construct a new instance.
         */
        public Cache() {
            this(new HashMap<>());
        }

        /**
         * Construct a new instance.
         *
         * @param estimatedSize the estimated cache size (hint)
         */
        public Cache(final int estimatedSize) {
            this(new HashMap<>(estimatedSize));
        }

        private Cache(HashMap<Insn<?>, Insn<?>> map) {
            this.map = map;
        }
    }
}
