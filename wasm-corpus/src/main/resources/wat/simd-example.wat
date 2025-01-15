(module
  (func $add_vectors (param v128) (param v128) (result v128)
    (i32x4.add (local.get 0) (local.get 1))
  )

  (func $main (export "main") (result i32)
    v128.const i32x4 1 2 3 4
    v128.const i32x4 5 6 7 8
    call $add_vectors
    i32x4.extract_lane 0
  )
)
