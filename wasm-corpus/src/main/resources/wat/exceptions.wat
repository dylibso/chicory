(module

  (tag $e0 (export "e0"))
  (tag $e1)
  (tag $e2)
  (tag $e-i32 (param i32))

  (func (export "catch-complex-1") (param i32) (result i32)
    (block $h1
      (try_table (result i32) (catch $e1 $h1)
        (block $h0
          (try_table (result i32) (catch $e0 $h0)
            (if (i32.eqz (local.get 0))
              (then (throw $e0))
              (else
                (if (i32.eq (local.get 0) (i32.const 1))
                  (then (throw $e1))
                  (else (throw $e2))
                )
              )
            )
            (i32.const 2)
          )
          (br 1)
        )
        (i32.const 3)
      )
      (return)
    )
    (i32.const 4)
  )

  (func $throw-if (param i32) (result i32)
    (local.get 0)
    (i32.const 0) (if (i32.ne) (then (throw $e0)))
    (i32.const 0)
  )

  (func $catchless-try (export "catchless-try") (param i32) (result i32)
    (block $h
      (try_table (result i32) (catch $e0 $h)
        (try_table (result i32) (call $throw-if (local.get 0)))
      )
      (return)
    )
    (i32.const 1)
  )

)