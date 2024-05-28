(module
(func (export "add") (param $x i32) (param $y i32) (result i32)
    (i32.add
        (local.get $x)
        (local.get $y))
    )
)
