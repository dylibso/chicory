(module
  (type (;0;) (func (param i64 i64) (result i64)))
  (func (;0;) (type 0) (param i64 i64) (result i64)
    local.get 0
    i64.eqz
    if (result i64)  ;; label = @1
      local.get 1
    else
      local.get 0
      i64.const 1
      i64.sub
      local.get 1
      i64.const 1
      i64.add
      return_call 0
    end)
  (export "f" (func 0)))
