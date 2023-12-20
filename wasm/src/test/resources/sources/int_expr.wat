;; Test interesting integer "expressions". These tests contain code
;; patterns which tempt common value-changing optimizations.

;; Test that x+1<y+1 is not folded to x<y.

(module
  (func (export "i32.no_fold_cmp_s_offset") (param $x i32) (param $y i32) (result i32)
    (i32.lt_s (i32.add (local.get $x) (i32.const 1)) (i32.add (local.get $y) (i32.const 1))))
  (func (export "i32.no_fold_cmp_u_offset") (param $x i32) (param $y i32) (result i32)
    (i32.lt_u (i32.add (local.get $x) (i32.const 1)) (i32.add (local.get $y) (i32.const 1))))

  (func (export "i64.no_fold_cmp_s_offset") (param $x i64) (param $y i64) (result i32)
    (i64.lt_s (i64.add (local.get $x) (i64.const 1)) (i64.add (local.get $y) (i64.const 1))))
  (func (export "i64.no_fold_cmp_u_offset") (param $x i64) (param $y i64) (result i32)
    (i64.lt_u (i64.add (local.get $x) (i64.const 1)) (i64.add (local.get $y) (i64.const 1))))

;; Test that wrap(extend_s(x)) is not folded to x.
;;
;;(module
;;  (func (export "i64.no_fold_wrap_extend_s") (param $x i64) (result i64)
;;    (i64.extend_i32_s (i32.wrap_i64 (local.get $x)))
;;)
;;
;;;; Test that wrap(extend_u(x)) is not folded to x.
;;
;;(module
;;  (func (export "i64.no_fold_wrap_extend_u") (param $x i64) (result i64)
;;    (i64.extend_i32_u (i32.wrap_i64 (local.get $x))))
;;)
;;
;; Test that x<<n>>n is not folded to x.

  (func (export "i32.no_fold_shl_shr_s") (param $x i32) (result i32)
    (i32.shr_s (i32.shl (local.get $x) (i32.const 1)) (i32.const 1)))
  (func (export "i32.no_fold_shl_shr_u") (param $x i32) (result i32)
    (i32.shr_u (i32.shl (local.get $x) (i32.const 1)) (i32.const 1)))

  (func (export "i64.no_fold_shl_shr_s") (param $x i64) (result i64)
    (i64.shr_s (i64.shl (local.get $x) (i64.const 1)) (i64.const 1)))
  (func (export "i64.no_fold_shl_shr_u") (param $x i64) (result i64)
    (i64.shr_u (i64.shl (local.get $x) (i64.const 1)) (i64.const 1)))

;; Test that x>>n<<n is not folded to x.

  (func (export "i32.no_fold_shr_s_shl") (param $x i32) (result i32)
    (i32.shl (i32.shr_s (local.get $x) (i32.const 1)) (i32.const 1)))
  (func (export "i32.no_fold_shr_u_shl") (param $x i32) (result i32)
    (i32.shl (i32.shr_u (local.get $x) (i32.const 1)) (i32.const 1)))

  (func (export "i64.no_fold_shr_s_shl") (param $x i64) (result i64)
    (i64.shl (i64.shr_s (local.get $x) (i64.const 1)) (i64.const 1)))
  (func (export "i64.no_fold_shr_u_shl") (param $x i64) (result i64)
    (i64.shl (i64.shr_u (local.get $x) (i64.const 1)) (i64.const 1)))
;; Test that x/n*n is not folded to x.

  (func (export "i32.no_fold_div_s_mul") (param $x i32) (result i32)
    (i32.mul (i32.div_s (local.get $x) (i32.const 6)) (i32.const 6)))
  (func (export "i32.no_fold_div_u_mul") (param $x i32) (result i32)
    (i32.mul (i32.div_u (local.get $x) (i32.const 6)) (i32.const 6)))

  (func (export "i64.no_fold_div_s_mul") (param $x i64) (result i64)
    (i64.mul (i64.div_s (local.get $x) (i64.const 6)) (i64.const 6)))
  (func (export "i64.no_fold_div_u_mul") (param $x i64) (result i64)
    (i64.mul (i64.div_u (local.get $x) (i64.const 6)) (i64.const 6)))

;; Test that x/x is not folded to 1.

  (func (export "i32.no_fold_div_s_self") (param $x i32) (result i32)
    (i32.div_s (local.get $x) (local.get $x)))
  (func (export "i32.no_fold_div_u_self") (param $x i32) (result i32)
    (i32.div_u (local.get $x) (local.get $x)))

  (func (export "i64.no_fold_div_s_self") (param $x i64) (result i64)
    (i64.div_s (local.get $x) (local.get $x)))
  (func (export "i64.no_fold_div_u_self") (param $x i64) (result i64)
    (i64.div_u (local.get $x) (local.get $x)))

;; Test that x%x is not folded to 0.

  (func (export "i32.no_fold_rem_s_self") (param $x i32) (result i32)
    (i32.rem_s (local.get $x) (local.get $x)))
  (func (export "i32.no_fold_rem_u_self") (param $x i32) (result i32)
    (i32.rem_u (local.get $x) (local.get $x)))

  (func (export "i64.no_fold_rem_s_self") (param $x i64) (result i64)
    (i64.rem_s (local.get $x) (local.get $x)))
  (func (export "i64.no_fold_rem_u_self") (param $x i64) (result i64)
    (i64.rem_u (local.get $x) (local.get $x)))
;; Test that x*n/n is not folded to x.

  (func (export "i32.no_fold_mul_div_s") (param $x i32) (result i32)
    (i32.div_s (i32.mul (local.get $x) (i32.const 6)) (i32.const 6)))
  (func (export "i32.no_fold_mul_div_u") (param $x i32) (result i32)
    (i32.div_u (i32.mul (local.get $x) (i32.const 6)) (i32.const 6)))

  (func (export "i64.no_fold_mul_div_s") (param $x i64) (result i64)
    (i64.div_s (i64.mul (local.get $x) (i64.const 6)) (i64.const 6)))
  (func (export "i64.no_fold_mul_div_u") (param $x i64) (result i64)
    (i64.div_u (i64.mul (local.get $x) (i64.const 6)) (i64.const 6)))

;; Test that x/n where n is a known power of 2 is not folded to shr_s.

  (func (export "i32.no_fold_div_s_2") (param $x i32) (result i32)
    (i32.div_s (local.get $x) (i32.const 2)))

  (func (export "i64.no_fold_div_s_2") (param $x i64) (result i64)
    (i64.div_s (local.get $x) (i64.const 2)))

;; Test that x%n where n is a known power of 2 is not folded to and.

  (func (export "i32.no_fold_rem_s_2") (param $x i32) (result i32)
    (i32.rem_s (local.get $x) (i32.const 2)))

  (func (export "i64.no_fold_rem_s_2") (param $x i64) (result i64)
    (i64.rem_s (local.get $x) (i64.const 2)))


  (func (export "i32.div_s_0") (param $x i32) (result i32)
    (i32.div_s (local.get $x) (i32.const 0)))
  (func (export "i32.div_u_0") (param $x i32) (result i32)
    (i32.div_u (local.get $x) (i32.const 0)))

  (func (export "i64.div_s_0") (param $x i64) (result i64)
    (i64.div_s (local.get $x) (i64.const 0)))
  (func (export "i64.div_u_0") (param $x i64) (result i64)
    (i64.div_u (local.get $x) (i64.const 0)))

;; Test that x/3 works.

  (func (export "i32.div_s_3") (param $x i32) (result i32)
    (i32.div_s (local.get $x) (i32.const 3)))
  (func (export "i32.div_u_3") (param $x i32) (result i32)
    (i32.div_u (local.get $x) (i32.const 3)))

  (func (export "i64.div_s_3") (param $x i64) (result i64)
    (i64.div_s (local.get $x) (i64.const 3)))
  (func (export "i64.div_u_3") (param $x i64) (result i64)
    (i64.div_u (local.get $x) (i64.const 3)))

;; Test that x/5 works.

  (func (export "i32.div_s_5") (param $x i32) (result i32)
    (i32.div_s (local.get $x) (i32.const 5)))
  (func (export "i32.div_u_5") (param $x i32) (result i32)
    (i32.div_u (local.get $x) (i32.const 5)))

  (func (export "i64.div_s_5") (param $x i64) (result i64)
    (i64.div_s (local.get $x) (i64.const 5)))
  (func (export "i64.div_u_5") (param $x i64) (result i64)
    (i64.div_u (local.get $x) (i64.const 5)))

;; Test that x/7 works.

  (func (export "i32.div_s_7") (param $x i32) (result i32)
    (i32.div_s (local.get $x) (i32.const 7)))
  (func (export "i32.div_u_7") (param $x i32) (result i32)
    (i32.div_u (local.get $x) (i32.const 7)))

  (func (export "i64.div_s_7") (param $x i64) (result i64)
    (i64.div_s (local.get $x) (i64.const 7)))
  (func (export "i64.div_u_7") (param $x i64) (result i64)
    (i64.div_u (local.get $x) (i64.const 7)))

;; Test that x%3 works.

  (func (export "i32.rem_s_3") (param $x i32) (result i32)
    (i32.rem_s (local.get $x) (i32.const 3)))
  (func (export "i32.rem_u_3") (param $x i32) (result i32)
    (i32.rem_u (local.get $x) (i32.const 3)))

  (func (export "i64.rem_s_3") (param $x i64) (result i64)
    (i64.rem_s (local.get $x) (i64.const 3)))
  (func (export "i64.rem_u_3") (param $x i64) (result i64)
    (i64.rem_u (local.get $x) (i64.const 3)))
;; Test that x%5 works.

  (func (export "i32.rem_s_5") (param $x i32) (result i32)
    (i32.rem_s (local.get $x) (i32.const 5)))
  (func (export "i32.rem_u_5") (param $x i32) (result i32)
    (i32.rem_u (local.get $x) (i32.const 5)))

  (func (export "i64.rem_s_5") (param $x i64) (result i64)
    (i64.rem_s (local.get $x) (i64.const 5)))
  (func (export "i64.rem_u_5") (param $x i64) (result i64)
    (i64.rem_u (local.get $x) (i64.const 5)))

;; Test that x%7 works.

  (func (export "i32.rem_s_7") (param $x i32) (result i32)
    (i32.rem_s (local.get $x) (i32.const 7)))
  (func (export "i32.rem_u_7") (param $x i32) (result i32)
    (i32.rem_u (local.get $x) (i32.const 7)))

  (func (export "i64.rem_s_7") (param $x i64) (result i64)
    (i64.rem_s (local.get $x) (i64.const 7)))
  (func (export "i64.rem_u_7") (param $x i64) (result i64)
    (i64.rem_u (local.get $x) (i64.const 7)))

;; Test that x/-1 is not folded to -x.

  (func (export "i32.no_fold_div_neg1") (param $x i32) (result i32)
    (i32.div_s (local.get $x) (i32.const -1)))

  (func (export "i64.no_fold_div_neg1") (param $x i64) (result i64)
    (i64.div_s (local.get $x) (i64.const -1)))
)

