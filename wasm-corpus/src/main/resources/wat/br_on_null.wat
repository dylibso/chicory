(module
  (type $point (struct (field $x i32) (field $y i32)))

  ;; Helper that requires a non-nullable ref
  (func $get_x (param $p (ref $point)) (result i32)
    (struct.get $point $x (local.get $p))
  )

  ;; Test br_on_null: fall-through refines nullable ref to non-nullable.
  ;; Takes a nullable ref, returns the x field or -1 if null.
  ;; Calls $get_x which requires (ref $point), not (ref null $point).
  ;; Without the fix, the AOT compiler rejects this with:
  ;; "Expected type Ref[0] <> RefNull[0]"
  (func $get_x_or_default (export "get_x_or_default")
        (param $p (ref null $point)) (result i32)
    (block $is_null
      (br_on_null $is_null (local.get $p))
      ;; Fall-through: ref must be refined to non-nullable (ref $point)
      ;; to satisfy $get_x's parameter type
      (return (call $get_x))
    )
    ;; Null path
    (i32.const -1)
  )

  ;; Test the null path: passes null to get_x_or_default, expects -1.
  (func $test_null (export "test_null") (result i32)
    (call $get_x_or_default (ref.null $point))
  )

  (func $new_point (export "new_point")
        (param $x i32) (param $y i32) (result (ref $point))
    (struct.new $point (local.get $x) (local.get $y))
  )
)
