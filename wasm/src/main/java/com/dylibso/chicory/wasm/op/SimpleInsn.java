package com.dylibso.chicory.wasm.op;

/**
 * Instructions which do not take arguments. Always singleton.
 */
@SuppressWarnings("SpellCheckingInspection")
public final class SimpleInsn extends Insn<Op.Simple> {

    private SimpleInsn(Op.Simple op) {
        super(op, 0);
    }

    public static final SimpleInsn catch_all = new SimpleInsn(Op.Simple.catch_all);
    public static final SimpleInsn drop = new SimpleInsn(Op.Simple.drop);
    public static final SimpleInsn else_ = new SimpleInsn(Op.Simple.else_);
    public static final SimpleInsn end = new SimpleInsn(Op.Simple.end);
    public static final SimpleInsn f32_abs = new SimpleInsn(Op.Simple.f32_abs);
    public static final SimpleInsn f32_add = new SimpleInsn(Op.Simple.f32_add);
    public static final SimpleInsn f32_ceil = new SimpleInsn(Op.Simple.f32_ceil);
    public static final SimpleInsn f32_convert_i32_s = new SimpleInsn(Op.Simple.f32_convert_i32_s);
    public static final SimpleInsn f32_convert_i32_u = new SimpleInsn(Op.Simple.f32_convert_i32_u);
    public static final SimpleInsn f32_convert_i64_s = new SimpleInsn(Op.Simple.f32_convert_i64_s);
    public static final SimpleInsn f32_convert_i64_u = new SimpleInsn(Op.Simple.f32_convert_i64_u);
    public static final SimpleInsn f32_copysign = new SimpleInsn(Op.Simple.f32_copysign);
    public static final SimpleInsn f32_demote_f64 = new SimpleInsn(Op.Simple.f32_demote_f64);
    public static final SimpleInsn f32_div = new SimpleInsn(Op.Simple.f32_div);
    public static final SimpleInsn f32_eq = new SimpleInsn(Op.Simple.f32_eq);
    public static final SimpleInsn f32_floor = new SimpleInsn(Op.Simple.f32_floor);
    public static final SimpleInsn f32_ge = new SimpleInsn(Op.Simple.f32_ge);
    public static final SimpleInsn f32_gt = new SimpleInsn(Op.Simple.f32_gt);
    public static final SimpleInsn f32_le = new SimpleInsn(Op.Simple.f32_le);
    public static final SimpleInsn f32_lt = new SimpleInsn(Op.Simple.f32_lt);
    public static final SimpleInsn f32_max = new SimpleInsn(Op.Simple.f32_max);
    public static final SimpleInsn f32_min = new SimpleInsn(Op.Simple.f32_min);
    public static final SimpleInsn f32_mul = new SimpleInsn(Op.Simple.f32_mul);
    public static final SimpleInsn f32_ne = new SimpleInsn(Op.Simple.f32_ne);
    public static final SimpleInsn f32_nearest = new SimpleInsn(Op.Simple.f32_nearest);
    public static final SimpleInsn f32_neg = new SimpleInsn(Op.Simple.f32_neg);
    public static final SimpleInsn f32_reinterpret_i32 =
            new SimpleInsn(Op.Simple.f32_reinterpret_i32);
    public static final SimpleInsn f32_sqrt = new SimpleInsn(Op.Simple.f32_sqrt);
    public static final SimpleInsn f32_sub = new SimpleInsn(Op.Simple.f32_sub);
    public static final SimpleInsn f32_trunc = new SimpleInsn(Op.Simple.f32_trunc);
    public static final SimpleInsn f32x4_abs = new SimpleInsn(Op.Simple.f32x4_abs);
    public static final SimpleInsn f32x4_add = new SimpleInsn(Op.Simple.f32x4_add);
    public static final SimpleInsn f32x4_ceil = new SimpleInsn(Op.Simple.f32x4_ceil);
    public static final SimpleInsn f32x4_convert_i32x4_s =
            new SimpleInsn(Op.Simple.f32x4_convert_i32x4_s);
    public static final SimpleInsn f32x4_convert_i32x4_u =
            new SimpleInsn(Op.Simple.f32x4_convert_i32x4_u);
    public static final SimpleInsn f32x4_demote_f64x2_zero =
            new SimpleInsn(Op.Simple.f32x4_demote_f64x2_zero);
    public static final SimpleInsn f32x4_div = new SimpleInsn(Op.Simple.f32x4_div);
    public static final SimpleInsn f32x4_eq = new SimpleInsn(Op.Simple.f32x4_eq);
    public static final SimpleInsn f32x4_floor = new SimpleInsn(Op.Simple.f32x4_floor);
    public static final SimpleInsn f32x4_ge = new SimpleInsn(Op.Simple.f32x4_ge);
    public static final SimpleInsn f32x4_gt = new SimpleInsn(Op.Simple.f32x4_gt);
    public static final SimpleInsn f32x4_le = new SimpleInsn(Op.Simple.f32x4_le);
    public static final SimpleInsn f32x4_lt = new SimpleInsn(Op.Simple.f32x4_lt);
    public static final SimpleInsn f32x4_max = new SimpleInsn(Op.Simple.f32x4_max);
    public static final SimpleInsn f32x4_min = new SimpleInsn(Op.Simple.f32x4_min);
    public static final SimpleInsn f32x4_mul = new SimpleInsn(Op.Simple.f32x4_mul);
    public static final SimpleInsn f32x4_ne = new SimpleInsn(Op.Simple.f32x4_ne);
    public static final SimpleInsn f32x4_nearest = new SimpleInsn(Op.Simple.f32x4_nearest);
    public static final SimpleInsn f32x4_neg = new SimpleInsn(Op.Simple.f32x4_neg);
    public static final SimpleInsn f32x4_pmax = new SimpleInsn(Op.Simple.f32x4_pmax);
    public static final SimpleInsn f32x4_pmin = new SimpleInsn(Op.Simple.f32x4_pmin);
    public static final SimpleInsn f32x4_splat = new SimpleInsn(Op.Simple.f32x4_splat);
    public static final SimpleInsn f32x4_sqrt = new SimpleInsn(Op.Simple.f32x4_sqrt);
    public static final SimpleInsn f32x4_sub = new SimpleInsn(Op.Simple.f32x4_sub);
    public static final SimpleInsn f32x4_trunc = new SimpleInsn(Op.Simple.f32x4_trunc);
    public static final SimpleInsn f64_abs = new SimpleInsn(Op.Simple.f64_abs);
    public static final SimpleInsn f64_add = new SimpleInsn(Op.Simple.f64_add);
    public static final SimpleInsn f64_ceil = new SimpleInsn(Op.Simple.f64_ceil);
    public static final SimpleInsn f64_convert_i32_s = new SimpleInsn(Op.Simple.f64_convert_i32_s);
    public static final SimpleInsn f64_convert_i32_u = new SimpleInsn(Op.Simple.f64_convert_i32_u);
    public static final SimpleInsn f64_convert_i64_s = new SimpleInsn(Op.Simple.f64_convert_i64_s);
    public static final SimpleInsn f64_convert_i64_u = new SimpleInsn(Op.Simple.f64_convert_i64_u);
    public static final SimpleInsn f64_copysign = new SimpleInsn(Op.Simple.f64_copysign);
    public static final SimpleInsn f64_div = new SimpleInsn(Op.Simple.f64_div);
    public static final SimpleInsn f64_eq = new SimpleInsn(Op.Simple.f64_eq);
    public static final SimpleInsn f64_floor = new SimpleInsn(Op.Simple.f64_floor);
    public static final SimpleInsn f64_ge = new SimpleInsn(Op.Simple.f64_ge);
    public static final SimpleInsn f64_gt = new SimpleInsn(Op.Simple.f64_gt);
    public static final SimpleInsn f64_le = new SimpleInsn(Op.Simple.f64_le);
    public static final SimpleInsn f64_lt = new SimpleInsn(Op.Simple.f64_lt);
    public static final SimpleInsn f64_max = new SimpleInsn(Op.Simple.f64_max);
    public static final SimpleInsn f64_min = new SimpleInsn(Op.Simple.f64_min);
    public static final SimpleInsn f64_mul = new SimpleInsn(Op.Simple.f64_mul);
    public static final SimpleInsn f64_ne = new SimpleInsn(Op.Simple.f64_ne);
    public static final SimpleInsn f64_nearest = new SimpleInsn(Op.Simple.f64_nearest);
    public static final SimpleInsn f64_neg = new SimpleInsn(Op.Simple.f64_neg);
    public static final SimpleInsn f64_promote_f32 = new SimpleInsn(Op.Simple.f64_promote_f32);
    public static final SimpleInsn f64_reinterpret_i64 =
            new SimpleInsn(Op.Simple.f64_reinterpret_i64);
    public static final SimpleInsn f64_sqrt = new SimpleInsn(Op.Simple.f64_sqrt);
    public static final SimpleInsn f64_sub = new SimpleInsn(Op.Simple.f64_sub);
    public static final SimpleInsn f64_trunc = new SimpleInsn(Op.Simple.f64_trunc);
    public static final SimpleInsn f64x2_abs = new SimpleInsn(Op.Simple.f64x2_abs);
    public static final SimpleInsn f64x2_add = new SimpleInsn(Op.Simple.f64x2_add);
    public static final SimpleInsn f64x2_ceil = new SimpleInsn(Op.Simple.f64x2_ceil);
    public static final SimpleInsn f64x2_convert_low_i32x4_s =
            new SimpleInsn(Op.Simple.f64x2_convert_low_i32x4_s);
    public static final SimpleInsn f64x2_convert_low_i32x4_u =
            new SimpleInsn(Op.Simple.f64x2_convert_low_i32x4_u);
    public static final SimpleInsn f64x2_div = new SimpleInsn(Op.Simple.f64x2_div);
    public static final SimpleInsn f64x2_eq = new SimpleInsn(Op.Simple.f64x2_eq);
    public static final SimpleInsn f64x2_floor = new SimpleInsn(Op.Simple.f64x2_floor);
    public static final SimpleInsn f64x2_ge = new SimpleInsn(Op.Simple.f64x2_ge);
    public static final SimpleInsn f64x2_gt = new SimpleInsn(Op.Simple.f64x2_gt);
    public static final SimpleInsn f64x2_le = new SimpleInsn(Op.Simple.f64x2_le);
    public static final SimpleInsn f64x2_lt = new SimpleInsn(Op.Simple.f64x2_lt);
    public static final SimpleInsn f64x2_max = new SimpleInsn(Op.Simple.f64x2_max);
    public static final SimpleInsn f64x2_min = new SimpleInsn(Op.Simple.f64x2_min);
    public static final SimpleInsn f64x2_mul = new SimpleInsn(Op.Simple.f64x2_mul);
    public static final SimpleInsn f64x2_ne = new SimpleInsn(Op.Simple.f64x2_ne);
    public static final SimpleInsn f64x2_nearest = new SimpleInsn(Op.Simple.f64x2_nearest);
    public static final SimpleInsn f64x2_neg = new SimpleInsn(Op.Simple.f64x2_neg);
    public static final SimpleInsn f64x2_pmax = new SimpleInsn(Op.Simple.f64x2_pmax);
    public static final SimpleInsn f64x2_pmin = new SimpleInsn(Op.Simple.f64x2_pmin);
    public static final SimpleInsn f64x2_promote_low_f32x4 =
            new SimpleInsn(Op.Simple.f64x2_promote_low_f32x4);
    public static final SimpleInsn f64x2_splat = new SimpleInsn(Op.Simple.f64x2_splat);
    public static final SimpleInsn f64x2_sqrt = new SimpleInsn(Op.Simple.f64x2_sqrt);
    public static final SimpleInsn f64x2_sub = new SimpleInsn(Op.Simple.f64x2_sub);
    public static final SimpleInsn f64x2_trunc = new SimpleInsn(Op.Simple.f64x2_trunc);
    public static final SimpleInsn i16x8_abs = new SimpleInsn(Op.Simple.i16x8_abs);
    public static final SimpleInsn i16x8_add = new SimpleInsn(Op.Simple.i16x8_add);
    public static final SimpleInsn i16x8_add_sat_s = new SimpleInsn(Op.Simple.i16x8_add_sat_s);
    public static final SimpleInsn i16x8_add_sat_u = new SimpleInsn(Op.Simple.i16x8_add_sat_u);
    public static final SimpleInsn i16x8_all_true = new SimpleInsn(Op.Simple.i16x8_all_true);
    public static final SimpleInsn i16x8_avgr_u = new SimpleInsn(Op.Simple.i16x8_avgr_u);
    public static final SimpleInsn i16x8_bitmask = new SimpleInsn(Op.Simple.i16x8_bitmask);
    public static final SimpleInsn i16x8_eq = new SimpleInsn(Op.Simple.i16x8_eq);
    public static final SimpleInsn i16x8_extadd_pariwise_i8x16_s =
            new SimpleInsn(Op.Simple.i16x8_extadd_pariwise_i8x16_s);
    public static final SimpleInsn i16x8_extadd_pariwise_i8x16_u =
            new SimpleInsn(Op.Simple.i16x8_extadd_pariwise_i8x16_u);
    public static final SimpleInsn i16x8_extend_high_i8x16_s =
            new SimpleInsn(Op.Simple.i16x8_extend_high_i8x16_s);
    public static final SimpleInsn i16x8_extend_high_i8x16_u =
            new SimpleInsn(Op.Simple.i16x8_extend_high_i8x16_u);
    public static final SimpleInsn i16x8_extend_low_i8x16_s =
            new SimpleInsn(Op.Simple.i16x8_extend_low_i8x16_s);
    public static final SimpleInsn i16x8_extend_low_i8x16_u =
            new SimpleInsn(Op.Simple.i16x8_extend_low_i8x16_u);
    public static final SimpleInsn i16x8_extmul_high_i8x16_s =
            new SimpleInsn(Op.Simple.i16x8_extmul_high_i8x16_s);
    public static final SimpleInsn i16x8_extmul_high_i8x16_u =
            new SimpleInsn(Op.Simple.i16x8_extmul_high_i8x16_u);
    public static final SimpleInsn i16x8_extmul_low_i8x16_s =
            new SimpleInsn(Op.Simple.i16x8_extmul_low_i8x16_s);
    public static final SimpleInsn i16x8_extmul_low_i8x16_u =
            new SimpleInsn(Op.Simple.i16x8_extmul_low_i8x16_u);
    public static final SimpleInsn i16x8_ge_s = new SimpleInsn(Op.Simple.i16x8_ge_s);
    public static final SimpleInsn i16x8_ge_u = new SimpleInsn(Op.Simple.i16x8_ge_u);
    public static final SimpleInsn i16x8_gt_s = new SimpleInsn(Op.Simple.i16x8_gt_s);
    public static final SimpleInsn i16x8_gt_u = new SimpleInsn(Op.Simple.i16x8_gt_u);
    public static final SimpleInsn i16x8_le_s = new SimpleInsn(Op.Simple.i16x8_le_s);
    public static final SimpleInsn i16x8_le_u = new SimpleInsn(Op.Simple.i16x8_le_u);
    public static final SimpleInsn i16x8_lt_s = new SimpleInsn(Op.Simple.i16x8_lt_s);
    public static final SimpleInsn i16x8_lt_u = new SimpleInsn(Op.Simple.i16x8_lt_u);
    public static final SimpleInsn i16x8_max_s = new SimpleInsn(Op.Simple.i16x8_max_s);
    public static final SimpleInsn i16x8_max_u = new SimpleInsn(Op.Simple.i16x8_max_u);
    public static final SimpleInsn i16x8_min_s = new SimpleInsn(Op.Simple.i16x8_min_s);
    public static final SimpleInsn i16x8_min_u = new SimpleInsn(Op.Simple.i16x8_min_u);
    public static final SimpleInsn i16x8_mul = new SimpleInsn(Op.Simple.i16x8_mul);
    public static final SimpleInsn i16x8_narrow_i32x4_s =
            new SimpleInsn(Op.Simple.i16x8_narrow_i32x4_s);
    public static final SimpleInsn i16x8_narrow_i32x4_u =
            new SimpleInsn(Op.Simple.i16x8_narrow_i32x4_u);
    public static final SimpleInsn i16x8_ne = new SimpleInsn(Op.Simple.i16x8_ne);
    public static final SimpleInsn i16x8_neg = new SimpleInsn(Op.Simple.i16x8_neg);
    public static final SimpleInsn i16x8_q15mulr_sat_s =
            new SimpleInsn(Op.Simple.i16x8_q15mulr_sat_s);
    public static final SimpleInsn i16x8_shl = new SimpleInsn(Op.Simple.i16x8_shl);
    public static final SimpleInsn i16x8_shr_s = new SimpleInsn(Op.Simple.i16x8_shr_s);
    public static final SimpleInsn i16x8_shr_u = new SimpleInsn(Op.Simple.i16x8_shr_u);
    public static final SimpleInsn i16x8_splat = new SimpleInsn(Op.Simple.i16x8_splat);
    public static final SimpleInsn i16x8_sub = new SimpleInsn(Op.Simple.i16x8_sub);
    public static final SimpleInsn i16x8_sub_sat_s = new SimpleInsn(Op.Simple.i16x8_sub_sat_s);
    public static final SimpleInsn i16x8_sub_sat_u = new SimpleInsn(Op.Simple.i16x8_sub_sat_u);
    public static final SimpleInsn i32_add = new SimpleInsn(Op.Simple.i32_add);
    public static final SimpleInsn i32_and = new SimpleInsn(Op.Simple.i32_and);
    public static final SimpleInsn i32_clz = new SimpleInsn(Op.Simple.i32_clz);
    public static final SimpleInsn i32_ctz = new SimpleInsn(Op.Simple.i32_ctz);
    public static final SimpleInsn i32_div_s = new SimpleInsn(Op.Simple.i32_div_s);
    public static final SimpleInsn i32_div_u = new SimpleInsn(Op.Simple.i32_div_u);
    public static final SimpleInsn i32_eq = new SimpleInsn(Op.Simple.i32_eq);
    public static final SimpleInsn i32_eqz = new SimpleInsn(Op.Simple.i32_eqz);
    public static final SimpleInsn i32_extend16_s = new SimpleInsn(Op.Simple.i32_extend16_s);
    public static final SimpleInsn i32_extend8_s = new SimpleInsn(Op.Simple.i32_extend8_s);
    public static final SimpleInsn i32_ge_s = new SimpleInsn(Op.Simple.i32_ge_s);
    public static final SimpleInsn i32_ge_u = new SimpleInsn(Op.Simple.i32_ge_u);
    public static final SimpleInsn i32_gt_s = new SimpleInsn(Op.Simple.i32_gt_s);
    public static final SimpleInsn i32_gt_u = new SimpleInsn(Op.Simple.i32_gt_u);
    public static final SimpleInsn i32_le_s = new SimpleInsn(Op.Simple.i32_le_s);
    public static final SimpleInsn i32_le_u = new SimpleInsn(Op.Simple.i32_le_u);
    public static final SimpleInsn i32_lt_s = new SimpleInsn(Op.Simple.i32_lt_s);
    public static final SimpleInsn i32_lt_u = new SimpleInsn(Op.Simple.i32_lt_u);
    public static final SimpleInsn i32_mul = new SimpleInsn(Op.Simple.i32_mul);
    public static final SimpleInsn i32_ne = new SimpleInsn(Op.Simple.i32_ne);
    public static final SimpleInsn i32_or = new SimpleInsn(Op.Simple.i32_or);
    public static final SimpleInsn i32_popcnt = new SimpleInsn(Op.Simple.i32_popcnt);
    public static final SimpleInsn i32_reinterpret_f32 =
            new SimpleInsn(Op.Simple.i32_reinterpret_f32);
    public static final SimpleInsn i32_rem_s = new SimpleInsn(Op.Simple.i32_rem_s);
    public static final SimpleInsn i32_rem_u = new SimpleInsn(Op.Simple.i32_rem_u);
    public static final SimpleInsn i32_rotl = new SimpleInsn(Op.Simple.i32_rotl);
    public static final SimpleInsn i32_rotr = new SimpleInsn(Op.Simple.i32_rotr);
    public static final SimpleInsn i32_shl = new SimpleInsn(Op.Simple.i32_shl);
    public static final SimpleInsn i32_shr_s = new SimpleInsn(Op.Simple.i32_shr_s);
    public static final SimpleInsn i32_shr_u = new SimpleInsn(Op.Simple.i32_shr_u);
    public static final SimpleInsn i32_sub = new SimpleInsn(Op.Simple.i32_sub);
    public static final SimpleInsn i32_trunc_f32_s = new SimpleInsn(Op.Simple.i32_trunc_f32_s);
    public static final SimpleInsn i32_trunc_f32_u = new SimpleInsn(Op.Simple.i32_trunc_f32_u);
    public static final SimpleInsn i32_trunc_f64_s = new SimpleInsn(Op.Simple.i32_trunc_f64_s);
    public static final SimpleInsn i32_trunc_f64_u = new SimpleInsn(Op.Simple.i32_trunc_f64_u);
    public static final SimpleInsn i32_trunc_sat_f32_s =
            new SimpleInsn(Op.Simple.i32_trunc_sat_f32_s);
    public static final SimpleInsn i32_trunc_sat_f32_u =
            new SimpleInsn(Op.Simple.i32_trunc_sat_f32_u);
    public static final SimpleInsn i32_trunc_sat_f64_s =
            new SimpleInsn(Op.Simple.i32_trunc_sat_f64_s);
    public static final SimpleInsn i32_trunc_sat_f64_u =
            new SimpleInsn(Op.Simple.i32_trunc_sat_f64_u);
    public static final SimpleInsn i32_wrap_i64 = new SimpleInsn(Op.Simple.i32_wrap_i64);
    public static final SimpleInsn i32_xor = new SimpleInsn(Op.Simple.i32_xor);
    public static final SimpleInsn i32x4_abs = new SimpleInsn(Op.Simple.i32x4_abs);
    public static final SimpleInsn i32x4_add = new SimpleInsn(Op.Simple.i32x4_add);
    public static final SimpleInsn i32x4_all_true = new SimpleInsn(Op.Simple.i32x4_all_true);
    public static final SimpleInsn i32x4_bitmask = new SimpleInsn(Op.Simple.i32x4_bitmask);
    public static final SimpleInsn i32x4_dot_i16x8_s = new SimpleInsn(Op.Simple.i32x4_dot_i16x8_s);
    public static final SimpleInsn i32x4_eq = new SimpleInsn(Op.Simple.i32x4_eq);
    public static final SimpleInsn i32x4_extadd_pariwise_i16x8_s =
            new SimpleInsn(Op.Simple.i32x4_extadd_pariwise_i16x8_s);
    public static final SimpleInsn i32x4_extadd_pariwise_i16x8_u =
            new SimpleInsn(Op.Simple.i32x4_extadd_pariwise_i16x8_u);
    public static final SimpleInsn i32x4_extend_high_i16x8_s =
            new SimpleInsn(Op.Simple.i32x4_extend_high_i16x8_s);
    public static final SimpleInsn i32x4_extend_high_i16x8_u =
            new SimpleInsn(Op.Simple.i32x4_extend_high_i16x8_u);
    public static final SimpleInsn i32x4_extend_low_i16x8_s =
            new SimpleInsn(Op.Simple.i32x4_extend_low_i16x8_s);
    public static final SimpleInsn i32x4_extend_low_i16x8_u =
            new SimpleInsn(Op.Simple.i32x4_extend_low_i16x8_u);
    public static final SimpleInsn i32x4_extmul_high_i16x8_s =
            new SimpleInsn(Op.Simple.i32x4_extmul_high_i16x8_s);
    public static final SimpleInsn i32x4_extmul_high_i16x8_u =
            new SimpleInsn(Op.Simple.i32x4_extmul_high_i16x8_u);
    public static final SimpleInsn i32x4_extmul_low_i16x8_s =
            new SimpleInsn(Op.Simple.i32x4_extmul_low_i16x8_s);
    public static final SimpleInsn i32x4_extmul_low_i16x8_u =
            new SimpleInsn(Op.Simple.i32x4_extmul_low_i16x8_u);
    public static final SimpleInsn i32x4_ge_s = new SimpleInsn(Op.Simple.i32x4_ge_s);
    public static final SimpleInsn i32x4_ge_u = new SimpleInsn(Op.Simple.i32x4_ge_u);
    public static final SimpleInsn i32x4_gt_s = new SimpleInsn(Op.Simple.i32x4_gt_s);
    public static final SimpleInsn i32x4_gt_u = new SimpleInsn(Op.Simple.i32x4_gt_u);
    public static final SimpleInsn i32x4_le_s = new SimpleInsn(Op.Simple.i32x4_le_s);
    public static final SimpleInsn i32x4_le_u = new SimpleInsn(Op.Simple.i32x4_le_u);
    public static final SimpleInsn i32x4_lt_s = new SimpleInsn(Op.Simple.i32x4_lt_s);
    public static final SimpleInsn i32x4_lt_u = new SimpleInsn(Op.Simple.i32x4_lt_u);
    public static final SimpleInsn i32x4_max_s = new SimpleInsn(Op.Simple.i32x4_max_s);
    public static final SimpleInsn i32x4_max_u = new SimpleInsn(Op.Simple.i32x4_max_u);
    public static final SimpleInsn i32x4_min_s = new SimpleInsn(Op.Simple.i32x4_min_s);
    public static final SimpleInsn i32x4_min_u = new SimpleInsn(Op.Simple.i32x4_min_u);
    public static final SimpleInsn i32x4_mul = new SimpleInsn(Op.Simple.i32x4_mul);
    public static final SimpleInsn i32x4_ne = new SimpleInsn(Op.Simple.i32x4_ne);
    public static final SimpleInsn i32x4_neg = new SimpleInsn(Op.Simple.i32x4_neg);
    public static final SimpleInsn i32x4_shl = new SimpleInsn(Op.Simple.i32x4_shl);
    public static final SimpleInsn i32x4_shr_s = new SimpleInsn(Op.Simple.i32x4_shr_s);
    public static final SimpleInsn i32x4_shr_u = new SimpleInsn(Op.Simple.i32x4_shr_u);
    public static final SimpleInsn i32x4_splat = new SimpleInsn(Op.Simple.i32x4_splat);
    public static final SimpleInsn i32x4_sub = new SimpleInsn(Op.Simple.i32x4_sub);
    public static final SimpleInsn i32x4_trunc_sat_f32x4_s =
            new SimpleInsn(Op.Simple.i32x4_trunc_sat_f32x4_s);
    public static final SimpleInsn i32x4_trunc_sat_f32x4_u =
            new SimpleInsn(Op.Simple.i32x4_trunc_sat_f32x4_u);
    public static final SimpleInsn i32x4_trunc_sat_f64x2_s_zero =
            new SimpleInsn(Op.Simple.i32x4_trunc_sat_f64x2_s_zero);
    public static final SimpleInsn i32x4_trunc_sat_f64x2_u_zero =
            new SimpleInsn(Op.Simple.i32x4_trunc_sat_f64x2_u_zero);
    public static final SimpleInsn i64_add = new SimpleInsn(Op.Simple.i64_add);
    public static final SimpleInsn i64_and = new SimpleInsn(Op.Simple.i64_and);
    public static final SimpleInsn i64_clz = new SimpleInsn(Op.Simple.i64_clz);
    public static final SimpleInsn i64_ctz = new SimpleInsn(Op.Simple.i64_ctz);
    public static final SimpleInsn i64_div_s = new SimpleInsn(Op.Simple.i64_div_s);
    public static final SimpleInsn i64_div_u = new SimpleInsn(Op.Simple.i64_div_u);
    public static final SimpleInsn i64_eq = new SimpleInsn(Op.Simple.i64_eq);
    public static final SimpleInsn i64_eqz = new SimpleInsn(Op.Simple.i64_eqz);
    public static final SimpleInsn i64_extend16_s = new SimpleInsn(Op.Simple.i64_extend16_s);
    public static final SimpleInsn i64_extend32_s = new SimpleInsn(Op.Simple.i64_extend32_s);
    public static final SimpleInsn i64_extend8_s = new SimpleInsn(Op.Simple.i64_extend8_s);
    public static final SimpleInsn i64_extend_i32_s = new SimpleInsn(Op.Simple.i64_extend_i32_s);
    public static final SimpleInsn i64_extend_i32_u = new SimpleInsn(Op.Simple.i64_extend_i32_u);
    public static final SimpleInsn i64_ge_s = new SimpleInsn(Op.Simple.i64_ge_s);
    public static final SimpleInsn i64_ge_u = new SimpleInsn(Op.Simple.i64_ge_u);
    public static final SimpleInsn i64_gt_s = new SimpleInsn(Op.Simple.i64_gt_s);
    public static final SimpleInsn i64_gt_u = new SimpleInsn(Op.Simple.i64_gt_u);
    public static final SimpleInsn i64_le_s = new SimpleInsn(Op.Simple.i64_le_s);
    public static final SimpleInsn i64_le_u = new SimpleInsn(Op.Simple.i64_le_u);
    public static final SimpleInsn i64_lt_s = new SimpleInsn(Op.Simple.i64_lt_s);
    public static final SimpleInsn i64_lt_u = new SimpleInsn(Op.Simple.i64_lt_u);
    public static final SimpleInsn i64_mul = new SimpleInsn(Op.Simple.i64_mul);
    public static final SimpleInsn i64_ne = new SimpleInsn(Op.Simple.i64_ne);
    public static final SimpleInsn i64_or = new SimpleInsn(Op.Simple.i64_or);
    public static final SimpleInsn i64_popcnt = new SimpleInsn(Op.Simple.i64_popcnt);
    public static final SimpleInsn i64_reinterpret_f64 =
            new SimpleInsn(Op.Simple.i64_reinterpret_f64);
    public static final SimpleInsn i64_rem_s = new SimpleInsn(Op.Simple.i64_rem_s);
    public static final SimpleInsn i64_rem_u = new SimpleInsn(Op.Simple.i64_rem_u);
    public static final SimpleInsn i64_rotl = new SimpleInsn(Op.Simple.i64_rotl);
    public static final SimpleInsn i64_rotr = new SimpleInsn(Op.Simple.i64_rotr);
    public static final SimpleInsn i64_shl = new SimpleInsn(Op.Simple.i64_shl);
    public static final SimpleInsn i64_shr_s = new SimpleInsn(Op.Simple.i64_shr_s);
    public static final SimpleInsn i64_shr_u = new SimpleInsn(Op.Simple.i64_shr_u);
    public static final SimpleInsn i64_sub = new SimpleInsn(Op.Simple.i64_sub);
    public static final SimpleInsn i64_trunc_f32_s = new SimpleInsn(Op.Simple.i64_trunc_f32_s);
    public static final SimpleInsn i64_trunc_f32_u = new SimpleInsn(Op.Simple.i64_trunc_f32_u);
    public static final SimpleInsn i64_trunc_f64_s = new SimpleInsn(Op.Simple.i64_trunc_f64_s);
    public static final SimpleInsn i64_trunc_f64_u = new SimpleInsn(Op.Simple.i64_trunc_f64_u);
    public static final SimpleInsn i64_trunc_sat_f32_s =
            new SimpleInsn(Op.Simple.i64_trunc_sat_f32_s);
    public static final SimpleInsn i64_trunc_sat_f32_u =
            new SimpleInsn(Op.Simple.i64_trunc_sat_f32_u);
    public static final SimpleInsn i64_trunc_sat_f64_s =
            new SimpleInsn(Op.Simple.i64_trunc_sat_f64_s);
    public static final SimpleInsn i64_trunc_sat_f64_u =
            new SimpleInsn(Op.Simple.i64_trunc_sat_f64_u);
    public static final SimpleInsn i64_xor = new SimpleInsn(Op.Simple.i64_xor);
    public static final SimpleInsn i64x2_abs = new SimpleInsn(Op.Simple.i64x2_abs);
    public static final SimpleInsn i64x2_add = new SimpleInsn(Op.Simple.i64x2_add);
    public static final SimpleInsn i64x2_all_true = new SimpleInsn(Op.Simple.i64x2_all_true);
    public static final SimpleInsn i64x2_bitmask = new SimpleInsn(Op.Simple.i64x2_bitmask);
    public static final SimpleInsn i64x2_dot_i16x8_s = new SimpleInsn(Op.Simple.i64x2_dot_i16x8_s);
    public static final SimpleInsn i64x2_extend_high_i32x4_s =
            new SimpleInsn(Op.Simple.i64x2_extend_high_i32x4_s);
    public static final SimpleInsn i64x2_extend_high_i32x4_u =
            new SimpleInsn(Op.Simple.i64x2_extend_high_i32x4_u);
    public static final SimpleInsn i64x2_extend_low_i32x4_s =
            new SimpleInsn(Op.Simple.i64x2_extend_low_i32x4_s);
    public static final SimpleInsn i64x2_extend_low_i32x4_u =
            new SimpleInsn(Op.Simple.i64x2_extend_low_i32x4_u);
    public static final SimpleInsn i64x2_extmul_high_i32x4_s =
            new SimpleInsn(Op.Simple.i64x2_extmul_high_i32x4_s);
    public static final SimpleInsn i64x2_extmul_high_i32x4_u =
            new SimpleInsn(Op.Simple.i64x2_extmul_high_i32x4_u);
    public static final SimpleInsn i64x2_extmul_low_i32x4_s =
            new SimpleInsn(Op.Simple.i64x2_extmul_low_i32x4_s);
    public static final SimpleInsn i64x2_extmul_low_i32x4_u =
            new SimpleInsn(Op.Simple.i64x2_extmul_low_i32x4_u);
    public static final SimpleInsn i64x2_max_s = new SimpleInsn(Op.Simple.i64x2_max_s);
    public static final SimpleInsn i64x2_max_u = new SimpleInsn(Op.Simple.i64x2_max_u);
    public static final SimpleInsn i64x2_min_s = new SimpleInsn(Op.Simple.i64x2_min_s);
    public static final SimpleInsn i64x2_min_u = new SimpleInsn(Op.Simple.i64x2_min_u);
    public static final SimpleInsn i64x2_mul = new SimpleInsn(Op.Simple.i64x2_mul);
    public static final SimpleInsn i64x2_neg = new SimpleInsn(Op.Simple.i64x2_neg);
    public static final SimpleInsn i64x2_shl = new SimpleInsn(Op.Simple.i64x2_shl);
    public static final SimpleInsn i64x2_shr_s = new SimpleInsn(Op.Simple.i64x2_shr_s);
    public static final SimpleInsn i64x2_shr_u = new SimpleInsn(Op.Simple.i64x2_shr_u);
    public static final SimpleInsn i64x2_splat = new SimpleInsn(Op.Simple.i64x2_splat);
    public static final SimpleInsn i64x2_sub = new SimpleInsn(Op.Simple.i64x2_sub);
    public static final SimpleInsn i8x16_abs = new SimpleInsn(Op.Simple.i8x16_abs);
    public static final SimpleInsn i8x16_add = new SimpleInsn(Op.Simple.i8x16_add);
    public static final SimpleInsn i8x16_add_sat_s = new SimpleInsn(Op.Simple.i8x16_add_sat_s);
    public static final SimpleInsn i8x16_add_sat_u = new SimpleInsn(Op.Simple.i8x16_add_sat_u);
    public static final SimpleInsn i8x16_all_true = new SimpleInsn(Op.Simple.i8x16_all_true);
    public static final SimpleInsn i8x16_avgr_u = new SimpleInsn(Op.Simple.i8x16_avgr_u);
    public static final SimpleInsn i8x16_bitmask = new SimpleInsn(Op.Simple.i8x16_bitmask);
    public static final SimpleInsn i8x16_eq = new SimpleInsn(Op.Simple.i8x16_eq);
    public static final SimpleInsn i8x16_ge_s = new SimpleInsn(Op.Simple.i8x16_ge_s);
    public static final SimpleInsn i8x16_ge_u = new SimpleInsn(Op.Simple.i8x16_ge_u);
    public static final SimpleInsn i8x16_gt_s = new SimpleInsn(Op.Simple.i8x16_gt_s);
    public static final SimpleInsn i8x16_gt_u = new SimpleInsn(Op.Simple.i8x16_gt_u);
    public static final SimpleInsn i8x16_le_s = new SimpleInsn(Op.Simple.i8x16_le_s);
    public static final SimpleInsn i8x16_le_u = new SimpleInsn(Op.Simple.i8x16_le_u);
    public static final SimpleInsn i8x16_lt_s = new SimpleInsn(Op.Simple.i8x16_lt_s);
    public static final SimpleInsn i8x16_lt_u = new SimpleInsn(Op.Simple.i8x16_lt_u);
    public static final SimpleInsn i8x16_max_s = new SimpleInsn(Op.Simple.i8x16_max_s);
    public static final SimpleInsn i8x16_max_u = new SimpleInsn(Op.Simple.i8x16_max_u);
    public static final SimpleInsn i8x16_min_s = new SimpleInsn(Op.Simple.i8x16_min_s);
    public static final SimpleInsn i8x16_min_u = new SimpleInsn(Op.Simple.i8x16_min_u);
    public static final SimpleInsn i8x16_narrow_i16x8_s =
            new SimpleInsn(Op.Simple.i8x16_narrow_i16x8_s);
    public static final SimpleInsn i8x16_narrow_i16x8_u =
            new SimpleInsn(Op.Simple.i8x16_narrow_i16x8_u);
    public static final SimpleInsn i8x16_ne = new SimpleInsn(Op.Simple.i8x16_ne);
    public static final SimpleInsn i8x16_neg = new SimpleInsn(Op.Simple.i8x16_neg);
    public static final SimpleInsn i8x16_popcnt = new SimpleInsn(Op.Simple.i8x16_popcnt);
    public static final SimpleInsn i8x16_shl = new SimpleInsn(Op.Simple.i8x16_shl);
    public static final SimpleInsn i8x16_shr_s = new SimpleInsn(Op.Simple.i8x16_shr_s);
    public static final SimpleInsn i8x16_shr_u = new SimpleInsn(Op.Simple.i8x16_shr_u);
    public static final SimpleInsn i8x16_splat = new SimpleInsn(Op.Simple.i8x16_splat);
    public static final SimpleInsn i8x16_sub = new SimpleInsn(Op.Simple.i8x16_sub);
    public static final SimpleInsn i8x16_sub_sat_s = new SimpleInsn(Op.Simple.i8x16_sub_sat_s);
    public static final SimpleInsn i8x16_sub_sat_u = new SimpleInsn(Op.Simple.i8x16_sub_sat_u);
    public static final SimpleInsn i8x16_swizzle = new SimpleInsn(Op.Simple.i8x16_swizzle);
    public static final SimpleInsn nop = new SimpleInsn(Op.Simple.nop);
    public static final SimpleInsn ref_is_null = new SimpleInsn(Op.Simple.ref_is_null);
    public static final SimpleInsn return_ = new SimpleInsn(Op.Simple.return_);
    public static final SimpleInsn select = new SimpleInsn(Op.Simple.select);
    public static final SimpleInsn unreachable = new SimpleInsn(Op.Simple.unreachable);
    public static final SimpleInsn v128_and = new SimpleInsn(Op.Simple.v128_and);
    public static final SimpleInsn v128_andnot = new SimpleInsn(Op.Simple.v128_andnot);
    public static final SimpleInsn v128_any_true = new SimpleInsn(Op.Simple.v128_any_true);
    public static final SimpleInsn v128_bitselect = new SimpleInsn(Op.Simple.v128_bitselect);
    public static final SimpleInsn v128_not = new SimpleInsn(Op.Simple.v128_not);
    public static final SimpleInsn v128_or = new SimpleInsn(Op.Simple.v128_or);
    public static final SimpleInsn v128_xor = new SimpleInsn(Op.Simple.v128_xor);
    // atomics spec
    public static final SimpleInsn atomic_fence = new SimpleInsn(Op.Simple.atomic_fence);

    public boolean equals(final Object obj) {
        return this == obj;
    }

    public static SimpleInsn forOp(Op.Simple insn) {
        // todo: marginally nicer with Java 17-style switch expressions
        switch (insn) {
            case catch_all:
                return catch_all;
            case drop:
                return drop;
            case else_:
                return else_;
            case end:
                return end;
            case f32_abs:
                return f32_abs;
            case f32_add:
                return f32_add;
            case f32_ceil:
                return f32_ceil;
            case f32_convert_i32_s:
                return f32_convert_i32_s;
            case f32_convert_i32_u:
                return f32_convert_i32_u;
            case f32_convert_i64_s:
                return f32_convert_i64_s;
            case f32_convert_i64_u:
                return f32_convert_i64_u;
            case f32_copysign:
                return f32_copysign;
            case f32_demote_f64:
                return f32_demote_f64;
            case f32_div:
                return f32_div;
            case f32_eq:
                return f32_eq;
            case f32_floor:
                return f32_floor;
            case f32_ge:
                return f32_ge;
            case f32_gt:
                return f32_gt;
            case f32_le:
                return f32_le;
            case f32_lt:
                return f32_lt;
            case f32_max:
                return f32_max;
            case f32_min:
                return f32_min;
            case f32_mul:
                return f32_mul;
            case f32_ne:
                return f32_ne;
            case f32_nearest:
                return f32_nearest;
            case f32_neg:
                return f32_neg;
            case f32_reinterpret_i32:
                return f32_reinterpret_i32;
            case f32_sqrt:
                return f32_sqrt;
            case f32_sub:
                return f32_sub;
            case f32_trunc:
                return f32_trunc;
            case f32x4_abs:
                return f32x4_abs;
            case f32x4_add:
                return f32x4_add;
            case f32x4_ceil:
                return f32x4_ceil;
            case f32x4_convert_i32x4_s:
                return f32x4_convert_i32x4_s;
            case f32x4_convert_i32x4_u:
                return f32x4_convert_i32x4_u;
            case f32x4_demote_f64x2_zero:
                return f32x4_demote_f64x2_zero;
            case f32x4_div:
                return f32x4_div;
            case f32x4_eq:
                return f32x4_eq;
            case f32x4_floor:
                return f32x4_floor;
            case f32x4_ge:
                return f32x4_ge;
            case f32x4_gt:
                return f32x4_gt;
            case f32x4_le:
                return f32x4_le;
            case f32x4_lt:
                return f32x4_lt;
            case f32x4_max:
                return f32x4_max;
            case f32x4_min:
                return f32x4_min;
            case f32x4_mul:
                return f32x4_mul;
            case f32x4_ne:
                return f32x4_ne;
            case f32x4_nearest:
                return f32x4_nearest;
            case f32x4_neg:
                return f32x4_neg;
            case f32x4_pmax:
                return f32x4_pmax;
            case f32x4_pmin:
                return f32x4_pmin;
            case f32x4_splat:
                return f32x4_splat;
            case f32x4_sqrt:
                return f32x4_sqrt;
            case f32x4_sub:
                return f32x4_sub;
            case f32x4_trunc:
                return f32x4_trunc;
            case f64_abs:
                return f64_abs;
            case f64_add:
                return f64_add;
            case f64_ceil:
                return f64_ceil;
            case f64_convert_i32_s:
                return f64_convert_i32_s;
            case f64_convert_i32_u:
                return f64_convert_i32_u;
            case f64_convert_i64_s:
                return f64_convert_i64_s;
            case f64_convert_i64_u:
                return f64_convert_i64_u;
            case f64_copysign:
                return f64_copysign;
            case f64_div:
                return f64_div;
            case f64_eq:
                return f64_eq;
            case f64_floor:
                return f64_floor;
            case f64_ge:
                return f64_ge;
            case f64_gt:
                return f64_gt;
            case f64_le:
                return f64_le;
            case f64_lt:
                return f64_lt;
            case f64_max:
                return f64_max;
            case f64_min:
                return f64_min;
            case f64_mul:
                return f64_mul;
            case f64_ne:
                return f64_ne;
            case f64_nearest:
                return f64_nearest;
            case f64_neg:
                return f64_neg;
            case f64_promote_f32:
                return f64_promote_f32;
            case f64_reinterpret_i64:
                return f64_reinterpret_i64;
            case f64_sqrt:
                return f64_sqrt;
            case f64_sub:
                return f64_sub;
            case f64_trunc:
                return f64_trunc;
            case f64x2_abs:
                return f64x2_abs;
            case f64x2_add:
                return f64x2_add;
            case f64x2_ceil:
                return f64x2_ceil;
            case f64x2_convert_low_i32x4_s:
                return f64x2_convert_low_i32x4_s;
            case f64x2_convert_low_i32x4_u:
                return f64x2_convert_low_i32x4_u;
            case f64x2_div:
                return f64x2_div;
            case f64x2_eq:
                return f64x2_eq;
            case f64x2_floor:
                return f64x2_floor;
            case f64x2_ge:
                return f64x2_ge;
            case f64x2_gt:
                return f64x2_gt;
            case f64x2_le:
                return f64x2_le;
            case f64x2_lt:
                return f64x2_lt;
            case f64x2_max:
                return f64x2_max;
            case f64x2_min:
                return f64x2_min;
            case f64x2_mul:
                return f64x2_mul;
            case f64x2_ne:
                return f64x2_ne;
            case f64x2_nearest:
                return f64x2_nearest;
            case f64x2_neg:
                return f64x2_neg;
            case f64x2_pmax:
                return f64x2_pmax;
            case f64x2_pmin:
                return f64x2_pmin;
            case f64x2_promote_low_f32x4:
                return f64x2_promote_low_f32x4;
            case f64x2_splat:
                return f64x2_splat;
            case f64x2_sqrt:
                return f64x2_sqrt;
            case f64x2_sub:
                return f64x2_sub;
            case f64x2_trunc:
                return f64x2_trunc;
            case i16x8_abs:
                return i16x8_abs;
            case i16x8_add:
                return i16x8_add;
            case i16x8_add_sat_s:
                return i16x8_add_sat_s;
            case i16x8_add_sat_u:
                return i16x8_add_sat_u;
            case i16x8_all_true:
                return i16x8_all_true;
            case i16x8_avgr_u:
                return i16x8_avgr_u;
            case i16x8_bitmask:
                return i16x8_bitmask;
            case i16x8_eq:
                return i16x8_eq;
            case i16x8_extadd_pariwise_i8x16_s:
                return i16x8_extadd_pariwise_i8x16_s;
            case i16x8_extadd_pariwise_i8x16_u:
                return i16x8_extadd_pariwise_i8x16_u;
            case i16x8_extend_high_i8x16_s:
                return i16x8_extend_high_i8x16_s;
            case i16x8_extend_high_i8x16_u:
                return i16x8_extend_high_i8x16_u;
            case i16x8_extend_low_i8x16_s:
                return i16x8_extend_low_i8x16_s;
            case i16x8_extend_low_i8x16_u:
                return i16x8_extend_low_i8x16_u;
            case i16x8_extmul_high_i8x16_s:
                return i16x8_extmul_high_i8x16_s;
            case i16x8_extmul_high_i8x16_u:
                return i16x8_extmul_high_i8x16_u;
            case i16x8_extmul_low_i8x16_s:
                return i16x8_extmul_low_i8x16_s;
            case i16x8_extmul_low_i8x16_u:
                return i16x8_extmul_low_i8x16_u;
            case i16x8_ge_s:
                return i16x8_ge_s;
            case i16x8_ge_u:
                return i16x8_ge_u;
            case i16x8_gt_s:
                return i16x8_gt_s;
            case i16x8_gt_u:
                return i16x8_gt_u;
            case i16x8_le_s:
                return i16x8_le_s;
            case i16x8_le_u:
                return i16x8_le_u;
            case i16x8_lt_s:
                return i16x8_lt_s;
            case i16x8_lt_u:
                return i16x8_lt_u;
            case i16x8_max_s:
                return i16x8_max_s;
            case i16x8_max_u:
                return i16x8_max_u;
            case i16x8_min_s:
                return i16x8_min_s;
            case i16x8_min_u:
                return i16x8_min_u;
            case i16x8_mul:
                return i16x8_mul;
            case i16x8_narrow_i32x4_s:
                return i16x8_narrow_i32x4_s;
            case i16x8_narrow_i32x4_u:
                return i16x8_narrow_i32x4_u;
            case i16x8_ne:
                return i16x8_ne;
            case i16x8_neg:
                return i16x8_neg;
            case i16x8_q15mulr_sat_s:
                return i16x8_q15mulr_sat_s;
            case i16x8_shl:
                return i16x8_shl;
            case i16x8_shr_s:
                return i16x8_shr_s;
            case i16x8_shr_u:
                return i16x8_shr_u;
            case i16x8_splat:
                return i16x8_splat;
            case i16x8_sub:
                return i16x8_sub;
            case i16x8_sub_sat_s:
                return i16x8_sub_sat_s;
            case i16x8_sub_sat_u:
                return i16x8_sub_sat_u;
            case i32_add:
                return i32_add;
            case i32_and:
                return i32_and;
            case i32_clz:
                return i32_clz;
            case i32_ctz:
                return i32_ctz;
            case i32_div_s:
                return i32_div_s;
            case i32_div_u:
                return i32_div_u;
            case i32_eq:
                return i32_eq;
            case i32_eqz:
                return i32_eqz;
            case i32_extend16_s:
                return i32_extend16_s;
            case i32_extend8_s:
                return i32_extend8_s;
            case i32_ge_s:
                return i32_ge_s;
            case i32_ge_u:
                return i32_ge_u;
            case i32_gt_s:
                return i32_gt_s;
            case i32_gt_u:
                return i32_gt_u;
            case i32_le_s:
                return i32_le_s;
            case i32_le_u:
                return i32_le_u;
            case i32_lt_s:
                return i32_lt_s;
            case i32_lt_u:
                return i32_lt_u;
            case i32_mul:
                return i32_mul;
            case i32_ne:
                return i32_ne;
            case i32_or:
                return i32_or;
            case i32_popcnt:
                return i32_popcnt;
            case i32_reinterpret_f32:
                return i32_reinterpret_f32;
            case i32_rem_s:
                return i32_rem_s;
            case i32_rem_u:
                return i32_rem_u;
            case i32_rotl:
                return i32_rotl;
            case i32_rotr:
                return i32_rotr;
            case i32_shl:
                return i32_shl;
            case i32_shr_s:
                return i32_shr_s;
            case i32_shr_u:
                return i32_shr_u;
            case i32_sub:
                return i32_sub;
            case i32_trunc_f32_s:
                return i32_trunc_f32_s;
            case i32_trunc_f32_u:
                return i32_trunc_f32_u;
            case i32_trunc_f64_s:
                return i32_trunc_f64_s;
            case i32_trunc_f64_u:
                return i32_trunc_f64_u;
            case i32_trunc_sat_f32_s:
                return i32_trunc_sat_f32_s;
            case i32_trunc_sat_f32_u:
                return i32_trunc_sat_f32_u;
            case i32_trunc_sat_f64_s:
                return i32_trunc_sat_f64_s;
            case i32_trunc_sat_f64_u:
                return i32_trunc_sat_f64_u;
            case i32_wrap_i64:
                return i32_wrap_i64;
            case i32_xor:
                return i32_xor;
            case i32x4_abs:
                return i32x4_abs;
            case i32x4_add:
                return i32x4_add;
            case i32x4_all_true:
                return i32x4_all_true;
            case i32x4_bitmask:
                return i32x4_bitmask;
            case i32x4_dot_i16x8_s:
                return i32x4_dot_i16x8_s;
            case i32x4_eq:
                return i32x4_eq;
            case i32x4_extadd_pariwise_i16x8_s:
                return i32x4_extadd_pariwise_i16x8_s;
            case i32x4_extadd_pariwise_i16x8_u:
                return i32x4_extadd_pariwise_i16x8_u;
            case i32x4_extend_high_i16x8_s:
                return i32x4_extend_high_i16x8_s;
            case i32x4_extend_high_i16x8_u:
                return i32x4_extend_high_i16x8_u;
            case i32x4_extend_low_i16x8_s:
                return i32x4_extend_low_i16x8_s;
            case i32x4_extend_low_i16x8_u:
                return i32x4_extend_low_i16x8_u;
            case i32x4_extmul_high_i16x8_s:
                return i32x4_extmul_high_i16x8_s;
            case i32x4_extmul_high_i16x8_u:
                return i32x4_extmul_high_i16x8_u;
            case i32x4_extmul_low_i16x8_s:
                return i32x4_extmul_low_i16x8_s;
            case i32x4_extmul_low_i16x8_u:
                return i32x4_extmul_low_i16x8_u;
            case i32x4_ge_s:
                return i32x4_ge_s;
            case i32x4_ge_u:
                return i32x4_ge_u;
            case i32x4_gt_s:
                return i32x4_gt_s;
            case i32x4_gt_u:
                return i32x4_gt_u;
            case i32x4_le_s:
                return i32x4_le_s;
            case i32x4_le_u:
                return i32x4_le_u;
            case i32x4_lt_s:
                return i32x4_lt_s;
            case i32x4_lt_u:
                return i32x4_lt_u;
            case i32x4_max_s:
                return i32x4_max_s;
            case i32x4_max_u:
                return i32x4_max_u;
            case i32x4_min_s:
                return i32x4_min_s;
            case i32x4_min_u:
                return i32x4_min_u;
            case i32x4_mul:
                return i32x4_mul;
            case i32x4_ne:
                return i32x4_ne;
            case i32x4_neg:
                return i32x4_neg;
            case i32x4_shl:
                return i32x4_shl;
            case i32x4_shr_s:
                return i32x4_shr_s;
            case i32x4_shr_u:
                return i32x4_shr_u;
            case i32x4_splat:
                return i32x4_splat;
            case i32x4_sub:
                return i32x4_sub;
            case i32x4_trunc_sat_f32x4_s:
                return i32x4_trunc_sat_f32x4_s;
            case i32x4_trunc_sat_f32x4_u:
                return i32x4_trunc_sat_f32x4_u;
            case i32x4_trunc_sat_f64x2_s_zero:
                return i32x4_trunc_sat_f64x2_s_zero;
            case i32x4_trunc_sat_f64x2_u_zero:
                return i32x4_trunc_sat_f64x2_u_zero;
            case i64_add:
                return i64_add;
            case i64_and:
                return i64_and;
            case i64_clz:
                return i64_clz;
            case i64_ctz:
                return i64_ctz;
            case i64_div_s:
                return i64_div_s;
            case i64_div_u:
                return i64_div_u;
            case i64_eq:
                return i64_eq;
            case i64_eqz:
                return i64_eqz;
            case i64_extend16_s:
                return i64_extend16_s;
            case i64_extend32_s:
                return i64_extend32_s;
            case i64_extend8_s:
                return i64_extend8_s;
            case i64_extend_i32_s:
                return i64_extend_i32_s;
            case i64_extend_i32_u:
                return i64_extend_i32_u;
            case i64_ge_s:
                return i64_ge_s;
            case i64_ge_u:
                return i64_ge_u;
            case i64_gt_s:
                return i64_gt_s;
            case i64_gt_u:
                return i64_gt_u;
            case i64_le_s:
                return i64_le_s;
            case i64_le_u:
                return i64_le_u;
            case i64_lt_s:
                return i64_lt_s;
            case i64_lt_u:
                return i64_lt_u;
            case i64_mul:
                return i64_mul;
            case i64_ne:
                return i64_ne;
            case i64_or:
                return i64_or;
            case i64_popcnt:
                return i64_popcnt;
            case i64_reinterpret_f64:
                return i64_reinterpret_f64;
            case i64_rem_s:
                return i64_rem_s;
            case i64_rem_u:
                return i64_rem_u;
            case i64_rotl:
                return i64_rotl;
            case i64_rotr:
                return i64_rotr;
            case i64_shl:
                return i64_shl;
            case i64_shr_s:
                return i64_shr_s;
            case i64_shr_u:
                return i64_shr_u;
            case i64_sub:
                return i64_sub;
            case i64_trunc_f32_s:
                return i64_trunc_f32_s;
            case i64_trunc_f32_u:
                return i64_trunc_f32_u;
            case i64_trunc_f64_s:
                return i64_trunc_f64_s;
            case i64_trunc_f64_u:
                return i64_trunc_f64_u;
            case i64_trunc_sat_f32_s:
                return i64_trunc_sat_f32_s;
            case i64_trunc_sat_f32_u:
                return i64_trunc_sat_f32_u;
            case i64_trunc_sat_f64_s:
                return i64_trunc_sat_f64_s;
            case i64_trunc_sat_f64_u:
                return i64_trunc_sat_f64_u;
            case i64_xor:
                return i64_xor;
            case i64x2_abs:
                return i64x2_abs;
            case i64x2_add:
                return i64x2_add;
            case i64x2_all_true:
                return i64x2_all_true;
            case i64x2_bitmask:
                return i64x2_bitmask;
            case i64x2_dot_i16x8_s:
                return i64x2_dot_i16x8_s;
            case i64x2_extend_high_i32x4_s:
                return i64x2_extend_high_i32x4_s;
            case i64x2_extend_high_i32x4_u:
                return i64x2_extend_high_i32x4_u;
            case i64x2_extend_low_i32x4_s:
                return i64x2_extend_low_i32x4_s;
            case i64x2_extend_low_i32x4_u:
                return i64x2_extend_low_i32x4_u;
            case i64x2_extmul_high_i32x4_s:
                return i64x2_extmul_high_i32x4_s;
            case i64x2_extmul_high_i32x4_u:
                return i64x2_extmul_high_i32x4_u;
            case i64x2_extmul_low_i32x4_s:
                return i64x2_extmul_low_i32x4_s;
            case i64x2_extmul_low_i32x4_u:
                return i64x2_extmul_low_i32x4_u;
            case i64x2_max_s:
                return i64x2_max_s;
            case i64x2_max_u:
                return i64x2_max_u;
            case i64x2_min_s:
                return i64x2_min_s;
            case i64x2_min_u:
                return i64x2_min_u;
            case i64x2_mul:
                return i64x2_mul;
            case i64x2_neg:
                return i64x2_neg;
            case i64x2_shl:
                return i64x2_shl;
            case i64x2_shr_s:
                return i64x2_shr_s;
            case i64x2_shr_u:
                return i64x2_shr_u;
            case i64x2_splat:
                return i64x2_splat;
            case i64x2_sub:
                return i64x2_sub;
            case i8x16_abs:
                return i8x16_abs;
            case i8x16_add:
                return i8x16_add;
            case i8x16_add_sat_s:
                return i8x16_add_sat_s;
            case i8x16_add_sat_u:
                return i8x16_add_sat_u;
            case i8x16_all_true:
                return i8x16_all_true;
            case i8x16_avgr_u:
                return i8x16_avgr_u;
            case i8x16_bitmask:
                return i8x16_bitmask;
            case i8x16_eq:
                return i8x16_eq;
            case i8x16_ge_s:
                return i8x16_ge_s;
            case i8x16_ge_u:
                return i8x16_ge_u;
            case i8x16_gt_s:
                return i8x16_gt_s;
            case i8x16_gt_u:
                return i8x16_gt_u;
            case i8x16_le_s:
                return i8x16_le_s;
            case i8x16_le_u:
                return i8x16_le_u;
            case i8x16_lt_s:
                return i8x16_lt_s;
            case i8x16_lt_u:
                return i8x16_lt_u;
            case i8x16_max_s:
                return i8x16_max_s;
            case i8x16_max_u:
                return i8x16_max_u;
            case i8x16_min_s:
                return i8x16_min_s;
            case i8x16_min_u:
                return i8x16_min_u;
            case i8x16_narrow_i16x8_s:
                return i8x16_narrow_i16x8_s;
            case i8x16_narrow_i16x8_u:
                return i8x16_narrow_i16x8_u;
            case i8x16_ne:
                return i8x16_ne;
            case i8x16_neg:
                return i8x16_neg;
            case i8x16_popcnt:
                return i8x16_popcnt;
            case i8x16_shl:
                return i8x16_shl;
            case i8x16_shr_s:
                return i8x16_shr_s;
            case i8x16_shr_u:
                return i8x16_shr_u;
            case i8x16_splat:
                return i8x16_splat;
            case i8x16_sub:
                return i8x16_sub;
            case i8x16_sub_sat_s:
                return i8x16_sub_sat_s;
            case i8x16_sub_sat_u:
                return i8x16_sub_sat_u;
            case i8x16_swizzle:
                return i8x16_swizzle;
            case nop:
                return nop;
            case ref_is_null:
                return ref_is_null;
            case return_:
                return return_;
            case select:
                return select;
            case unreachable:
                return unreachable;
            case v128_and:
                return v128_and;
            case v128_andnot:
                return v128_andnot;
            case v128_any_true:
                return v128_any_true;
            case v128_bitselect:
                return v128_bitselect;
            case v128_not:
                return v128_not;
            case v128_or:
                return v128_or;
            case v128_xor:
                return v128_xor;
            case atomic_fence:
                return atomic_fence;
            default:
                throw new IllegalArgumentException();
        }
    }
}
