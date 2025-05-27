(module
  (tag $imported-e0 (import "test" "e0"))
  (func $imported-throw (import "test" "throw"))

  (func (export "catch-imported") (result i32)
    (block $h
      (try_table (result i32) (catch $imported-e0 $h)
        (call $imported-throw (i32.const 1))
      )
      (return)
    )
    (i32.const 2)
  )
)
