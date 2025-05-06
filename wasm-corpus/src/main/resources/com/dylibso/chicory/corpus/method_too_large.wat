(module
  (type (;0;) (func (param i32) (result i32)))
  (import "funcs" "host_func" (func $host_func (type 0)))

  (table 1 funcref)
  (elem (i32.const 0) $func_0)

  (func $func_0 (export "func_0") (param i32) (result i32)
    local.get 0
    call $host_func
  )
  (func $func_1 (export "func_1") (param i32) (result i32)
    local.get 0

    #foreach ($instr in $instructions)
    i32.const 1
    i32.add
    i32.const 1
    i32.sub
    #end

    i32.const 0
    i32.eq
    if (result i32)
      local.get 0
      call $func_0
    else
      local.get 0
      i32.const 0
      call_indirect (type 0)
    end
  )

  (func $func_2 (export "func_2") (param i32) (result i32)
    local.get 0
    call $func_1
  )
)
