(module
  (type (;0;) (func (param i64) (result i64)))
  (type (;1;) (func (param i64) (result i64 i64)))
  (type (;2;) (func (param i64 i64) (result i64 i64 i64)))
  (func (;0;) (type 1) (param i64) (result i64 i64)
    local.get 0
    local.get 0)
  (func (;1;) (type 2) (param i64 i64) (result i64 i64 i64)
    local.get 0
    local.get 1
    local.get 0)
  (func (;2;) (type 0) (param i64) (result i64)
    i64.const 1
    local.get 0
    loop (param i64 i64) (result i64)  ;; label = @1
      call 1
      call 1
      i64.mul
      call 1
      i64.const 1
      i64.sub
      call 0
      i64.const 0
      i64.gt_u
      br_if 0 (;@1;)
      drop
      return
    end)
  (export "fac-ssa" (func 2)))
