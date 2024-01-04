(module
;; 64 bit version
;; $iterFact computes factorial:
;; int result = 1;
;; while (i > 0) {
;;   result = result * i;
;;   i = i - 1;
;; }
(func $iterFact (param i64) (result i64)
       (local i64)
       i64.const 1
       local.set 1
       (block
           local.get 0
           i64.eqz
           br_if 0
           (loop
            local.get 1
            local.get 0
            i64.mul
            local.set 1
            local.get 0
            i64.const -1
            i64.add
            local.tee 0
            i64.eqz
            br_if 1
            br 0))
       local.get 1)

(export "iterFact" (func $iterFact))
)