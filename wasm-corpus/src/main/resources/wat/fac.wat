(module
  (type (;0;) (func (param i64) (result i64)))
  (type (;1;) (func (param i64) (result i64 i64)))
  (type (;2;) (func (param i64 i64) (result i64 i64 i64)))
  (type (;3;) (func (param i64 i64) (result i64)))
  (func (;0;) (type 0) (param i64) (result i64)
    local.get 0
    i64.const 0
    i64.eq
    if (result i64)  ;; label = @1
      i64.const 1
    else
      local.get 0
      local.get 0
      i64.const 1
      i64.sub
      call 0
      i64.mul
    end)
  (func (;1;) (type 0) (param i64) (result i64)
    local.get 0
    i64.const 0
    i64.eq
    if (result i64)  ;; label = @1
      i64.const 1
    else
      local.get 0
      local.get 0
      i64.const 1
      i64.sub
      call 1
      i64.mul
    end)
  (func (;2;) (type 0) (param i64) (result i64)
    (local i64 i64)
    local.get 0
    local.set 1
    i64.const 1
    local.set 2
    block  ;; label = @1
      loop  ;; label = @2
        local.get 1
        i64.const 0
        i64.eq
        if  ;; label = @3
          br 2 (;@1;)
        else
          local.get 1
          local.get 2
          i64.mul
          local.set 2
          local.get 1
          i64.const 1
          i64.sub
          local.set 1
        end
        br 0 (;@2;)
      end
    end
    local.get 2)
  (func (;3;) (type 0) (param i64) (result i64)
    (local i64 i64)
    local.get 0
    local.set 1
    i64.const 1
    local.set 2
    block  ;; label = @1
      loop  ;; label = @2
        local.get 1
        i64.const 0
        i64.eq
        if  ;; label = @3
          br 2 (;@1;)
        else
          local.get 1
          local.get 2
          i64.mul
          local.set 2
          local.get 1
          i64.const 1
          i64.sub
          local.set 1
        end
        br 0 (;@2;)
      end
    end
    local.get 2)
  (func (;4;) (type 0) (param i64) (result i64)
    (local i64)
    i64.const 1
    local.set 1
    block  ;; label = @1
      local.get 0
      i64.const 2
      i64.lt_s
      br_if 0 (;@1;)
      loop  ;; label = @2
        local.get 1
        local.get 0
        i64.mul
        local.set 1
        local.get 0
        i64.const -1
        i64.add
        local.set 0
        local.get 0
        i64.const 1
        i64.gt_s
        br_if 0 (;@2;)
      end
    end
    local.get 1)
  (func (;5;) (type 1) (param i64) (result i64 i64)
    local.get 0
    local.get 0)
  (func (;6;) (type 2) (param i64 i64) (result i64 i64 i64)
    local.get 0
    local.get 1
    local.get 0)
  (func (;7;) (type 0) (param i64) (result i64)
    i64.const 1
    local.get 0
    loop (param i64 i64) (result i64)  ;; label = @1
      call 6
      call 6
      i64.mul
      call 6
      i64.const 1
      i64.sub
      call 5
      i64.const 0
      i64.gt_u
      br_if 0 (;@1;)
      drop
      return
    end)
  (export "fac-rec" (func 0))
  (export "fac-rec-named" (func 1))
  (export "fac-iter" (func 2))
  (export "fac-iter-named" (func 3))
  (export "fac-opt" (func 4))
  (export "fac-ssa" (func 7)))
