(module
  (type (;0;) (func (param i32 i32 i32) (result i32)))
  (func (;0;) (type 0) (param i32 i32 i32) (result i32)
    local.get 0
    i32.eqz
    if (result i32)  ;; label = @1
      local.get 1
    else
      local.get 0
      i32.const 1
      i32.eq
      if (result i32)  ;; label = @2
        local.get 2
      else
        local.get 0
        i32.const 1
        i32.sub
        local.get 2
        local.get 1
        local.get 2
        i32.add
        return_call 0
      end
    end)
  (export "f" (func 0)))
