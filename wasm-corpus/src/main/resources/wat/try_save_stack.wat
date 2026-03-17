(module
  ;; Test that values below try_table are preserved after catch.
  ;; JVM exception handlers clear the operand stack, but WASM
  ;; try_table semantics preserve values below the try scope.

  (tag $e (param i32))

  (func $do_throw (param $val i32)
    (throw $e (local.get $val))
  )

  ;; Push a value, enter try_table, throw, catch — the value below must survive.
  (func (export "value-below-try") (result i32)
    (i32.const 10)               ;; this value is below the try scope
    (block $h (result i32)
      (try_table (catch $e $h)
        (call $do_throw (i32.const 32))
      )
      (unreachable)
    )
    (i32.add)                    ;; 10 + 32 = 42
  )

  ;; Two values below the try scope
  (func (export "two-values-below-try") (result i32)
    (i32.const 100)              ;; bottom value
    (i32.const 200)              ;; second value
    (block $h (result i32)
      (try_table (catch $e $h)
        (call $do_throw (i32.const 5))
      )
      (unreachable)
    )
    (i32.add)                    ;; 200 + 5 = 205
    (i32.add)                    ;; 100 + 205 = 305
  )

  ;; Nested try_table with values below both levels
  (func (export "nested-try-values") (result i32)
    (i32.const 1)                ;; below outer try
    (block $outer (result i32)
      (try_table (catch $e $outer)
        (i32.const 2)            ;; below inner try
        (block $inner (result i32)
          (try_table (catch $e $inner)
            (call $do_throw (i32.const 3))
          )
          (unreachable)
        )
        (i32.add)                ;; 2 + 3 = 5
        (call $do_throw)         ;; throw 5 to outer
      )
      (unreachable)
    )
    (i32.add)                    ;; 1 + 5 = 6
  )
)
