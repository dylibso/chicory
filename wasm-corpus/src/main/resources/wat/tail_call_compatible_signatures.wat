(module
  (type (;0;) (func (param i32 i32) (result i32)))
  (type (;1;) (func (param i32 i32) (result i32)))
  (type (;2;) (func (param i32 i32 i32) (result i32)))
  (type (;3;) (func (param i32 i32 i32 i32) (result i32)))
  (func (;0;) (type 3) (param i32 i32 i32 i32) (result i32)
    local.get 0
    local.get 1
    local.get 2
    local.get 3
    i32.add
    call 1)
  (func (;1;) (type 2) (param i32 i32 i32) (result i32)
    local.get 0
    local.get 2
    i32.add
    local.get 1
    return_call 2)
  (func (;2;) (type 1) (param i32 i32) (result i32)
    local.get 0
    local.get 1
    call 3)
  (func (;3;) (type 0) (param i32 i32) (result i32)
    local.get 0
    local.get 1
    i32.mul)
  (export "f" (func 0)))
