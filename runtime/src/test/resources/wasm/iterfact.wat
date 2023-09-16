(module
;; $iterFact computes factorial:
;; int result = 1;
;; while (i > 0) {
;;   result = result * i;
;;   i = i - 1;
;; }
(func $iterFact (param i32) (result i32)
       (local i32)
       i32.const 1
       local.set 1
       (block
           local.get 0
           i32.eqz
           br_if 0
           (loop
            local.get 1
            local.get 0
            i32.mul
            local.set 1
            local.get 0
            i32.const -1
            i32.add
            local.tee 0
            i32.eqz
            br_if 1
            br 0))
       local.get 1)

(export "iterFact" (func $iterFact))
)