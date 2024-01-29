package com.dylibso.chicory.wasm.op;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * The complete set of operations, organized syntactically for simplified code generation.
 */
@SuppressWarnings("SpellCheckingInspection")
public final class Ops {

    private Ops() {}

    // unqualified instructions

    public static final Op.Simple catch_all = Op.Simple.catch_all;
    public static final Op.Simple drop = Op.Simple.drop;
    public static final Op.Simple else_ = Op.Simple.else_;
    public static final Op.Simple end = Op.Simple.end;
    public static final Op.Simple nop = Op.Simple.nop;
    public static final Op.Simple return_ = Op.Simple.return_;
    public static final Op.Simple unreachable = Op.Simple.unreachable;
    public static final Op.Block block = Op.Block.block;
    public static final Op.Block loop = Op.Block.loop;
    public static final Op.Block if_ = Op.Block.if_;
    public static final Op.Block try_ = Op.Block.try_;
    public static final Op.Types select_typed = Op.Types.select;
    public static final Op.Simple select = Op.Simple.select;
    public static final Op.Branch br = Op.Branch.br;
    public static final Op.Branch br_if = Op.Branch.br_if;
    public static final Op.Func call = Op.Func.call;
    public static final Op.MultiBranch br_table = Op.MultiBranch.br_table;
    public static final Op.TableAndFuncType call_indirect = Op.TableAndFuncType.call_indirect;
    public static final Op.Tag catch_ = Op.Tag.catch_;
    public static final Op.Tag throw_ = Op.Tag.throw_;
    public static final Op.Exception delegate = Op.Exception.delegate;
    public static final Op.Exception rethrow = Op.Exception.rethrow;

    // qualified instructions

    public static final class atomic {
        private atomic() {}

        public static final Op.Simple fence = Op.Simple.atomic_fence;
    }

    public static final class data {
        private data() {}

        public static final Op.Data drop = Op.Data.data_drop;
    }

    public static final class elem {
        private elem() {}

        public static final Op.Element drop = Op.Element.elem_drop;
    }

    public static final class local {
        private local() {}

        public static final Op.Local get = Op.Local.local_get;
        public static final Op.Local set = Op.Local.local_set;
        public static final Op.Local tee = Op.Local.local_tee;
    }

    public static final class global {
        private global() {}

        public static final Op.Global get = Op.Global.global_get;
        public static final Op.Global set = Op.Global.global_set;
    }

    public static final class table {
        private table() {}

        public static final Op.Table fill = Op.Table.table_fill;
        public static final Op.Table get = Op.Table.table_get;
        public static final Op.Table grow = Op.Table.table_grow;
        public static final Op.Table set = Op.Table.table_set;
        public static final Op.Table size = Op.Table.table_size;

        public static final Op.TableToTable copy = Op.TableToTable.table_copy;

        public static final Op.ElementAndTable init = Op.ElementAndTable.table_init;
    }

    public static final class memory {
        private memory() {}

        public static final Op.Memory fill = Op.Memory.memory_fill;
        public static final Op.Memory grow = Op.Memory.memory_grow;
        public static final Op.Memory size = Op.Memory.memory_size;

        public static final Op.MemoryToMemory copy = Op.MemoryToMemory.memory_copy;

        public static final Op.MemoryAndData init = Op.MemoryAndData.memory_init;

        public static final class atomic {
            private atomic() {}

            public static final Op.AtomicMemoryAccess notify =
                    Op.AtomicMemoryAccess.memory_atomic_notify;
            public static final Op.AtomicMemoryAccess wait32 =
                    Op.AtomicMemoryAccess.memory_atomic_wait32;
            public static final Op.AtomicMemoryAccess wait64 =
                    Op.AtomicMemoryAccess.memory_atomic_wait64;
        }
    }

    public static final class f32 {
        private f32() {}

        public static final Op.Simple abs = Op.Simple.f32_abs;
        public static final Op.Simple add = Op.Simple.f32_add;
        public static final Op.Simple ceil = Op.Simple.f32_ceil;
        public static final Op.Simple convert_i32_s = Op.Simple.f32_convert_i32_s;
        public static final Op.Simple convert_i32_u = Op.Simple.f32_convert_i32_u;
        public static final Op.Simple convert_i64_s = Op.Simple.f32_convert_i64_s;
        public static final Op.Simple convert_i64_u = Op.Simple.f32_convert_i64_u;
        public static final Op.Simple copysign = Op.Simple.f32_copysign;
        public static final Op.Simple demote_f64 = Op.Simple.f32_demote_f64;
        public static final Op.Simple div = Op.Simple.f32_div;
        public static final Op.Simple eq = Op.Simple.f32_eq;
        public static final Op.Simple floor = Op.Simple.f32_floor;
        public static final Op.Simple ge = Op.Simple.f32_ge;
        public static final Op.Simple gt = Op.Simple.f32_gt;
        public static final Op.Simple le = Op.Simple.f32_le;
        public static final Op.Simple lt = Op.Simple.f32_lt;
        public static final Op.Simple max = Op.Simple.f32_max;
        public static final Op.Simple min = Op.Simple.f32_min;
        public static final Op.Simple mul = Op.Simple.f32_mul;
        public static final Op.Simple ne = Op.Simple.f32_ne;
        public static final Op.Simple nearest = Op.Simple.f32_nearest;
        public static final Op.Simple neg = Op.Simple.f32_neg;
        public static final Op.Simple reinterpret_i32 = Op.Simple.f32_reinterpret_i32;
        public static final Op.Simple sqrt = Op.Simple.f32_sqrt;
        public static final Op.Simple sub = Op.Simple.f32_sub;
        public static final Op.Simple trunc = Op.Simple.f32_trunc;

        public static final Op.MemoryAccess load = Op.MemoryAccess.f32_load;
        public static final Op.MemoryAccess store = Op.MemoryAccess.f32_store;

        public static final Op.ConstF32 const_ = Op.ConstF32.f32_const;
    }

    public static final class f64 {
        private f64() {}

        public static final Op.Simple abs = Op.Simple.f64_abs;
        public static final Op.Simple add = Op.Simple.f64_add;
        public static final Op.Simple ceil = Op.Simple.f64_ceil;
        public static final Op.Simple convert_i32_s = Op.Simple.f64_convert_i32_s;
        public static final Op.Simple convert_i32_u = Op.Simple.f64_convert_i32_u;
        public static final Op.Simple convert_i64_s = Op.Simple.f64_convert_i64_s;
        public static final Op.Simple convert_i64_u = Op.Simple.f64_convert_i64_u;
        public static final Op.Simple copysign = Op.Simple.f64_copysign;
        public static final Op.Simple div = Op.Simple.f64_div;
        public static final Op.Simple eq = Op.Simple.f64_eq;
        public static final Op.Simple floor = Op.Simple.f64_floor;
        public static final Op.Simple ge = Op.Simple.f64_ge;
        public static final Op.Simple gt = Op.Simple.f64_gt;
        public static final Op.Simple le = Op.Simple.f64_le;
        public static final Op.Simple lt = Op.Simple.f64_lt;
        public static final Op.Simple max = Op.Simple.f64_max;
        public static final Op.Simple min = Op.Simple.f64_min;
        public static final Op.Simple mul = Op.Simple.f64_mul;
        public static final Op.Simple ne = Op.Simple.f64_ne;
        public static final Op.Simple nearest = Op.Simple.f64_nearest;
        public static final Op.Simple neg = Op.Simple.f64_neg;
        public static final Op.Simple promote_f32 = Op.Simple.f64_promote_f32;
        public static final Op.Simple reinterpret_i64 = Op.Simple.f64_reinterpret_i64;
        public static final Op.Simple sqrt = Op.Simple.f64_sqrt;
        public static final Op.Simple sub = Op.Simple.f64_sub;
        public static final Op.Simple trunc = Op.Simple.f64_trunc;

        public static final Op.MemoryAccess load = Op.MemoryAccess.f64_load;
        public static final Op.MemoryAccess store = Op.MemoryAccess.f64_store;

        public static final Op.ConstF64 const_ = Op.ConstF64.f64_const;
    }

    public static final class i32 {
        private i32() {}

        public static final Op.Simple add = Op.Simple.i32_add;
        public static final Op.Simple and = Op.Simple.i32_and;
        public static final Op.Simple clz = Op.Simple.i32_clz;
        public static final Op.Simple ctz = Op.Simple.i32_ctz;
        public static final Op.Simple div_s = Op.Simple.i32_div_s;
        public static final Op.Simple div_u = Op.Simple.i32_div_u;
        public static final Op.Simple eq = Op.Simple.i32_eq;
        public static final Op.Simple eqz = Op.Simple.i32_eqz;
        public static final Op.Simple extend16_s = Op.Simple.i32_extend16_s;
        public static final Op.Simple extend8_s = Op.Simple.i32_extend8_s;
        public static final Op.Simple ge_s = Op.Simple.i32_ge_s;
        public static final Op.Simple ge_u = Op.Simple.i32_ge_u;
        public static final Op.Simple gt_s = Op.Simple.i32_gt_s;
        public static final Op.Simple gt_u = Op.Simple.i32_gt_u;
        public static final Op.Simple le_s = Op.Simple.i32_le_s;
        public static final Op.Simple le_u = Op.Simple.i32_le_u;
        public static final Op.Simple lt_s = Op.Simple.i32_lt_s;
        public static final Op.Simple lt_u = Op.Simple.i32_lt_u;
        public static final Op.Simple mul = Op.Simple.i32_mul;
        public static final Op.Simple ne = Op.Simple.i32_ne;
        public static final Op.Simple or = Op.Simple.i32_or;
        public static final Op.Simple popcnt = Op.Simple.i32_popcnt;
        public static final Op.Simple reinterpret_f32 = Op.Simple.i32_reinterpret_f32;
        public static final Op.Simple rem_s = Op.Simple.i32_rem_s;
        public static final Op.Simple rem_u = Op.Simple.i32_rem_u;
        public static final Op.Simple rotl = Op.Simple.i32_rotl;
        public static final Op.Simple rotr = Op.Simple.i32_rotr;
        public static final Op.Simple shl = Op.Simple.i32_shl;
        public static final Op.Simple shr_s = Op.Simple.i32_shr_s;
        public static final Op.Simple shr_u = Op.Simple.i32_shr_u;
        public static final Op.Simple sub = Op.Simple.i32_sub;
        public static final Op.Simple trunc_f32_s = Op.Simple.i32_trunc_f32_s;
        public static final Op.Simple trunc_f32_u = Op.Simple.i32_trunc_f32_u;
        public static final Op.Simple trunc_f64_s = Op.Simple.i32_trunc_f64_s;
        public static final Op.Simple trunc_f64_u = Op.Simple.i32_trunc_f64_u;
        public static final Op.Simple trunc_sat_f32_s = Op.Simple.i32_trunc_sat_f32_s;
        public static final Op.Simple trunc_sat_f32_u = Op.Simple.i32_trunc_sat_f32_u;
        public static final Op.Simple trunc_sat_f64_s = Op.Simple.i32_trunc_sat_f64_s;
        public static final Op.Simple trunc_sat_f64_u = Op.Simple.i32_trunc_sat_f64_u;
        public static final Op.Simple wrap_i64 = Op.Simple.i32_wrap_i64;
        public static final Op.Simple xor = Op.Simple.i32_xor;

        public static final Op.MemoryAccess load = Op.MemoryAccess.i32_load;
        public static final Op.MemoryAccess load16_s = Op.MemoryAccess.i32_load16_s;
        public static final Op.MemoryAccess load16_u = Op.MemoryAccess.i32_load16_u;
        public static final Op.MemoryAccess load8_s = Op.MemoryAccess.i32_load8_s;
        public static final Op.MemoryAccess load8_u = Op.MemoryAccess.i32_load8_u;
        public static final Op.MemoryAccess store = Op.MemoryAccess.i32_store;
        public static final Op.MemoryAccess store16 = Op.MemoryAccess.i32_store16;
        public static final Op.MemoryAccess store8 = Op.MemoryAccess.i32_store8;

        public static final Op.ConstI32 const_ = Op.ConstI32.i32_const;

        public static final class atomic {
            private atomic() {}

            public static final Op.AtomicMemoryAccess load = Op.AtomicMemoryAccess.i32_atomic_load;
            public static final Op.AtomicMemoryAccess load16_u =
                    Op.AtomicMemoryAccess.i32_atomic_load16_u;
            public static final Op.AtomicMemoryAccess load8_u =
                    Op.AtomicMemoryAccess.i32_atomic_load8_u;
            public static final Op.AtomicMemoryAccess store =
                    Op.AtomicMemoryAccess.i32_atomic_store;
            public static final Op.AtomicMemoryAccess store16 =
                    Op.AtomicMemoryAccess.i32_atomic_store16;
            public static final Op.AtomicMemoryAccess store8 =
                    Op.AtomicMemoryAccess.i32_atomic_store8;

            public static final class rmw {
                private rmw() {}

                public static final Op.AtomicMemoryAccess add =
                        Op.AtomicMemoryAccess.i32_atomic_rmw_add;
                public static final Op.AtomicMemoryAccess and =
                        Op.AtomicMemoryAccess.i32_atomic_rmw_and;
                public static final Op.AtomicMemoryAccess cmpxchg =
                        Op.AtomicMemoryAccess.i32_atomic_rmw_cmpxchg;
                public static final Op.AtomicMemoryAccess or =
                        Op.AtomicMemoryAccess.i32_atomic_rmw_or;
                public static final Op.AtomicMemoryAccess sub =
                        Op.AtomicMemoryAccess.i32_atomic_rmw_sub;
                public static final Op.AtomicMemoryAccess xchg =
                        Op.AtomicMemoryAccess.i32_atomic_rmw_xchg;
                public static final Op.AtomicMemoryAccess xor =
                        Op.AtomicMemoryAccess.i32_atomic_rmw_xor;
            }

            public static final class rmw8 {
                private rmw8() {}

                public static final Op.AtomicMemoryAccess add_u =
                        Op.AtomicMemoryAccess.i32_atomic_rmw8_add_u;
                public static final Op.AtomicMemoryAccess and_u =
                        Op.AtomicMemoryAccess.i32_atomic_rmw8_and_u;
                public static final Op.AtomicMemoryAccess cmpxchg_u =
                        Op.AtomicMemoryAccess.i32_atomic_rmw8_cmpxchg_u;
                public static final Op.AtomicMemoryAccess or_u =
                        Op.AtomicMemoryAccess.i32_atomic_rmw8_or_u;
                public static final Op.AtomicMemoryAccess sub_u =
                        Op.AtomicMemoryAccess.i32_atomic_rmw8_sub_u;
                public static final Op.AtomicMemoryAccess xchg_u =
                        Op.AtomicMemoryAccess.i32_atomic_rmw8_xchg_u;
                public static final Op.AtomicMemoryAccess xor_u =
                        Op.AtomicMemoryAccess.i32_atomic_rmw8_xor_u;
            }

            public static final class rmw16 {
                private rmw16() {}

                public static final Op.AtomicMemoryAccess add_u =
                        Op.AtomicMemoryAccess.i32_atomic_rmw16_add_u;
                public static final Op.AtomicMemoryAccess and_u =
                        Op.AtomicMemoryAccess.i32_atomic_rmw16_and_u;
                public static final Op.AtomicMemoryAccess cmpxchg_u =
                        Op.AtomicMemoryAccess.i32_atomic_rmw16_cmpxchg_u;
                public static final Op.AtomicMemoryAccess or_u =
                        Op.AtomicMemoryAccess.i32_atomic_rmw16_or_u;
                public static final Op.AtomicMemoryAccess sub_u =
                        Op.AtomicMemoryAccess.i32_atomic_rmw16_sub_u;
                public static final Op.AtomicMemoryAccess xchg_u =
                        Op.AtomicMemoryAccess.i32_atomic_rmw16_xchg_u;
                public static final Op.AtomicMemoryAccess xor_u =
                        Op.AtomicMemoryAccess.i32_atomic_rmw16_xor_u;
            }
        }
    }

    public static final class i64 {
        private i64() {}

        public static final Op.Simple add = Op.Simple.i64_add;
        public static final Op.Simple and = Op.Simple.i64_and;
        public static final Op.Simple clz = Op.Simple.i64_clz;
        public static final Op.Simple ctz = Op.Simple.i64_ctz;
        public static final Op.Simple div_s = Op.Simple.i64_div_s;
        public static final Op.Simple div_u = Op.Simple.i64_div_u;
        public static final Op.Simple eq = Op.Simple.i64_eq;
        public static final Op.Simple eqz = Op.Simple.i64_eqz;
        public static final Op.Simple extend16_s = Op.Simple.i64_extend16_s;
        public static final Op.Simple extend32_s = Op.Simple.i64_extend32_s;
        public static final Op.Simple extend8_s = Op.Simple.i64_extend8_s;
        public static final Op.Simple extend_i32_s = Op.Simple.i64_extend_i32_s;
        public static final Op.Simple extend_i32_u = Op.Simple.i64_extend_i32_u;
        public static final Op.Simple ge_s = Op.Simple.i64_ge_s;
        public static final Op.Simple ge_u = Op.Simple.i64_ge_u;
        public static final Op.Simple gt_s = Op.Simple.i64_gt_s;
        public static final Op.Simple gt_u = Op.Simple.i64_gt_u;
        public static final Op.Simple le_s = Op.Simple.i64_le_s;
        public static final Op.Simple le_u = Op.Simple.i64_le_u;
        public static final Op.Simple lt_s = Op.Simple.i64_lt_s;
        public static final Op.Simple lt_u = Op.Simple.i64_lt_u;
        public static final Op.Simple mul = Op.Simple.i64_mul;
        public static final Op.Simple ne = Op.Simple.i64_ne;
        public static final Op.Simple or = Op.Simple.i64_or;
        public static final Op.Simple popcnt = Op.Simple.i64_popcnt;
        public static final Op.Simple reinterpret_f64 = Op.Simple.i64_reinterpret_f64;
        public static final Op.Simple rem_s = Op.Simple.i64_rem_s;
        public static final Op.Simple rem_u = Op.Simple.i64_rem_u;
        public static final Op.Simple rotl = Op.Simple.i64_rotl;
        public static final Op.Simple rotr = Op.Simple.i64_rotr;
        public static final Op.Simple shl = Op.Simple.i64_shl;
        public static final Op.Simple shr_s = Op.Simple.i64_shr_s;
        public static final Op.Simple shr_u = Op.Simple.i64_shr_u;
        public static final Op.Simple sub = Op.Simple.i64_sub;
        public static final Op.Simple trunc_f32_s = Op.Simple.i64_trunc_f32_s;
        public static final Op.Simple trunc_f32_u = Op.Simple.i64_trunc_f32_u;
        public static final Op.Simple trunc_f64_s = Op.Simple.i64_trunc_f64_s;
        public static final Op.Simple trunc_f64_u = Op.Simple.i64_trunc_f64_u;
        public static final Op.Simple trunc_sat_f32_s = Op.Simple.i64_trunc_sat_f32_s;
        public static final Op.Simple trunc_sat_f32_u = Op.Simple.i64_trunc_sat_f32_u;
        public static final Op.Simple trunc_sat_f64_s = Op.Simple.i64_trunc_sat_f64_s;
        public static final Op.Simple trunc_sat_f64_u = Op.Simple.i64_trunc_sat_f64_u;
        public static final Op.Simple xor = Op.Simple.i64_xor;

        public static final Op.MemoryAccess load = Op.MemoryAccess.i64_load;
        public static final Op.MemoryAccess load16_s = Op.MemoryAccess.i64_load16_s;
        public static final Op.MemoryAccess load16_u = Op.MemoryAccess.i64_load16_u;
        public static final Op.MemoryAccess load32_s = Op.MemoryAccess.i64_load32_s;
        public static final Op.MemoryAccess load32_u = Op.MemoryAccess.i64_load32_u;
        public static final Op.MemoryAccess load8_s = Op.MemoryAccess.i64_load8_s;
        public static final Op.MemoryAccess load8_u = Op.MemoryAccess.i64_load8_u;
        public static final Op.MemoryAccess store = Op.MemoryAccess.i64_store;
        public static final Op.MemoryAccess store16 = Op.MemoryAccess.i64_store16;
        public static final Op.MemoryAccess store32 = Op.MemoryAccess.i64_store32;
        public static final Op.MemoryAccess store8 = Op.MemoryAccess.i64_store8;

        public static final Op.ConstI64 const_ = Op.ConstI64.i64_const;

        public static final class atomic {
            private atomic() {}

            public static final class rmw {
                private rmw() {}
            }

            public static final class rmw8 {
                private rmw8() {}
            }

            public static final class rmw16 {
                private rmw16() {}
            }

            public static final class rmw32 {
                private rmw32() {}
            }
        }
    }

    public static final class f32x4 {
        private f32x4() {}

        public static final Op.Simple abs = Op.Simple.f32x4_abs;
        public static final Op.Simple add = Op.Simple.f32x4_add;
        public static final Op.Simple ceil = Op.Simple.f32x4_ceil;
        public static final Op.Simple convert_i32x4_s = Op.Simple.f32x4_convert_i32x4_s;
        public static final Op.Simple convert_i32x4_u = Op.Simple.f32x4_convert_i32x4_u;
        public static final Op.Simple demote_f64x2_zero = Op.Simple.f32x4_demote_f64x2_zero;
        public static final Op.Simple div = Op.Simple.f32x4_div;
        public static final Op.Simple eq = Op.Simple.f32x4_eq;
        public static final Op.Simple floor = Op.Simple.f32x4_floor;
        public static final Op.Simple ge = Op.Simple.f32x4_ge;
        public static final Op.Simple gt = Op.Simple.f32x4_gt;
        public static final Op.Simple le = Op.Simple.f32x4_le;
        public static final Op.Simple lt = Op.Simple.f32x4_lt;
        public static final Op.Simple max = Op.Simple.f32x4_max;
        public static final Op.Simple min = Op.Simple.f32x4_min;
        public static final Op.Simple mul = Op.Simple.f32x4_mul;
        public static final Op.Simple ne = Op.Simple.f32x4_ne;
        public static final Op.Simple nearest = Op.Simple.f32x4_nearest;
        public static final Op.Simple neg = Op.Simple.f32x4_neg;
        public static final Op.Simple pmax = Op.Simple.f32x4_pmax;
        public static final Op.Simple pmin = Op.Simple.f32x4_pmin;
        public static final Op.Simple splat = Op.Simple.f32x4_splat;
        public static final Op.Simple sqrt = Op.Simple.f32x4_sqrt;
        public static final Op.Simple sub = Op.Simple.f32x4_sub;
        public static final Op.Simple trunc = Op.Simple.f32x4_trunc;

        public static final Op.Lane extract_lane = Op.Lane.f32x4_extract_lane;
        public static final Op.Lane replace_lane = Op.Lane.f32x4_replace_lane;
    }

    public static final class f64x2 {
        private f64x2() {}

        public static final Op.Simple abs = Op.Simple.f64x2_abs;
        public static final Op.Simple add = Op.Simple.f64x2_add;
        public static final Op.Simple ceil = Op.Simple.f64x2_ceil;
        public static final Op.Simple convert_low_i32x4_s = Op.Simple.f64x2_convert_low_i32x4_s;
        public static final Op.Simple convert_low_i32x4_u = Op.Simple.f64x2_convert_low_i32x4_u;
        public static final Op.Simple div = Op.Simple.f64x2_div;
        public static final Op.Simple eq = Op.Simple.f64x2_eq;
        public static final Op.Simple floor = Op.Simple.f64x2_floor;
        public static final Op.Simple ge = Op.Simple.f64x2_ge;
        public static final Op.Simple gt = Op.Simple.f64x2_gt;
        public static final Op.Simple le = Op.Simple.f64x2_le;
        public static final Op.Simple lt = Op.Simple.f64x2_lt;
        public static final Op.Simple max = Op.Simple.f64x2_max;
        public static final Op.Simple min = Op.Simple.f64x2_min;
        public static final Op.Simple mul = Op.Simple.f64x2_mul;
        public static final Op.Simple ne = Op.Simple.f64x2_ne;
        public static final Op.Simple nearest = Op.Simple.f64x2_nearest;
        public static final Op.Simple neg = Op.Simple.f64x2_neg;
        public static final Op.Simple pmax = Op.Simple.f64x2_pmax;
        public static final Op.Simple pmin = Op.Simple.f64x2_pmin;
        public static final Op.Simple promote_low_f32x4 = Op.Simple.f64x2_promote_low_f32x4;
        public static final Op.Simple splat = Op.Simple.f64x2_splat;
        public static final Op.Simple sqrt = Op.Simple.f64x2_sqrt;
        public static final Op.Simple sub = Op.Simple.f64x2_sub;
        public static final Op.Simple trunc = Op.Simple.f64x2_trunc;

        public static final Op.Lane extract_lane = Op.Lane.f64x2_extract_lane;
        public static final Op.Lane replace_lane = Op.Lane.f64x2_replace_lane;
    }

    public static final class i16x8 {
        private i16x8() {}

        public static final Op.Simple abs = Op.Simple.i16x8_abs;
        public static final Op.Simple add = Op.Simple.i16x8_add;
        public static final Op.Simple add_sat_s = Op.Simple.i16x8_add_sat_s;
        public static final Op.Simple add_sat_u = Op.Simple.i16x8_add_sat_u;
        public static final Op.Simple all_true = Op.Simple.i16x8_all_true;
        public static final Op.Simple avgr_u = Op.Simple.i16x8_avgr_u;
        public static final Op.Simple bitmask = Op.Simple.i16x8_bitmask;
        public static final Op.Simple eq = Op.Simple.i16x8_eq;
        public static final Op.Simple extadd_pariwise_i8x16_s =
                Op.Simple.i16x8_extadd_pariwise_i8x16_s;
        public static final Op.Simple extadd_pariwise_i8x16_u =
                Op.Simple.i16x8_extadd_pariwise_i8x16_u;
        public static final Op.Simple extend_high_i8x16_s = Op.Simple.i16x8_extend_high_i8x16_s;
        public static final Op.Simple extend_high_i8x16_u = Op.Simple.i16x8_extend_high_i8x16_u;
        public static final Op.Simple extend_low_i8x16_s = Op.Simple.i16x8_extend_low_i8x16_s;
        public static final Op.Simple extend_low_i8x16_u = Op.Simple.i16x8_extend_low_i8x16_u;
        public static final Op.Simple extmul_high_i8x16_s = Op.Simple.i16x8_extmul_high_i8x16_s;
        public static final Op.Simple extmul_high_i8x16_u = Op.Simple.i16x8_extmul_high_i8x16_u;
        public static final Op.Simple extmul_low_i8x16_s = Op.Simple.i16x8_extmul_low_i8x16_s;
        public static final Op.Simple extmul_low_i8x16_u = Op.Simple.i16x8_extmul_low_i8x16_u;
        public static final Op.Simple ge_s = Op.Simple.i16x8_ge_s;
        public static final Op.Simple ge_u = Op.Simple.i16x8_ge_u;
        public static final Op.Simple gt_s = Op.Simple.i16x8_gt_s;
        public static final Op.Simple gt_u = Op.Simple.i16x8_gt_u;
        public static final Op.Simple le_s = Op.Simple.i16x8_le_s;
        public static final Op.Simple le_u = Op.Simple.i16x8_le_u;
        public static final Op.Simple lt_s = Op.Simple.i16x8_lt_s;
        public static final Op.Simple lt_u = Op.Simple.i16x8_lt_u;
        public static final Op.Simple max_s = Op.Simple.i16x8_max_s;
        public static final Op.Simple max_u = Op.Simple.i16x8_max_u;
        public static final Op.Simple min_s = Op.Simple.i16x8_min_s;
        public static final Op.Simple min_u = Op.Simple.i16x8_min_u;
        public static final Op.Simple mul = Op.Simple.i16x8_mul;
        public static final Op.Simple narrow_i32x4_s = Op.Simple.i16x8_narrow_i32x4_s;
        public static final Op.Simple narrow_i32x4_u = Op.Simple.i16x8_narrow_i32x4_u;
        public static final Op.Simple ne = Op.Simple.i16x8_ne;
        public static final Op.Simple neg = Op.Simple.i16x8_neg;
        public static final Op.Simple q15mulr_sat_s = Op.Simple.i16x8_q15mulr_sat_s;
        public static final Op.Simple shl = Op.Simple.i16x8_shl;
        public static final Op.Simple shr_s = Op.Simple.i16x8_shr_s;
        public static final Op.Simple shr_u = Op.Simple.i16x8_shr_u;
        public static final Op.Simple splat = Op.Simple.i16x8_splat;
        public static final Op.Simple sub = Op.Simple.i16x8_sub;
        public static final Op.Simple sub_sat_s = Op.Simple.i16x8_sub_sat_s;
        public static final Op.Simple sub_sat_u = Op.Simple.i16x8_sub_sat_u;

        public static final Op.Lane extract_lane_s = Op.Lane.i16x8_extract_lane_s;
        public static final Op.Lane extract_lane_u = Op.Lane.i16x8_extract_lane_u;
        public static final Op.Lane replace_lane = Op.Lane.i16x8_replace_lane;
    }

    public static final class i32x4 {
        private i32x4() {}

        public static final Op.Simple abs = Op.Simple.i32x4_abs;
        public static final Op.Simple add = Op.Simple.i32x4_add;
        public static final Op.Simple all_true = Op.Simple.i32x4_all_true;
        public static final Op.Simple bitmask = Op.Simple.i32x4_bitmask;
        public static final Op.Simple dot_i16x8_s = Op.Simple.i32x4_dot_i16x8_s;
        public static final Op.Simple eq = Op.Simple.i32x4_eq;
        public static final Op.Simple extadd_pariwise_i16x8_s =
                Op.Simple.i32x4_extadd_pariwise_i16x8_s;
        public static final Op.Simple extadd_pariwise_i16x8_u =
                Op.Simple.i32x4_extadd_pariwise_i16x8_u;
        public static final Op.Simple extend_high_i16x8_s = Op.Simple.i32x4_extend_high_i16x8_s;
        public static final Op.Simple extend_high_i16x8_u = Op.Simple.i32x4_extend_high_i16x8_u;
        public static final Op.Simple extend_low_i16x8_s = Op.Simple.i32x4_extend_low_i16x8_s;
        public static final Op.Simple extend_low_i16x8_u = Op.Simple.i32x4_extend_low_i16x8_u;
        public static final Op.Simple extmul_high_i16x8_s = Op.Simple.i32x4_extmul_high_i16x8_s;
        public static final Op.Simple extmul_high_i16x8_u = Op.Simple.i32x4_extmul_high_i16x8_u;
        public static final Op.Simple extmul_low_i16x8_s = Op.Simple.i32x4_extmul_low_i16x8_s;
        public static final Op.Simple extmul_low_i16x8_u = Op.Simple.i32x4_extmul_low_i16x8_u;
        public static final Op.Simple ge_s = Op.Simple.i32x4_ge_s;
        public static final Op.Simple ge_u = Op.Simple.i32x4_ge_u;
        public static final Op.Simple gt_s = Op.Simple.i32x4_gt_s;
        public static final Op.Simple gt_u = Op.Simple.i32x4_gt_u;
        public static final Op.Simple le_s = Op.Simple.i32x4_le_s;
        public static final Op.Simple le_u = Op.Simple.i32x4_le_u;
        public static final Op.Simple lt_s = Op.Simple.i32x4_lt_s;
        public static final Op.Simple lt_u = Op.Simple.i32x4_lt_u;
        public static final Op.Simple max_s = Op.Simple.i32x4_max_s;
        public static final Op.Simple max_u = Op.Simple.i32x4_max_u;
        public static final Op.Simple min_s = Op.Simple.i32x4_min_s;
        public static final Op.Simple min_u = Op.Simple.i32x4_min_u;
        public static final Op.Simple mul = Op.Simple.i32x4_mul;
        public static final Op.Simple ne = Op.Simple.i32x4_ne;
        public static final Op.Simple neg = Op.Simple.i32x4_neg;
        public static final Op.Simple shl = Op.Simple.i32x4_shl;
        public static final Op.Simple shr_s = Op.Simple.i32x4_shr_s;
        public static final Op.Simple shr_u = Op.Simple.i32x4_shr_u;
        public static final Op.Simple splat = Op.Simple.i32x4_splat;
        public static final Op.Simple sub = Op.Simple.i32x4_sub;
        public static final Op.Simple trunc_sat_f32x4_s = Op.Simple.i32x4_trunc_sat_f32x4_s;
        public static final Op.Simple trunc_sat_f32x4_u = Op.Simple.i32x4_trunc_sat_f32x4_u;
        public static final Op.Simple trunc_sat_f64x2_s_zero =
                Op.Simple.i32x4_trunc_sat_f64x2_s_zero;
        public static final Op.Simple trunc_sat_f64x2_u_zero =
                Op.Simple.i32x4_trunc_sat_f64x2_u_zero;

        public static final Op.Lane extract_lane = Op.Lane.i32x4_extract_lane;
        public static final Op.Lane replace_lane = Op.Lane.i32x4_replace_lane;
    }

    public static final class i64x2 {
        private i64x2() {}

        public static final Op.Simple abs = Op.Simple.i64x2_abs;
        public static final Op.Simple add = Op.Simple.i64x2_add;
        public static final Op.Simple all_true = Op.Simple.i64x2_all_true;
        public static final Op.Simple bitmask = Op.Simple.i64x2_bitmask;
        public static final Op.Simple dot_i16x8_s = Op.Simple.i64x2_dot_i16x8_s;
        public static final Op.Simple extend_high_i32x4_s = Op.Simple.i64x2_extend_high_i32x4_s;
        public static final Op.Simple extend_high_i32x4_u = Op.Simple.i64x2_extend_high_i32x4_u;
        public static final Op.Simple extend_low_i32x4_s = Op.Simple.i64x2_extend_low_i32x4_s;
        public static final Op.Simple extend_low_i32x4_u = Op.Simple.i64x2_extend_low_i32x4_u;
        public static final Op.Simple extmul_high_i32x4_s = Op.Simple.i64x2_extmul_high_i32x4_s;
        public static final Op.Simple extmul_high_i32x4_u = Op.Simple.i64x2_extmul_high_i32x4_u;
        public static final Op.Simple extmul_low_i32x4_s = Op.Simple.i64x2_extmul_low_i32x4_s;
        public static final Op.Simple extmul_low_i32x4_u = Op.Simple.i64x2_extmul_low_i32x4_u;
        public static final Op.Simple max_s = Op.Simple.i64x2_max_s;
        public static final Op.Simple max_u = Op.Simple.i64x2_max_u;
        public static final Op.Simple min_s = Op.Simple.i64x2_min_s;
        public static final Op.Simple min_u = Op.Simple.i64x2_min_u;
        public static final Op.Simple mul = Op.Simple.i64x2_mul;
        public static final Op.Simple neg = Op.Simple.i64x2_neg;
        public static final Op.Simple shl = Op.Simple.i64x2_shl;
        public static final Op.Simple shr_s = Op.Simple.i64x2_shr_s;
        public static final Op.Simple shr_u = Op.Simple.i64x2_shr_u;
        public static final Op.Simple splat = Op.Simple.i64x2_splat;
        public static final Op.Simple sub = Op.Simple.i64x2_sub;

        public static final Op.Lane extract_lane = Op.Lane.i64x2_extract_lane;
        public static final Op.Lane replace_lane = Op.Lane.i64x2_replace_lane;
    }

    public static final class i8x16 {
        private i8x16() {}

        public static final Op.Simple abs = Op.Simple.i8x16_abs;
        public static final Op.Simple add = Op.Simple.i8x16_add;
        public static final Op.Simple add_sat_s = Op.Simple.i8x16_add_sat_s;
        public static final Op.Simple add_sat_u = Op.Simple.i8x16_add_sat_u;
        public static final Op.Simple all_true = Op.Simple.i8x16_all_true;
        public static final Op.Simple avgr_u = Op.Simple.i8x16_avgr_u;
        public static final Op.Simple bitmask = Op.Simple.i8x16_bitmask;
        public static final Op.Simple eq = Op.Simple.i8x16_eq;
        public static final Op.Simple ge_s = Op.Simple.i8x16_ge_s;
        public static final Op.Simple ge_u = Op.Simple.i8x16_ge_u;
        public static final Op.Simple gt_s = Op.Simple.i8x16_gt_s;
        public static final Op.Simple gt_u = Op.Simple.i8x16_gt_u;
        public static final Op.Simple le_s = Op.Simple.i8x16_le_s;
        public static final Op.Simple le_u = Op.Simple.i8x16_le_u;
        public static final Op.Simple lt_s = Op.Simple.i8x16_lt_s;
        public static final Op.Simple lt_u = Op.Simple.i8x16_lt_u;
        public static final Op.Simple max_s = Op.Simple.i8x16_max_s;
        public static final Op.Simple max_u = Op.Simple.i8x16_max_u;
        public static final Op.Simple min_s = Op.Simple.i8x16_min_s;
        public static final Op.Simple min_u = Op.Simple.i8x16_min_u;
        public static final Op.Simple narrow_i16x8_s = Op.Simple.i8x16_narrow_i16x8_s;
        public static final Op.Simple narrow_i16x8_u = Op.Simple.i8x16_narrow_i16x8_u;
        public static final Op.Simple ne = Op.Simple.i8x16_ne;
        public static final Op.Simple neg = Op.Simple.i8x16_neg;
        public static final Op.Simple popcnt = Op.Simple.i8x16_popcnt;
        public static final Op.Simple shl = Op.Simple.i8x16_shl;
        public static final Op.Simple shr_s = Op.Simple.i8x16_shr_s;
        public static final Op.Simple shr_u = Op.Simple.i8x16_shr_u;
        public static final Op.Simple splat = Op.Simple.i8x16_splat;
        public static final Op.Simple sub = Op.Simple.i8x16_sub;
        public static final Op.Simple sub_sat_s = Op.Simple.i8x16_sub_sat_s;
        public static final Op.Simple sub_sat_u = Op.Simple.i8x16_sub_sat_u;
        public static final Op.Simple swizzle = Op.Simple.i8x16_swizzle;

        public static final Op.Lane extract_lane_s = Op.Lane.i8x16_extract_lane_s;
        public static final Op.Lane extract_lane_u = Op.Lane.i8x16_extract_lane_u;
        public static final Op.Lane replace_lane = Op.Lane.i8x16_replace_lane;
        public static final Op.Lane shuffle = Op.Lane.i8x16_shuffle;
    }

    public static final class v128 {
        private v128() {}

        public static final Op.Simple and = Op.Simple.v128_and;
        public static final Op.Simple andnot = Op.Simple.v128_andnot;
        public static final Op.Simple any_true = Op.Simple.v128_any_true;
        public static final Op.Simple bitselect = Op.Simple.v128_bitselect;
        public static final Op.Simple not = Op.Simple.v128_not;
        public static final Op.Simple or = Op.Simple.v128_or;
        public static final Op.Simple xor = Op.Simple.v128_xor;

        public static final Op.MemoryAccess load = Op.MemoryAccess.v128_load;
        public static final Op.MemoryAccess load8x8_s = Op.MemoryAccess.v128_load8x8_s;
        public static final Op.MemoryAccess load8x8_u = Op.MemoryAccess.v128_load8x8_u;
        public static final Op.MemoryAccess load16x4_s = Op.MemoryAccess.v128_load16x4_s;
        public static final Op.MemoryAccess load16x4_u = Op.MemoryAccess.v128_load16x4_u;
        public static final Op.MemoryAccess load32x2_s = Op.MemoryAccess.v128_load32x2_s;
        public static final Op.MemoryAccess load32x2_u = Op.MemoryAccess.v128_load32x2_u;
        public static final Op.MemoryAccess load8_splat = Op.MemoryAccess.v128_load8_splat;
        public static final Op.MemoryAccess load16_splat = Op.MemoryAccess.v128_load16_splat;
        public static final Op.MemoryAccess load32_splat = Op.MemoryAccess.v128_load32_splat;
        public static final Op.MemoryAccess load64_splat = Op.MemoryAccess.v128_load64_splat;
        public static final Op.MemoryAccess store = Op.MemoryAccess.v128_store;
        public static final Op.MemoryAccess load32_zero = Op.MemoryAccess.v128_load32_zero;
        public static final Op.MemoryAccess load64_zero = Op.MemoryAccess.v128_load64_zero;

        public static final Op.ConstV128 const_ = Op.ConstV128.v128_const;

        public static final Op.MemoryAccessLane load8_lane = Op.MemoryAccessLane.v128_load8_lane;
        public static final Op.MemoryAccessLane load16_lane = Op.MemoryAccessLane.v128_load16_lane;
        public static final Op.MemoryAccessLane load32_lane = Op.MemoryAccessLane.v128_load32_lane;
        public static final Op.MemoryAccessLane load64_lane = Op.MemoryAccessLane.v128_load64_lane;
        public static final Op.MemoryAccessLane store8_lane = Op.MemoryAccessLane.v128_store8_lane;
        public static final Op.MemoryAccessLane store16_lane =
                Op.MemoryAccessLane.v128_store16_lane;
        public static final Op.MemoryAccessLane store32_lane =
                Op.MemoryAccessLane.v128_store32_lane;
        public static final Op.MemoryAccessLane store64_lane =
                Op.MemoryAccessLane.v128_store64_lane;
    }

    public static final class ref {
        private ref() {}

        public static final Op.Func func = Op.Func.ref_func;

        public static final Op.Simple is_null = Op.Simple.ref_is_null;

        public static final Op.RefTyped null_ = Op.RefTyped.ref_null;
    }

    private static final Op[] NO_PREFIX;
    private static final Op[][] BY_PREFIX;

    private static final Map<String, Op> NAME_MAP;

    static {
        Op[][] byPrefix = new Op[3][256];
        Op[] noPrefix = new Op[256];
        Map<String, Op> nameMap = new HashMap<>();

        for (Op.Kind kind : Op.Kind.all()) {
            for (Op instruction : kind.ops()) {
                int op = instruction.opcode();
                int sec = instruction.secondaryOpcode();
                if (sec != -1) {
                    byPrefix[op - Opcodes.OP_PREFIX_FC][sec] = instruction;
                } else {
                    noPrefix[op] = instruction;
                }
                nameMap.put(instruction.toString(), instruction);
            }
        }
        BY_PREFIX = byPrefix;
        NO_PREFIX = noPrefix;
        NAME_MAP = nameMap;
    }

    public static Op forName(String name) {
        Op instruction = NAME_MAP.get(name);
        if (instruction == null) {
            throw new NoSuchElementException("No instruction named " + name);
        }
        return instruction;
    }

    public static Op forOpcode(int opcode) {
        Op op = opcode < 0 || opcode > 255 ? null : NO_PREFIX[opcode];
        if (op != null) {
            return op;
        }
        throw new NoSuchElementException(
                String.format("No such opcode 0x%02x", Integer.valueOf(opcode)));
    }

    public static Op forOpcode(int opcode, int secondaryOpcode) {
        Op[] sub = opcode < 0 || opcode > 255 ? null : BY_PREFIX[opcode];
        if (sub != null) {
            Op op = secondaryOpcode < 0 || secondaryOpcode > 255 ? null : sub[secondaryOpcode];
            if (op != null) {
                return op;
            }
        }
        throw new NoSuchElementException(
                String.format(
                        "No such opcode 0x%02x 0x%02x",
                        Integer.valueOf(opcode), Integer.valueOf(secondaryOpcode)));
    }
}
