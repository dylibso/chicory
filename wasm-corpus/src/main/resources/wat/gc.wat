(module
  (type $point (struct (field $x i32) (field $y i32)))

  (func $new_point (export "new_point") (param $x i32) (param $y i32) (result (ref $point))
    (struct.new $point (local.get $x) (local.get $y))
  )

  (func $get_x (export "get_x") (param $p (ref null $point)) (result i32)
    (struct.get $point $x (local.get $p))
  )

  (func $sum (export "sum") (param $p (ref null $point)) (result i32)
    (i32.add
      (struct.get $point $x (local.get $p))
      (struct.get $point $y (local.get $p)))
  )
)
