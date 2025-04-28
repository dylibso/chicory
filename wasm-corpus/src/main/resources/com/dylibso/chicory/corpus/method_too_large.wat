(module
  (type (;0;) (func (param i32) (result i32)))
  (import "funcs" "host_func" (func $host_func (type 0)))

  (func $func_0 (export "func_0") (param i32) (result i32)
    local.get 0
    i32.const 0
    i32.add

    call $host_func
  )
  (func $func_1 (export "func_1") (param i32) (result i32)
    local.get 0
    i32.const 0
    i32.add

    #foreach ($instr in $instructions)
    i32.const 1
    i32.add
    i32.const 1
    i32.sub
    #end

    call $func_0
  )
  (func $func_2 (export "func_2") (param i32) (result i32)
    local.get 0
    i32.const 0
    i32.add
    call $func_1
  )
)
