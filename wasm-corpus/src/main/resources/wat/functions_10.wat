(module
  (func $func_1 (export "func_1") (param i32) (result i32)
    local.get 0
    i32.const 1
    i32.add

    
    )
  (func $func_2 (export "func_2") (param i32) (result i32)
    local.get 0
    i32.const 2
    i32.add

    
        call $func_1
    )
  (func $func_3 (export "func_3") (param i32) (result i32)
    local.get 0
    i32.const 3
    i32.add

    
        call $func_2
    )
  (func $func_4 (export "func_4") (param i32) (result i32)
    local.get 0
    i32.const 4
    i32.add

    
        call $func_3
    )
  (func $func_5 (export "func_5") (param i32) (result i32)
    local.get 0
    i32.const 5
    i32.add

    
        call $func_4
    )
  (func $func_6 (export "func_6") (param i32) (result i32)
    local.get 0
    i32.const 6
    i32.add

    
        call $func_5
    )
  (func $func_7 (export "func_7") (param i32) (result i32)
    local.get 0
    i32.const 7
    i32.add

    
        call $func_6
    )
  (func $func_8 (export "func_8") (param i32) (result i32)
    local.get 0
    i32.const 8
    i32.add

    
        call $func_7
    )
  (func $func_9 (export "func_9") (param i32) (result i32)
    local.get 0
    i32.const 9
    i32.add

    
        call $func_8
    )
  (func $func_10 (export "func_10") (param i32) (result i32)
    local.get 0
    i32.const 10
    i32.add

    
        call $func_9
    )
)

