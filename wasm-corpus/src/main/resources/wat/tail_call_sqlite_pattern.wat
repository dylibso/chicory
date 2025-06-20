(module
  (type (;0;) (func (param i32 i32 i32 i32 i32 i32 i32) (result i32)))
  (type (;1;) (func (param i32 i32 i32 i32 i32 i32 i32 i32) (result i32)))
  (func (;0;) (type 0) (param i32 i32 i32 i32 i32 i32 i32) (result i32)
    local.get 0
    local.get 1
    local.get 2
    local.get 3
    local.get 4
    local.get 5
    local.get 6
    call 1)
  (func (;1;) (type 0) (param i32 i32 i32 i32 i32 i32 i32) (result i32)
    local.get 0
    local.get 1
    local.get 2
    local.get 3
    i32.const 31
    i32.and
    i32.const 128
    i32.or
    i32.const 0
    local.get 4
    local.get 5
    local.get 6
    return_call 2)
  (func (;2;) (type 1) (param i32 i32 i32 i32 i32 i32 i32 i32) (result i32)
    local.get 0
    local.get 1
    i32.add
    local.get 2
    i32.add
    local.get 3
    i32.add
    local.get 4
    i32.add
    local.get 5
    i32.add
    local.get 6
    i32.add
    local.get 7
    i32.add)
  (export "f" (func 0)))
