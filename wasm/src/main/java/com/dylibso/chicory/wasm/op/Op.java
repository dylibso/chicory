package com.dylibso.chicory.wasm.op;

import com.dylibso.chicory.wasm.io.WasmIOException;
import com.dylibso.chicory.wasm.io.WasmInputStream;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.List;

/**
 * A raw operation of some kind.
 */
// some ops have weird spellings by spec; don't trip on them.
// some ctors are unused but leave them for ease of expansion later on.
@SuppressWarnings({"SpellCheckingInspection", "unused"})
public /*sealed*/ interface Op /*permits Op.AtomicMemoryAccess,
                                   Op.Block,
                                   Op.ConstF32,
                                   Op.ConstF64,
                                   Op.ConstI32,
                                   Op.ConstI64,
                                   Op.ConstV128,
                                   Op.Data,
                                   Op.MemoryAndData,
                                   Op.Element,
                                   Op.ElementAndTable,
                                   Op.Exception,
                                   Op.Func,
                                   Op.Global,
                                   Op.Local,
                                   Op.Branch,
                                   Op.Lane,
                                   Op.Memory,
                                   Op.MemoryToMemory,
                                   Op.MemoryAccess,
                                   Op.MemoryAccessLane,
                                   Op.MultiBranch,
                                   Op.Prefix,
                                   Op.RefTyped,
                                   Op.Simple,
                                   Op.Table,
                                   Op.TableToTable,
                                   Op.TableAndFuncType,
                                   Op.Tag,
                                   Op.Types*/ {

    /**
     * {@return the numerical opcode for this operation}
     * @see Opcodes
     */
    int opcode();

    /**
     * {@return the secondary opcode for this operation, or -1 if there is no prefix}
     * @see Opcodes
     */
    int secondaryOpcode();

    /**
     * {@return the kind of operation}
     */
    Kind kind();

    /**
     * Read the instruction arguments corresponding to this opcode from the stream and
     * add the resultant instruction to the given sequence.
     *
     * @param in the input stream (must not be {@code null})
     * @param seq the instruction sequence (must not be {@code null})
     * @throws WasmIOException if an underlying I/O error occurred
     */
    void readFrom(WasmInputStream in, InsnSeq seq) throws WasmIOException;

    /**
     * Skip the instruction arguments correponding to this opcode in the stream.
     *
     * @param in the input stream (must not be {@code null})
     * @throws WasmIOException if an underlying I/O error occurred
     */
    void skip(WasmInputStream in) throws WasmIOException;

    enum Kind {
        ATOMIC_MEMORY_ACCESS(AtomicMemoryAccess.class),
        BLOCK(Block.class),
        BRANCH(Branch.class),
        CONST_F32(ConstF32.class),
        CONST_F64(ConstF64.class),
        CONST_I32(ConstI32.class),
        CONST_I64(ConstI64.class),
        CONST_V128(ConstV128.class),
        DATA(Data.class),
        ELEMENT_AND_TABLE(ElementAndTable.class),
        ELEMENT(Element.class),
        EXCEPTION(Exception.class),
        FUNC(Func.class),
        GLOBAL(Global.class),
        LANE(Lane.class),
        LOCAL(Local.class),
        MEMORY(Memory.class),
        MEMORY_ACCESS(MemoryAccess.class),
        MEMORY_ACCESS_LANE(MemoryAccessLane.class),
        MEMORY_AND_DATA(MemoryAndData.class),
        MEMORY_AND_MEMORY(MemoryToMemory.class),
        MULTI_BRANCH(MultiBranch.class),
        PREFIX(Prefix.class),
        REF_TYPED(RefTyped.class),
        SIMPLE(Simple.class),
        TABLE(Table.class),
        TABLE_AND_FUNC_TYPE(TableAndFuncType.class),
        TABLE_AND_TABLE(TableToTable.class),
        TAG(Tag.class),
        TYPES(Types.class),
        ;

        private static final List<Kind> kinds = List.of(values());
        private final Class<? extends Op> opClass;
        private final List<Op> ops;

        Kind(Class<? extends Op> opClass) {
            this.opClass = opClass;
            ops = List.of(opClass.getEnumConstants());
        }

        public Class<? extends Op> opClass() {
            return opClass;
        }

        public List<Op> ops() {
            return ops;
        }

        public static List<Kind> all() {
            return kinds;
        }
    }

    enum Block implements Op {
        block("block", Opcodes.OP_BLOCK),
        loop("loop", Opcodes.OP_LOOP),
        if_("if", Opcodes.OP_IF),
        try_("try", Opcodes.OP_TRY),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        Block(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        Block(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.BLOCK;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            int b = in.peekRawByteOpt();
            if (b == 0x40) {
                in.rawByte();
                seq.add(this, insn -> insn.body().readFrom(in));
            } else if (ValueType.forId(b) != null) {
                in.rawByte();
                seq.add(this, ValueType.forId(b), insn -> insn.body().readFrom(in));
            } else {
                seq.add(this, in.u31(), insn -> insn.body().readFrom(in));
            }
        }

        @Override
        public void skip(WasmInputStream in) {
            int b = in.peekRawByteOpt();
            if (b == 0x40) {
                in.rawByte();
            } else if (ValueType.forId(b) != null) {
                in.rawByte();
            } else {
                in.u32();
            }
            InsnSeq.skip(in);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Branch implements Op {
        // label index
        br("br", Opcodes.OP_BR),
        br_if("br_if", Opcodes.OP_BR_IF),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        Branch(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        Branch(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.BRANCH;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this, in.u31());
        }

        @Override
        public void skip(WasmInputStream in) {
            in.u31();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum ConstF32 implements Op {
        f32_const("f32.const", Opcodes.OP_F32_CONST),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        ConstF32(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        ConstF32(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.CONST_F32;
        }

        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this, in.f32());
        }

        @Override
        public void skip(WasmInputStream in) {
            in.f32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum ConstF64 implements Op {
        f64_const("f64.const", Opcodes.OP_F64_CONST),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        ConstF64(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        ConstF64(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.CONST_F64;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this, in.f64());
        }

        @Override
        public void skip(WasmInputStream in) {
            in.f64();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum ConstI32 implements Op {
        i32_const("i32.const", Opcodes.OP_I32_CONST),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        ConstI32(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        ConstI32(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.CONST_I32;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this, in.s32());
        }

        @Override
        public void skip(WasmInputStream in) {
            in.s32();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum ConstI64 implements Op {
        i64_const("i64.const", Opcodes.OP_I64_CONST),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        ConstI64(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        ConstI64(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.CONST_I64;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this, in.s64());
        }

        @Override
        public void skip(WasmInputStream in) {
            in.s64();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum ConstV128 implements Op {
        v128_const("v128.const", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_CONST),
        ;
        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        ConstV128(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        ConstV128(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.CONST_V128;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this, in.u64(), in.u64());
        }

        @Override
        public void skip(WasmInputStream in) {
            in.u64();
            in.u64();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Data implements Op {
        // data index
        data_drop("data.drop", Opcodes.OP_PREFIX_FC, Opcodes.OP_FC_DATA_DROP),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        Data(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        Data(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.DATA;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this, in.u31());
        }

        @Override
        public void skip(WasmInputStream in) {
            in.u31();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Element implements Op {
        // element index
        elem_drop("elem.drop", Opcodes.OP_PREFIX_FC, Opcodes.OP_FC_ELEM_DROP),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        Element(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        Element(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.ELEMENT;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this, in.u31());
        }

        @Override
        public void skip(WasmInputStream in) {
            in.u31();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum ElementAndTable implements Op {
        // elemidx, tableidx
        table_init("table.init", Opcodes.OP_PREFIX_FC, Opcodes.OP_FC_TABLE_INIT),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        ElementAndTable(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        ElementAndTable(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.ELEMENT_AND_TABLE;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this, in.u31(), in.u31());
        }

        @Override
        public void skip(WasmInputStream in) {
            in.u31();
            in.u31();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Exception implements Op {
        // label index
        delegate("delegate", Opcodes.OP_DELEGATE),
        rethrow("rethrow", Opcodes.OP_RETHROW),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        Exception(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        Exception(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.EXCEPTION;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this, in.u31());
        }

        @Override
        public void skip(WasmInputStream in) {
            in.u31();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Func implements Op {
        // function index
        call("call", Opcodes.OP_CALL),
        ref_func("ref.func", Opcodes.OP_REF_FUNC),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        Func(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        Func(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.FUNC;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this, in.u31());
        }

        @Override
        public void skip(WasmInputStream in) {
            in.u31();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Global implements Op {
        // global index
        global_get("global.get", Opcodes.OP_GLOBAL_GET),
        global_set("global.set", Opcodes.OP_GLOBAL_SET),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        Global(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        Global(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.GLOBAL;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this, in.u31());
        }

        @Override
        public void skip(WasmInputStream in) {
            in.u31();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Lane implements Op {
        // vector lane index
        i8x16_extract_lane_s(
                "i8x16.extract_lane_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_EXTRACT_LANE_S),
        i8x16_extract_lane_u(
                "i8x16.extract_lane_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_EXTRACT_LANE_S),
        i8x16_replace_lane(
                "i8x16.replace_lane", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_REPLACE_LANE),
        i8x16_shuffle("i8x16.shuffle", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_SHUFFLE),
        i16x8_extract_lane_s(
                "i16x8.extract_lane_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_EXTRACT_LANE_S),
        i16x8_extract_lane_u(
                "i16x8.extract_lane_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_EXTRACT_LANE_U),
        i16x8_replace_lane(
                "i16x8.replace_lane", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_REPLACE_LANE),
        i32x4_extract_lane(
                "i32x4.extract_lane", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_EXTRACT_LANE),
        i32x4_replace_lane(
                "i32x4.replace_lane", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_REPLACE_LANE),
        i64x2_extract_lane(
                "i64x2.extract_lane", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I64X2_EXTRACT_LANE),
        i64x2_replace_lane(
                "i64x2.replace_lane", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I64X2_REPLACE_LANE),
        f32x4_extract_lane(
                "f32x4.extract_lane", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_EXTRACT_LANE),
        f32x4_replace_lane(
                "f32x4.replace_lane", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_REPLACE_LANE),
        f64x2_extract_lane(
                "f64x2.extract_lane", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_EXTRACT_LANE),
        f64x2_replace_lane(
                "f64x2.replace_lane", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_REPLACE_LANE),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        Lane(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        Lane(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.LANE;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this, in.u8());
        }

        @Override
        public void skip(WasmInputStream in) {
            in.u8();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Local implements Op {
        // local index
        local_get("local.get", Opcodes.OP_LOCAL_GET),
        local_set("local.set", Opcodes.OP_LOCAL_SET),
        local_tee("local.tee", Opcodes.OP_LOCAL_TEE),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        Local(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        Local(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.LOCAL;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this, in.u31());
        }

        @Override
        public void skip(WasmInputStream in) {
            in.u31();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Memory implements Op {
        // memory index
        memory_fill("memory.fill", Opcodes.OP_PREFIX_FC, Opcodes.OP_FC_MEMORY_FILL),
        memory_grow("memory.grow", Opcodes.OP_MEMORY_GROW),
        memory_size("memory.size", Opcodes.OP_MEMORY_SIZE),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        Memory(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        Memory(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.MEMORY;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this, in.u31());
        }

        @Override
        public void skip(WasmInputStream in) {
            in.u31();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum AtomicMemoryAccess implements Op {
        i32_atomic_load("i32.atomic.load", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I32_ATOMIC_LOAD, 4),
        i32_atomic_load16_u(
                "i32.atomic.load16_u", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I32_ATOMIC_LOAD16_U, 2),
        i32_atomic_load8_u(
                "i32.atomic.load8_u", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I32_ATOMIC_LOAD8_U, 1),
        i32_atomic_rmw16_add_u(
                "i32.atomic.rmw16.add_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I32_ATOMIC_RMW16_ADD_U,
                2),
        i32_atomic_rmw16_and_u(
                "i32.atomic.rmw16.and_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I32_ATOMIC_RMW16_AND_U,
                2),
        i32_atomic_rmw16_cmpxchg_u(
                "i32.atomic.rmw16.cmpxchg_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I32_ATOMIC_RMW16_CMPXCHG_U,
                2),
        i32_atomic_rmw16_or_u(
                "i32.atomic.rmw16.or_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I32_ATOMIC_RMW16_OR_U,
                2),
        i32_atomic_rmw16_sub_u(
                "i32.atomic.rmw16.sub_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I32_ATOMIC_RMW16_SUB_U,
                2),
        i32_atomic_rmw16_xchg_u(
                "i32.atomic.rmw16.xchg_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I32_ATOMIC_RMW16_XCHG_U,
                2),
        i32_atomic_rmw16_xor_u(
                "i32.atomic.rmw16.xor_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I32_ATOMIC_RMW16_XOR_U,
                2),
        i32_atomic_rmw8_add_u(
                "i32.atomic.rmw8.add_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I32_ATOMIC_RMW8_ADD_U,
                1),
        i32_atomic_rmw8_and_u(
                "i32.atomic.rmw8.and_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I32_ATOMIC_RMW8_AND_U,
                1),
        i32_atomic_rmw8_cmpxchg_u(
                "i32.atomic.rmw8.cmpxchg_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I32_ATOMIC_RMW8_CMPXCHG_U,
                1),
        i32_atomic_rmw8_or_u(
                "i32.atomic.rmw8.or_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I32_ATOMIC_RMW8_OR_U,
                1),
        i32_atomic_rmw8_sub_u(
                "i32.atomic.rmw8.sub_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I32_ATOMIC_RMW8_SUB_U,
                1),
        i32_atomic_rmw8_xchg_u(
                "i32.atomic.rmw8.xchg_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I32_ATOMIC_RMW8_XCHG_U,
                1),
        i32_atomic_rmw8_xor_u(
                "i32.atomic.rmw8.xor_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I32_ATOMIC_RMW8_XOR_U,
                1),
        i32_atomic_rmw_add(
                "i32.atomic.rmw.add", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I32_ATOMIC_RMW_ADD, 4),
        i32_atomic_rmw_and(
                "i32.atomic.rmw.and", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I32_ATOMIC_RMW_AND, 4),
        i32_atomic_rmw_cmpxchg(
                "i32.atomic.rmw.cmpxchg",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I32_ATOMIC_RMW_CMPXCHG,
                4),
        i32_atomic_rmw_or(
                "i32.atomic.rmw.or", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I32_ATOMIC_RMW_OR, 4),
        i32_atomic_rmw_sub(
                "i32.atomic.rmw.sub", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I32_ATOMIC_RMW_SUB, 4),
        i32_atomic_rmw_xchg(
                "i32.atomic.rmw.xchg", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I32_ATOMIC_RMW_XCHG, 4),
        i32_atomic_rmw_xor(
                "i32.atomic.rmw.xor", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I32_ATOMIC_RMW_XOR, 4),
        i32_atomic_store(
                "i32.atomic.store", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I32_ATOMIC_STORE, 4),
        i32_atomic_store16(
                "i32.atomic.store16", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I32_ATOMIC_STORE16, 2),
        i32_atomic_store8(
                "i32.atomic.store8", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I32_ATOMIC_STORE8, 1),
        i64_atomic_load("i64.atomic.load", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I64_ATOMIC_LOAD, 8),
        i64_atomic_load16_u(
                "i64.atomic.load16_u", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I64_ATOMIC_LOAD16_U, 2),
        i64_atomic_load32_u(
                "i64.atomic.load32_u", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I64_ATOMIC_LOAD32_U, 4),
        i64_atomic_load8_u(
                "i64.atomic.load8_u", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I64_ATOMIC_LOAD8_U, 1),
        i64_atomic_rmw16_add_u(
                "i64.atomic.rmw16.add_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW16_ADD_U,
                2),
        i64_atomic_rmw16_and_u(
                "i64.atomic.rmw16.and_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW16_AND_U,
                2),
        i64_atomic_rmw16_cmpxchg_u(
                "i64.atomic.rmw16.cmpxchg_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW16_CMPXCHG_U,
                2),
        i64_atomic_rmw16_or_u(
                "i64.atomic.rmw16.or_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW16_OR_U,
                2),
        i64_atomic_rmw16_sub_u(
                "i64.atomic.rmw16.sub_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW16_SUB_U,
                2),
        i64_atomic_rmw16_xchg_u(
                "i64.atomic.rmw16.xchg_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW16_XCHG_U,
                2),
        i64_atomic_rmw16_xor_u(
                "i64.atomic.rmw16.xor_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW16_XOR_U,
                2),
        i64_atomic_rmw32_add_u(
                "i64.atomic.rmw32.add_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW32_ADD_U,
                4),
        i64_atomic_rmw32_and_u(
                "i64.atomic.rmw32.and_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW32_AND_U,
                4),
        i64_atomic_rmw32_cmpxchg_u(
                "i64.atomic.rmw32.cmpxchg_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW32_CMPXCHG_U,
                4),
        i64_atomic_rmw32_or_u(
                "i64.atomic.rmw32.or_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW32_OR_U,
                4),
        i64_atomic_rmw32_sub_u(
                "i64.atomic.rmw32.sub_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW32_SUB_U,
                4),
        i64_atomic_rmw32_xchg_u(
                "i64.atomic.rmw32.xchg_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW32_XCHG_U,
                4),
        i64_atomic_rmw32_xor_u(
                "i64.atomic.rmw32.xor_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW32_XOR_U,
                4),
        i64_atomic_rmw8_add_u(
                "i64.atomic.rmw8.add_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW8_ADD_U,
                1),
        i64_atomic_rmw8_and_u(
                "i64.atomic.rmw8.and_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW8_AND_U,
                1),
        i64_atomic_rmw8_cmpxchg_u(
                "i64.atomic.rmw8.cmpxchg_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW8_CMPXCHG_U,
                1),
        i64_atomic_rmw8_or_u(
                "i64.atomic.rmw8.or_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW8_OR_U,
                1),
        i64_atomic_rmw8_sub_u(
                "i64.atomic.rmw8.sub_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW8_SUB_U,
                1),
        i64_atomic_rmw8_xchg_u(
                "i64.atomic.rmw8.xchg_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW8_XCHG_U,
                1),
        i64_atomic_rmw8_xor_u(
                "i64.atomic.rmw8.xor_u",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW8_XOR_U,
                1),
        i64_atomic_rmw_add(
                "i64.atomic.rmw.add", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I64_ATOMIC_RMW_ADD, 8),
        i64_atomic_rmw_and(
                "i64.atomic.rmw.and", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I64_ATOMIC_RMW_AND, 8),
        i64_atomic_rmw_cmpxchg(
                "i64.atomic.rmw.cmpxchg",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_I64_ATOMIC_RMW_CMPXCHG,
                8),
        i64_atomic_rmw_or(
                "i64.atomic.rmw.or", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I64_ATOMIC_RMW_OR, 8),
        i64_atomic_rmw_sub(
                "i64.atomic.rmw.sub", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I64_ATOMIC_RMW_SUB, 8),
        i64_atomic_rmw_xchg(
                "i64.atomic.rmw.xchg", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I64_ATOMIC_RMW_XCHG, 8),
        i64_atomic_rmw_xor(
                "i64.atomic.rmw.xor", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I64_ATOMIC_RMW_XOR, 8),
        i64_atomic_store(
                "i64.atomic.store", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I64_ATOMIC_STORE, 8),
        i64_atomic_store16(
                "i64.atomic.store16", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I64_ATOMIC_STORE16, 2),
        i64_atomic_store32(
                "i64.atomic.store32", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I64_ATOMIC_STORE32, 4),
        i64_atomic_store8(
                "i64.atomic.store8", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_I64_ATOMIC_STORE8, 1),
        memory_atomic_notify(
                "memory.atomic.notify",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_MEMORY_ATOMIC_NOTIFY,
                4),
        memory_atomic_wait32(
                "memory.atomic.wait32",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_MEMORY_ATOMIC_WAIT32,
                4),
        memory_atomic_wait64(
                "memory.atomic.wait64",
                Opcodes.OP_PREFIX_FE,
                Opcodes.OP_FE_MEMORY_ATOMIC_WAIT64,
                8),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;
        private final int alignment;

        AtomicMemoryAccess(String name, int opcode, int secondaryOpcode, int alignment) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
            this.alignment = alignment;
        }

        AtomicMemoryAccess(String name, int opcode, int alignment) {
            this(name, opcode, -1, alignment);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        public int alignment() {
            return alignment;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.ATOMIC_MEMORY_ACCESS;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this, in.u31(), in.u31());
        }

        @Override
        public void skip(WasmInputStream in) {
            in.u31();
            in.u31();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum MemoryAccess implements Op {
        f32_load("f32.load", Opcodes.OP_F32_LOAD, 4),
        f32_store("f32.store", Opcodes.OP_F32_STORE, 4),
        f64_load("f64.load", Opcodes.OP_F64_LOAD, 8),
        f64_store("f64.store", Opcodes.OP_F64_STORE, 8),
        i32_load("i32.load", Opcodes.OP_I32_LOAD, 4),
        i32_load16_s("i32.load16_s", Opcodes.OP_I32_LOAD16_S, 2),
        i32_load16_u("i32.load16_u", Opcodes.OP_I32_LOAD16_U, 2),
        i32_load8_s("i32.load8_s", Opcodes.OP_I32_LOAD8_S, 2),
        i32_load8_u("i32.load8_u", Opcodes.OP_I32_LOAD8_U, 2),
        i32_store("i32.store", Opcodes.OP_I32_STORE, 3),
        i32_store16("i32.store16", Opcodes.OP_I32_STORE16, 2),
        i32_store8("i32.store8", Opcodes.OP_I32_STORE8, 1),
        i64_load("i64.load", Opcodes.OP_I64_LOAD, 8),
        i64_load16_s("i64.load16_s", Opcodes.OP_I64_LOAD16_S, 4),
        i64_load16_u("i64.load16_u", Opcodes.OP_I64_LOAD16_U, 4),
        i64_load32_s("i64.load32_s", Opcodes.OP_I64_LOAD32_S, 8),
        i64_load32_u("i64.load32_u", Opcodes.OP_I64_LOAD32_U, 8),
        i64_load8_s("i64.load8_s", Opcodes.OP_I64_LOAD8_S, 1),
        i64_load8_u("i64.load8_u", Opcodes.OP_I64_LOAD8_U, 1),
        i64_store("i64.store", Opcodes.OP_I64_STORE, 8),
        i64_store16("i64.store16", Opcodes.OP_I64_STORE16, 2),
        i64_store32("i64.store32", Opcodes.OP_I64_STORE32, 4),
        i64_store8("i64.store8", Opcodes.OP_I64_STORE8, 1),
        v128_load("v128.load", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_LOAD, 16),
        v128_load16_splat(
                "v128.load16_splat", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_LOAD16_SPLAT, 2),
        v128_load16x4_s("v128.load16x4_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_LOAD16X4_S, 8),
        v128_load16x4_u("v128.load16x4_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_LOAD16X4_U, 8),
        v128_load32_splat(
                "v128.load32_splat", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_LOAD32_SPLAT, 4),
        v128_load32_zero(
                "v128.load32_zero", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_LOAD32_ZERO, 4),
        v128_load32x2_s("v128.load32x2_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_LOAD32X2_S, 8),
        v128_load32x2_u("v128.load32x2_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_LOAD32X2_U, 8),
        v128_load64_splat(
                "v128.load64_splat", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_LOAD64_SPLAT, 8),
        v128_load64_zero(
                "v128.load64_zero", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_LOAD64_ZERO, 8),
        v128_load8_splat(
                "v128.load8_splat", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_LOAD8_SPLAT, 1),
        v128_load8x8_s("v128.load8x8_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_LOAD8X8_S, 8),
        v128_load8x8_u("v128.load8x8_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_LOAD8X8_U, 8),
        v128_store("v128.store", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_STORE, 16),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;
        private final int alignment;

        MemoryAccess(String name, int opcode, int secondaryOpcode, int alignment) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
            this.alignment = alignment;
        }

        MemoryAccess(String name, int opcode, int alignment) {
            this(name, opcode, -1, alignment);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.MEMORY_ACCESS;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            boolean hasMem = (in.peekRawByte() & 0x40) != 0;
            int alignShift = in.u8() & 0b111111;
            int memIdx = hasMem ? in.u31() : 0;
            int offset = in.u31();
            seq.add(this, memIdx, offset, 1 << alignShift);
        }

        @Override
        public void skip(WasmInputStream in) {
            boolean hasMem = (in.peekRawByte() & 0x40) != 0;
            in.u8();
            if (hasMem) in.u31();
            in.u31();
        }

        @Override
        public String toString() {
            return name;
        }

        public int alignment() {
            return alignment;
        }
    }

    enum MemoryAccessLane implements Op {
        v128_load8_lane("v128.load8_lane", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_LOAD8_LANE, 1),
        v128_load16_lane(
                "v128.load16_lane", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_LOAD16_LANE, 2),
        v128_load32_lane(
                "v128.load32_lane", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_LOAD32_LANE, 4),
        v128_load64_lane(
                "v128.load64_lane", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_LOAD64_LANE, 8),
        v128_store8_lane(
                "v128.store8_lane", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_STORE8_LANE, 1),
        v128_store16_lane(
                "v128.store16_lane", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_STORE16_LANE, 2),
        v128_store32_lane(
                "v128.store32_lane", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_STORE32_LANE, 4),
        v128_store64_lane(
                "v128.store64_lane", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_STORE64_LANE, 8),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;
        private final int alignment;

        MemoryAccessLane(String name, int opcode, int secondaryOpcode, int alignment) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
            this.alignment = alignment;
        }

        MemoryAccessLane(String name, int opcode, int alignment) {
            this(name, opcode, -1, alignment);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.MEMORY_ACCESS_LANE;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            boolean hasMem = (in.peekRawByte() & 0x40) != 0;
            int alignShift = in.u8() & 0b111111;
            int memIdx = hasMem ? in.u31() : 0;
            int offset = in.u31();
            int lane = in.u8();
            seq.add(this, memIdx, offset, 1 << alignShift, lane);
        }

        @Override
        public void skip(WasmInputStream in) {
            boolean hasMem = (in.peekRawByte() & 0x40) != 0;
            in.u8();
            if (hasMem) in.u31();
            in.u31();
            in.u8();
        }

        @Override
        public String toString() {
            return name;
        }

        public int alignment() {
            return alignment;
        }
    }

    enum MemoryAndData implements Op {
        // dataidx, memidx
        memory_init("memory.init", Opcodes.OP_PREFIX_FC, Opcodes.OP_FC_MEMORY_INIT),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        MemoryAndData(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        MemoryAndData(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.MEMORY_AND_DATA;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            int segIdx = in.u31();
            int memIdx = in.u31();
            seq.add(this, memIdx, segIdx);
        }

        @Override
        public void skip(WasmInputStream in) {
            int segIdx = in.u31();
            int memIdx = in.u31();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum MemoryToMemory implements Op {
        // memidx, memidx
        memory_copy("memory.copy", Opcodes.OP_PREFIX_FC, Opcodes.OP_FC_MEMORY_COPY),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        MemoryToMemory(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        MemoryToMemory(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.MEMORY_AND_MEMORY;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this, in.u31(), in.u31());
        }

        @Override
        public void skip(WasmInputStream in) {
            in.u31();
            in.u31();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum MultiBranch implements Op {
        br_table("br_table", Opcodes.OP_BR_TABLE);

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        MultiBranch(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        MultiBranch(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.MULTI_BRANCH;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            int cnt = in.u31();
            int[] targets = new int[cnt];
            for (int i = 0; i < cnt; i++) {
                targets[i] = in.u31();
            }
            int defTarget = in.u31();
            seq.add(this, targets, defTarget);
        }

        @Override
        public void skip(WasmInputStream in) {
            int cnt = in.u31();
            for (int i = 0; i < cnt; i++) {
                in.u31();
            }
            in.u31();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Prefix implements Op {
        fc(Opcodes.OP_PREFIX_FC),
        fd(Opcodes.OP_PREFIX_FD),
        fe(Opcodes.OP_PREFIX_FE),
        ;
        private final int opcode;

        Prefix(int opcode) {
            this.opcode = opcode;
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return -1;
        }

        @Override
        public Kind kind() {
            return Kind.PREFIX;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            // no arguments
        }

        @Override
        public void skip(WasmInputStream in) {
            // no arguments
        }

        @Override
        public String toString() {
            return "0x" + name();
        }
    }

    enum RefTyped implements Op {
        ref_null("ref.null", Opcodes.OP_REF_NULL),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        RefTyped(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        RefTyped(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.REF_TYPED;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this, in.refType());
        }

        @Override
        public void skip(WasmInputStream in) {
            in.refType();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Instructions which take no arguments.
     * For code generation, use the constants in {@link Ops} instead.
     */
    enum Simple implements Op {
        catch_all("catch_all", Opcodes.OP_CATCH),
        drop("drop", Opcodes.OP_DROP),
        else_("else", Opcodes.OP_ELSE),
        end("end", Opcodes.OP_END),
        f32_abs("f32.abs", Opcodes.OP_F32_ABS),
        f32_add("f32.add", Opcodes.OP_F32_ADD),
        f32_ceil("f32.ceil", Opcodes.OP_F32_CEIL),
        f32_convert_i32_s("f32.convert_i32_s", Opcodes.OP_F32_CONVERT_I32_S),
        f32_convert_i32_u("f32.convert_i32_u", Opcodes.OP_F32_CONVERT_I32_U),
        f32_convert_i64_s("f32.convert_i64_s", Opcodes.OP_F32_CONVERT_I64_S),
        f32_convert_i64_u("f32.convert_i64_u", Opcodes.OP_F32_CONVERT_I64_U),
        f32_copysign("f32.copysign", Opcodes.OP_F32_COPYSIGN),
        f32_demote_f64("f32.demote_f64", Opcodes.OP_F32_DEMOTE_F64),
        f32_div("f32.div", Opcodes.OP_F32_DIV),
        f32_eq("f32.eq", Opcodes.OP_F32_EQ),
        f32_floor("f32.floor", Opcodes.OP_F32_FLOOR),
        f32_ge("f32.ge", Opcodes.OP_F32_GE),
        f32_gt("f32.gt", Opcodes.OP_F32_GT),
        f32_le("f32.le", Opcodes.OP_F32_LE),
        f32_lt("f32.lt", Opcodes.OP_F32_LT),
        f32_max("f32.max", Opcodes.OP_F32_MAX),
        f32_min("f32.min", Opcodes.OP_F32_MIN),
        f32_mul("f32.mul", Opcodes.OP_F32_MUL),
        f32_ne("f32.ne", Opcodes.OP_F32_NE),
        f32_nearest("f32.nearest", Opcodes.OP_F32_NEAREST),
        f32_neg("f32.neg", Opcodes.OP_F32_NEG),
        f32_reinterpret_i32("f32.reinterpret_i32", Opcodes.OP_F32_REINTERPRET_I32),
        f32_sqrt("f32.sqrt", Opcodes.OP_F32_SQRT),
        f32_sub("f32.sub", Opcodes.OP_F32_SUB),
        f32_trunc("f32.trunc", Opcodes.OP_F32_TRUNC),
        f32x4_abs("f32x4.abs", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_ABS),
        f32x4_add("f32x4.add", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_ADD),
        f32x4_ceil("f32x4.ceil", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_CEIL),
        f32x4_convert_i32x4_s(
                "f32x4.convert_i32x4_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_CONVERT_I32X4_S),
        f32x4_convert_i32x4_u(
                "f32x4.convert_i32x4_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_CONVERT_I32X4_U),
        f32x4_demote_f64x2_zero(
                "f32x4.demote_f64x2_zero",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_F32X4_DEMOTE_F64X2_ZERO),
        f32x4_div("f32x4.div", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_DIV),
        f32x4_eq("f32x4.eq", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_EQ),
        f32x4_floor("f32x4.floor", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_FLOOR),
        f32x4_ge("f32x4.ge", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_GE),
        f32x4_gt("f32x4.gt", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_GT),
        f32x4_le("f32x4.le", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_LE),
        f32x4_lt("f32x4.lt", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_LT),
        f32x4_max("f32x4.max", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_MAX),
        f32x4_min("f32x4.min", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_MIN),
        f32x4_mul("f32x4.mul", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_MUL),
        f32x4_ne("f32x4.ne", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_NE),
        f32x4_nearest("f32x4.nearest", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_NEAREST),
        f32x4_neg("f32x4.neg", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_NEG),
        f32x4_pmax("f32x4.pmax", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_PMAX),
        f32x4_pmin("f32x4.pmin", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_PMIN),
        f32x4_splat("f32x4.splat", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_SPLAT),
        f32x4_sqrt("f32x4.sqrt", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_SQRT),
        f32x4_sub("f32x4.sub", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_SUB),
        f32x4_trunc("f32x4.trunc", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F32X4_TRUNC),
        f64_abs("f64.abs", Opcodes.OP_F64_ABS),
        f64_add("f64.add", Opcodes.OP_F64_ADD),
        f64_ceil("f64.ceil", Opcodes.OP_F64_CEIL),
        f64_convert_i32_s("f64.convert_i32_s", Opcodes.OP_F64_CONVERT_I32_S),
        f64_convert_i32_u("f64.convert_i32_u", Opcodes.OP_F64_CONVERT_I32_U),
        f64_convert_i64_s("f64.convert_i64_s", Opcodes.OP_F64_CONVERT_I64_S),
        f64_convert_i64_u("f64.convert_i64_u", Opcodes.OP_F64_CONVERT_I64_U),
        f64_copysign("f64.copysign", Opcodes.OP_F64_COPYSIGN),
        f64_div("f64.div", Opcodes.OP_F64_DIV),
        f64_eq("f64.eq", Opcodes.OP_F64_EQ),
        f64_floor("f64.floor", Opcodes.OP_F64_FLOOR),
        f64_ge("f64.ge", Opcodes.OP_F64_GE),
        f64_gt("f64.gt", Opcodes.OP_F64_GT),
        f64_le("f64.le", Opcodes.OP_F64_LE),
        f64_lt("f64.lt", Opcodes.OP_F64_LT),
        f64_max("f64.max", Opcodes.OP_F64_MAX),
        f64_min("f64.min", Opcodes.OP_F64_MIN),
        f64_mul("f64.mul", Opcodes.OP_F64_MUL),
        f64_ne("f64.ne", Opcodes.OP_F64_NE),
        f64_nearest("f64.nearest", Opcodes.OP_F64_NEAREST),
        f64_neg("f64.neg", Opcodes.OP_F64_NEG),
        f64_promote_f32("f64.promote_f32", Opcodes.OP_F64_PROMOTE_F32),
        f64_reinterpret_i64("f64.reinterpret_i64", Opcodes.OP_F64_REINTERPRET_I64),
        f64_sqrt("f64.sqrt", Opcodes.OP_F64_SQRT),
        f64_sub("f64.sub", Opcodes.OP_F64_SUB),
        f64_trunc("f64.trunc", Opcodes.OP_F64_TRUNC),
        f64x2_abs("f64x2.abs", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_ABS),
        f64x2_add("f64x2.add", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_ADD),
        f64x2_ceil("f64x2.ceil", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_CEIL),
        f64x2_convert_low_i32x4_s(
                "f64x2.convert_low_i32x4_s",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_F64X2_CONVERT_LOW_I32X4_S),
        f64x2_convert_low_i32x4_u(
                "f64x2.convert_low_i32x4_u",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_F64X2_CONVERT_LOW_I32X4_U),
        f64x2_div("f64x2.div", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_DIV),
        f64x2_eq("f64x2.eq", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_EQ),
        f64x2_floor("f64x2.floor", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_FLOOR),
        f64x2_ge("f64x2.ge", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_GE),
        f64x2_gt("f64x2.gt", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_GT),
        f64x2_le("f64x2.le", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_LE),
        f64x2_lt("f64x2.lt", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_LT),
        f64x2_max("f64x2.max", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_MAX),
        f64x2_min("f64x2.min", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_MIN),
        f64x2_mul("f64x2.mul", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_MUL),
        f64x2_ne("f64x2.ne", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_NE),
        f64x2_nearest("f64x2.nearest", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_NEAREST),
        f64x2_neg("f64x2.neg", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_NEG),
        f64x2_pmax("f64x2.pmax", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_PMAX),
        f64x2_pmin("f64x2.pmin", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_PMIN),
        f64x2_promote_low_f32x4(
                "f64x2.promote_low_f32x4",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_F64X2_PROMOTE_LOW_F32X4),
        f64x2_splat("f64x2.splat", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_SPLAT),
        f64x2_sqrt("f64x2.sqrt", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_SQRT),
        f64x2_sub("f64x2.sub", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_SUB),
        f64x2_trunc("f64x2.trunc", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_F64X2_TRUNC),
        i16x8_abs("i16x8.abs", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_ABS),
        i16x8_add("i16x8.add", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_ADD),
        i16x8_add_sat_s("i16x8.add_sat_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_ADD_SAT_S),
        i16x8_add_sat_u("i16x8.add_sat_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_ADD_SAT_U),
        i16x8_all_true("i16x8.all_true", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_ALL_TRUE),
        i16x8_avgr_u("i16x8.avgr_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_AVGR_U),
        i16x8_bitmask("i16x8.bitmask", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_BITMASK),
        i16x8_eq("i16x8.eq", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_EQ),
        i16x8_extadd_pariwise_i8x16_s(
                "i16x8.extadd_pariwise_i8x16_s",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I16X8_EXTADD_PARIWISE_I8X16_S),
        i16x8_extadd_pariwise_i8x16_u(
                "i16x8.extadd_pariwise_i8x16_u",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I16X8_EXTADD_PARIWISE_I8X16_U),
        i16x8_extend_high_i8x16_s(
                "i16x8.extend_high_i8x16_s",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I16X8_EXTEND_HIGH_I8X16_S),
        i16x8_extend_high_i8x16_u(
                "i16x8.extend_high_i8x16_u",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I16X8_EXTEND_HIGH_I8X16_U),
        i16x8_extend_low_i8x16_s(
                "i16x8.extend_low_i8x16_s",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I16X8_EXTEND_LOW_I8X16_S),
        i16x8_extend_low_i8x16_u(
                "i16x8.extend_low_i8x16_u",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I16X8_EXTEND_LOW_I8X16_U),
        i16x8_extmul_high_i8x16_s(
                "i16x8.extmul_high_i8x16_s",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I16X8_EXTMUL_HIGH_I8X16_S),
        i16x8_extmul_high_i8x16_u(
                "i16x8.extmul_high_i8x16_u",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I16X8_EXTMUL_HIGH_I8X16_U),
        i16x8_extmul_low_i8x16_s(
                "i16x8.extmul_low_i8x16_s",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I16X8_EXTMUL_LOW_I8X16_S),
        i16x8_extmul_low_i8x16_u(
                "i16x8.extmul_low_i8x16_u",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I16X8_EXTMUL_LOW_I8X16_U),
        i16x8_ge_s("i16x8.ge_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_GE_S),
        i16x8_ge_u("i16x8.ge_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_GE_U),
        i16x8_gt_s("i16x8.gt_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_GT_S),
        i16x8_gt_u("i16x8.gt_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_GT_U),
        i16x8_le_s("i16x8.le_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_LE_S),
        i16x8_le_u("i16x8.le_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_LE_U),
        i16x8_lt_s("i16x8.lt_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_LT_S),
        i16x8_lt_u("i16x8.lt_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_LT_U),
        i16x8_max_s("i16x8.max_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_MAX_S),
        i16x8_max_u("i16x8.max_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_MAX_U),
        i16x8_min_s("i16x8.min_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_MIN_S),
        i16x8_min_u("i16x8.min_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_MIN_U),
        i16x8_mul("i16x8.mul", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_MUL),
        i16x8_narrow_i32x4_s(
                "i16x8.narrow_i32x4_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_NARROW_I32X4_S),
        i16x8_narrow_i32x4_u(
                "i16x8.narrow_i32x4_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_NARROW_I32X4_U),
        i16x8_ne("i16x8.ne", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_NE),
        i16x8_neg("i16x8.neg", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_NEG),
        i16x8_q15mulr_sat_s(
                "i16x8.q15mulr_sat_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_Q15MULR_SAT_S),
        i16x8_shl("i16x8.shl", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_SHL),
        i16x8_shr_s("i16x8.shr_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_SHR_S),
        i16x8_shr_u("i16x8.shr_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_SHR_U),
        i16x8_splat("i16x8.splat", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_SPLAT),
        i16x8_sub("i16x8.sub", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_SUB),
        i16x8_sub_sat_s("i16x8.sub_sat_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_SUB_SAT_S),
        i16x8_sub_sat_u("i16x8.sub_sat_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I16X8_SUB_SAT_U),
        i32_add("i32.add", Opcodes.OP_I32_ADD),
        i32_and("i32.and", Opcodes.OP_I32_AND),
        i32_clz("i32.clz", Opcodes.OP_I32_CLZ),
        i32_ctz("i32.ctz", Opcodes.OP_I32_CTZ),
        i32_div_s("i32.div_s", Opcodes.OP_I32_DIV_S),
        i32_div_u("i32.div_u", Opcodes.OP_I32_DIV_U),
        i32_eq("i32.eq", Opcodes.OP_I32_EQ),
        i32_eqz("i32.eqz", Opcodes.OP_I32_EQZ),
        i32_extend16_s("i32.extend16_s", Opcodes.OP_I32_EXTEND16_S),
        i32_extend8_s("i32.extend8_s", Opcodes.OP_I32_EXTEND8_S),
        i32_ge_s("i32.ge_s", Opcodes.OP_I32_GE_S),
        i32_ge_u("i32.ge_u", Opcodes.OP_I32_GE_U),
        i32_gt_s("i32.gt_s", Opcodes.OP_I32_GT_S),
        i32_gt_u("i32.gt_u", Opcodes.OP_I32_GT_U),
        i32_le_s("i32.le_s", Opcodes.OP_I32_LE_S),
        i32_le_u("i32.le_u", Opcodes.OP_I32_LE_U),
        i32_lt_s("i32.lt_s", Opcodes.OP_I32_LT_S),
        i32_lt_u("i32.lt_u", Opcodes.OP_I32_LT_U),
        i32_mul("i32.mul", Opcodes.OP_I32_MUL),
        i32_ne("i32.ne", Opcodes.OP_I32_NE),
        i32_or("i32.or", Opcodes.OP_I32_OR),
        i32_popcnt("i32.popcnt", Opcodes.OP_I32_POPCNT),
        i32_reinterpret_f32("i32.reinterpret_f32", Opcodes.OP_I32_REINTERPRET_F32),
        i32_rem_s("i32.rem_s", Opcodes.OP_I32_REM_S),
        i32_rem_u("i32.rem_u", Opcodes.OP_I32_REM_U),
        i32_rotl("i32.rotl", Opcodes.OP_I32_ROTL),
        i32_rotr("i32.rotr", Opcodes.OP_I32_ROTR),
        i32_shl("i32.shl", Opcodes.OP_I32_SHL),
        i32_shr_s("i32.shr_s", Opcodes.OP_I32_SHR_S),
        i32_shr_u("i32.shr_u", Opcodes.OP_I32_SHR_U),
        i32_sub("i32.sub", Opcodes.OP_I32_SUB),
        i32_trunc_f32_s("i32.trunc_f32_s", Opcodes.OP_I32_TRUNC_F32_S),
        i32_trunc_f32_u("i32.trunc_f32_u", Opcodes.OP_I32_TRUNC_F32_U),
        i32_trunc_f64_s("i32.trunc_f64_s", Opcodes.OP_I32_TRUNC_F64_S),
        i32_trunc_f64_u("i32.trunc_f64_u", Opcodes.OP_I32_TRUNC_F64_U),
        i32_trunc_sat_f32_s(
                "i32.trunc_sat_f32_s", Opcodes.OP_PREFIX_FC, Opcodes.OP_FC_I32_TRUNC_SAT_F32_S),
        i32_trunc_sat_f32_u(
                "i32.trunc_sat_f32_u", Opcodes.OP_PREFIX_FC, Opcodes.OP_FC_I32_TRUNC_SAT_F32_U),
        i32_trunc_sat_f64_s(
                "i32.trunc_sat_f64_s", Opcodes.OP_PREFIX_FC, Opcodes.OP_FC_I32_TRUNC_SAT_F64_S),
        i32_trunc_sat_f64_u(
                "i32.trunc_sat_f64_u", Opcodes.OP_PREFIX_FC, Opcodes.OP_FC_I32_TRUNC_SAT_F64_U),
        i32_wrap_i64("i32.wrap_i64", Opcodes.OP_I32_WRAP_I64),
        i32_xor("i32.xor", Opcodes.OP_I32_XOR),
        i32x4_abs("i32x4.abs", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_ABS),
        i32x4_add("i32x4.add", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_ADD),
        i32x4_all_true("i32x4.all_true", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_ALL_TRUE),
        i32x4_bitmask("i32x4.bitmask", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_BITMASK),
        i32x4_dot_i16x8_s(
                "i32x4.dot_i16x8_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_DOT_I16X8_S),
        i32x4_eq("i32x4.eq", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_EQ),
        i32x4_extadd_pariwise_i16x8_s(
                "i32x4.extadd_pariwise_i16x8_s",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I32X4_EXTADD_PARIWISE_I16X8_S),
        i32x4_extadd_pariwise_i16x8_u(
                "i32x4.extadd_pariwise_i16x8_u",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I32X4_EXTADD_PARIWISE_I16X8_U),
        i32x4_extend_high_i16x8_s(
                "i32x4.extend_high_i16x8_s",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I32X4_EXTEND_HIGH_I16X8_S),
        i32x4_extend_high_i16x8_u(
                "i32x4.extend_high_i16x8_u",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I32X4_EXTEND_HIGH_I16X8_U),
        i32x4_extend_low_i16x8_s(
                "i32x4.extend_low_i16x8_s",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I32X4_EXTEND_LOW_I16X8_S),
        i32x4_extend_low_i16x8_u(
                "i32x4.extend_low_i16x8_u",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I32X4_EXTEND_LOW_I16X8_U),
        i32x4_extmul_high_i16x8_s(
                "i32x4.extmul_high_i16x8_s",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I32X4_EXTMUL_HIGH_I16X8_S),
        i32x4_extmul_high_i16x8_u(
                "i32x4.extmul_high_i16x8_u",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I32X4_EXTMUL_HIGH_I16X8_U),
        i32x4_extmul_low_i16x8_s(
                "i32x4.extmul_low_i16x8_s",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I32X4_EXTMUL_LOW_I16X8_S),
        i32x4_extmul_low_i16x8_u(
                "i32x4.extmul_low_i16x8_u",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I32X4_EXTMUL_LOW_I16X8_U),
        i32x4_ge_s("i32x4.ge_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_GE_S),
        i32x4_ge_u("i32x4.ge_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_GE_U),
        i32x4_gt_s("i32x4.gt_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_GT_S),
        i32x4_gt_u("i32x4.gt_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_GT_U),
        i32x4_le_s("i32x4.le_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_LE_S),
        i32x4_le_u("i32x4.le_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_LE_U),
        i32x4_lt_s("i32x4.lt_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_LT_S),
        i32x4_lt_u("i32x4.lt_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_LT_U),
        i32x4_max_s("i32x4.max_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_MAX_S),
        i32x4_max_u("i32x4.max_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_MAX_U),
        i32x4_min_s("i32x4.min_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_MIN_S),
        i32x4_min_u("i32x4.min_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_MIN_U),
        i32x4_mul("i32x4.mul", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_MUL),
        i32x4_ne("i32x4.ne", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_NE),
        i32x4_neg("i32x4.neg", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_NEG),
        i32x4_shl("i32x4.shl", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_SHL),
        i32x4_shr_s("i32x4.shr_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_SHR_S),
        i32x4_shr_u("i32x4.shr_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_SHR_U),
        i32x4_splat("i32x4.splat", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_SPLAT),
        i32x4_sub("i32x4.sub", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I32X4_SUB),
        i32x4_trunc_sat_f32x4_s(
                "i32x4.trunc_sat_f32x4_s",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I32X4_TRUNC_SAT_F32X4_S),
        i32x4_trunc_sat_f32x4_u(
                "i32x4.trunc_sat_f32x4_u",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I32X4_TRUNC_SAT_F32X4_U),
        i32x4_trunc_sat_f64x2_s_zero(
                "i32x4.trunc_sat_f64x2_s_zero",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I32X4_TRUNC_SAT_F64X2_S_ZERO),
        i32x4_trunc_sat_f64x2_u_zero(
                "i32x4.trunc_sat_f64x2_u_zero",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I32X4_TRUNC_SAT_F64X2_U_ZERO),
        i64_add("i64.add", Opcodes.OP_I64_ADD),
        i64_and("i64.and", Opcodes.OP_I64_AND),
        i64_clz("i64.clz", Opcodes.OP_I64_CLZ),
        i64_ctz("i64.ctz", Opcodes.OP_I64_CTZ),
        i64_div_s("i64.div_s", Opcodes.OP_I64_DIV_S),
        i64_div_u("i64.div_u", Opcodes.OP_I64_DIV_U),
        i64_eq("i64.eq", Opcodes.OP_I64_EQ),
        i64_eqz("i64.eqz", Opcodes.OP_I64_EQZ),
        i64_extend16_s("i64.extend16_s", Opcodes.OP_I64_EXTEND16_S),
        i64_extend32_s("i64.extend32_s", Opcodes.OP_I64_EXTEND32_S),
        i64_extend8_s("i64.extend8_s", Opcodes.OP_I64_EXTEND8_S),
        i64_extend_i32_s("i64.extend_i32_s", Opcodes.OP_I64_EXTEND_I32_S),
        i64_extend_i32_u("i64.extend_i32_u", Opcodes.OP_I64_EXTEND_I32_U),
        i64_ge_s("i64.ge_s", Opcodes.OP_I64_GE_S),
        i64_ge_u("i64.ge_u", Opcodes.OP_I64_GE_U),
        i64_gt_s("i64.gt_s", Opcodes.OP_I64_GT_S),
        i64_gt_u("i64.gt_u", Opcodes.OP_I64_GT_U),
        i64_le_s("i64.le_s", Opcodes.OP_I64_LE_S),
        i64_le_u("i64.le_u", Opcodes.OP_I64_LE_U),
        i64_lt_s("i64.lt_s", Opcodes.OP_I64_LT_S),
        i64_lt_u("i64.lt_u", Opcodes.OP_I64_LT_U),
        i64_mul("i64.mul", Opcodes.OP_I64_MUL),
        i64_ne("i64.ne", Opcodes.OP_I64_NE),
        i64_or("i64.or", Opcodes.OP_I64_OR),
        i64_popcnt("i64.popcnt", Opcodes.OP_I64_POPCNT),
        i64_reinterpret_f64("i64.reinterpret_f64", Opcodes.OP_I64_REINTERPRET_F64),
        i64_rem_s("i64.rem_s", Opcodes.OP_I64_REM_S),
        i64_rem_u("i64.rem_u", Opcodes.OP_I64_REM_U),
        i64_rotl("i64.rotl", Opcodes.OP_I64_ROTL),
        i64_rotr("i64.rotr", Opcodes.OP_I64_ROTR),
        i64_shl("i64.shl", Opcodes.OP_I64_SHL),
        i64_shr_s("i64.shr_s", Opcodes.OP_I64_SHR_S),
        i64_shr_u("i64.shr_u", Opcodes.OP_I64_SHR_U),
        i64_sub("i64.sub", Opcodes.OP_I64_SUB),
        i64_trunc_f32_s("i64.trunc_f32_s", Opcodes.OP_I64_TRUNC_F32_S),
        i64_trunc_f32_u("i64.trunc_f32_u", Opcodes.OP_I64_TRUNC_F32_U),
        i64_trunc_f64_s("i64.trunc_f64_s", Opcodes.OP_I64_TRUNC_F64_S),
        i64_trunc_f64_u("i64.trunc_f64_u", Opcodes.OP_I64_TRUNC_F64_U),
        i64_trunc_sat_f32_s(
                "i64.trunc_sat_f32_s", Opcodes.OP_PREFIX_FC, Opcodes.OP_FC_I64_TRUNC_SAT_F32_S),
        i64_trunc_sat_f32_u(
                "i64.trunc_sat_f32_u", Opcodes.OP_PREFIX_FC, Opcodes.OP_FC_I64_TRUNC_SAT_F32_U),
        i64_trunc_sat_f64_s(
                "i64.trunc_sat_f64_s", Opcodes.OP_PREFIX_FC, Opcodes.OP_FC_I64_TRUNC_SAT_F64_S),
        i64_trunc_sat_f64_u(
                "i64.trunc_sat_f64_u", Opcodes.OP_PREFIX_FC, Opcodes.OP_FC_I64_TRUNC_SAT_F64_U),
        i64_xor("i64.xor", Opcodes.OP_I64_XOR),
        i64x2_abs("i64x2.abs", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I64X2_ABS),
        i64x2_add("i64x2.add", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I64X2_ADD),
        i64x2_all_true("i64x2.all_true", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I64X2_ALL_TRUE),
        i64x2_bitmask("i64x2.bitmask", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I64X2_BITMASK),
        i64x2_dot_i16x8_s(
                "i64x2.dot_i16x8_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I64X2_DOT_I16X8_S),
        i64x2_extend_high_i32x4_s(
                "i64x2.extend_high_i32x4_s",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I64X2_EXTEND_HIGH_I32X4_S),
        i64x2_extend_high_i32x4_u(
                "i64x2.extend_high_i32x4_u",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I64X2_EXTEND_HIGH_I32X4_U),
        i64x2_extend_low_i32x4_s(
                "i64x2.extend_low_i32x4_s",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I64X2_EXTEND_LOW_I32X4_S),
        i64x2_extend_low_i32x4_u(
                "i64x2.extend_low_i32x4_u",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I64X2_EXTEND_LOW_I32X4_U),
        i64x2_extmul_high_i32x4_s(
                "i64x2.extmul_high_i32x4_s",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I64X2_EXTMUL_HIGH_I32X4_S),
        i64x2_extmul_high_i32x4_u(
                "i64x2.extmul_high_i32x4_u",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I64X2_EXTMUL_HIGH_I32X4_U),
        i64x2_extmul_low_i32x4_s(
                "i64x2.extmul_low_i32x4_s",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I64X2_EXTMUL_LOW_I32X4_S),
        i64x2_extmul_low_i32x4_u(
                "i64x2.extmul_low_i32x4_u",
                Opcodes.OP_PREFIX_FD,
                Opcodes.OP_FD_I64X2_EXTMUL_LOW_I32X4_U),
        i64x2_max_s("i64x2.max_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I64X2_MAX_S),
        i64x2_max_u("i64x2.max_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I64X2_MAX_U),
        i64x2_min_s("i64x2.min_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I64X2_MIN_S),
        i64x2_min_u("i64x2.min_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I64X2_MIN_U),
        i64x2_mul("i64x2.mul", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I64X2_MUL),
        i64x2_neg("i64x2.neg", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I64X2_NEG),
        i64x2_shl("i64x2.shl", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I64X2_SHL),
        i64x2_shr_s("i64x2.shr_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I64X2_SHR_S),
        i64x2_shr_u("i64x2.shr_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I64X2_SHR_U),
        i64x2_splat("i64x2.splat", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I64X2_SPLAT),
        i64x2_sub("i64x2.sub", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I64X2_SUB),
        i8x16_abs("i8x16.abs", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_ABS),
        i8x16_add("i8x16.add", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_ADD),
        i8x16_add_sat_s("i8x16.add_sat_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_ADD_SAT_S),
        i8x16_add_sat_u("i8x16.add_sat_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_ADD_SAT_U),
        i8x16_all_true("i8x16.all_true", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_ALL_TRUE),
        i8x16_avgr_u("i8x16.avgr_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_AVGR_U),
        i8x16_bitmask("i8x16.bitmask", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_BITMASK),
        i8x16_eq("i8x16.eq", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_EQ),
        i8x16_ge_s("i8x16.ge_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_GE_S),
        i8x16_ge_u("i8x16.ge_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_GE_U),
        i8x16_gt_s("i8x16.gt_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_GT_S),
        i8x16_gt_u("i8x16.gt_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_GT_U),
        i8x16_le_s("i8x16.le_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_LE_S),
        i8x16_le_u("i8x16.le_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_LE_U),
        i8x16_lt_s("i8x16.lt_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_LT_S),
        i8x16_lt_u("i8x16.lt_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_LT_U),
        i8x16_max_s("i8x16.max_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_MAX_S),
        i8x16_max_u("i8x16.max_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_MAX_U),
        i8x16_min_s("i8x16.min_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_MIN_S),
        i8x16_min_u("i8x16.min_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_MIN_U),
        i8x16_narrow_i16x8_s(
                "i8x16.narrow_i16x8_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_NARROW_I16X8_S),
        i8x16_narrow_i16x8_u(
                "i8x16.narrow_i16x8_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_NARROW_I16X8_U),
        i8x16_ne("i8x16.ne", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_NE),
        i8x16_neg("i8x16.neg", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_NEG),
        i8x16_popcnt("i8x16.popcnt", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_POPCNT),
        i8x16_shl("i8x16.shl", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_SHL),
        i8x16_shr_s("i8x16.shr_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_SHR_S),
        i8x16_shr_u("i8x16.shr_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_SHR_U),
        i8x16_splat("i8x16.splat", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_SPLAT),
        i8x16_sub("i8x16.sub", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_SUB),
        i8x16_sub_sat_s("i8x16.sub_sat_s", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_SUB_SAT_S),
        i8x16_sub_sat_u("i8x16.sub_sat_u", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_SUB_SAT_U),
        i8x16_swizzle("i8x16.swizzle", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_I8X16_SWIZZLE),
        nop("nop", Opcodes.OP_NOP),
        ref_is_null("ref.is_null", Opcodes.OP_REF_IS_NULL),
        return_("return", Opcodes.OP_RETURN),
        select("select", Opcodes.OP_SELECT_EMPTY),
        unreachable("unreachable", Opcodes.OP_UNREACHABLE),
        v128_and("v128.and", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_AND),
        v128_andnot("v128.andnot", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_ANDNOT),
        v128_any_true("v128.any_true", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_ANY_TRUE),
        v128_bitselect("v128.bitselect", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_BITSELECT),
        v128_not("v128.not", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_NOT),
        v128_or("v128.or", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_OR),
        v128_xor("v128.xor", Opcodes.OP_PREFIX_FD, Opcodes.OP_FD_V128_XOR),
        // atomic
        atomic_fence("atomic.fence", Opcodes.OP_PREFIX_FE, Opcodes.OP_FE_ATOMIC_FENCE),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        Simple(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        Simple(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.SIMPLE;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this);
        }

        @Override
        public void skip(WasmInputStream in) {
            // no arguments
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Table implements Op {
        // table index
        table_fill("table.fill", Opcodes.OP_PREFIX_FC, Opcodes.OP_FC_TABLE_FILL),
        table_get("table.get", Opcodes.OP_TABLE_GET),
        table_grow("table.grow", Opcodes.OP_PREFIX_FC, Opcodes.OP_FC_TABLE_GROW),
        table_set("table.set", Opcodes.OP_TABLE_SET),
        table_size("table.size", Opcodes.OP_PREFIX_FC, Opcodes.OP_FC_TABLE_SIZE),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        Table(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        Table(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.TABLE;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this, in.u31());
        }

        @Override
        public void skip(WasmInputStream in) {
            in.u31();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum TableAndFuncType implements Op {
        // tableidx, typeidx
        call_indirect("call_indirect", Opcodes.OP_CALL_INDIRECT),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        TableAndFuncType(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        TableAndFuncType(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.TABLE_AND_FUNC_TYPE;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            int typeId = in.u31();
            int tableIdx = in.u31();
            seq.add(this, tableIdx, typeId);
        }

        @Override
        public void skip(WasmInputStream in) {
            in.u31();
            in.u31();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum TableToTable implements Op {
        // tableidx, tableidx
        table_copy("table.copy", Opcodes.OP_PREFIX_FC, Opcodes.OP_FC_TABLE_COPY),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        TableToTable(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        TableToTable(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.TABLE_AND_TABLE;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this, in.u31(), in.u31());
        }

        @Override
        public void skip(WasmInputStream in) {
            in.u31();
            in.u31();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Tag implements Op {
        // tagidx
        catch_("catch", Opcodes.OP_CATCH),
        throw_("throw", Opcodes.OP_THROW),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        Tag(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        Tag(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.TAG;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            seq.add(this, in.u31());
        }

        @Override
        public void skip(WasmInputStream in) {
            in.u31();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Instructions which take zero or more types as arguments.
     * For code generation, use the constants in {@link Ops} instead.
     */
    enum Types implements Op {
        select("select", Opcodes.OP_SELECT),
        ;

        private final String name;
        private final int opcode;
        private final int secondaryOpcode;

        Types(String name, int opcode, int secondaryOpcode) {
            this.name = name;
            this.opcode = opcode;
            this.secondaryOpcode = secondaryOpcode;
        }

        Types(String name, int opcode) {
            this(name, opcode, -1);
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public int secondaryOpcode() {
            return secondaryOpcode;
        }

        @Override
        public Kind kind() {
            return Kind.TYPES;
        }

        @Override
        public void readFrom(WasmInputStream in, InsnSeq seq) {
            int cnt = in.u31();
            ValueType[] types = new ValueType[cnt];
            for (int i = 0; i < cnt; i++) {
                types[i] = in.type();
            }
            seq.add(this, List.of(types));
        }

        @Override
        public void skip(WasmInputStream in) {
            int cnt = in.u31();
            for (int i = 0; i < cnt; i++) {
                in.type();
            }
        }

        @Override
        public String toString() {
            return name;
        }

        public Simple asSimple() {
            return Simple.select;
        }
    }
}
