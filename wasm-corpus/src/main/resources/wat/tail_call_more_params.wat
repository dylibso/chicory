(module
  (type (;0;) (func (result i64 i64)))
  (type (;1;) (func (param i64 i64 i64 i64 i64 i64 i64 i64 i64) (result i64 i64)))
  (func (;0;) (type 0) (result i64 i64)
    call 1)
  (func (;1;) (type 0) (result i64 i64)
    i64.const 1
    i64.const 2
    i64.const 3
    i64.const 4
    i64.const 5
    i64.const 6
    i64.const 7
    i64.const 8
    i64.const 9
    return_call 2)
  (func (;2;) (type 1) (param i64 i64 i64 i64 i64 i64 i64 i64 i64) (result i64 i64)
    local.get 0
    local.get 1
    i64.add
    local.get 2
    i64.add
    local.get 3
    i64.add
    local.get 4
    local.get 5
    i64.add
    local.get 6
    i64.add
    local.get 7
    i64.add
    local.get 8
    i64.add)
  (export "f" (func 0)))
