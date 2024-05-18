(module
  (type $t0 (func (param i32) (result i32)))

  (func $innerFunc (type $t0) (param $arg0 i32) (result i32)
    (local $resultCode i32)
    block $outerBlock
      i32.const -1 ;; These values must be dropped from the stack
      i32.const -2 ;; when the br_if jump is triggered

      i32.const -3
      if (result i32)
        local.get $arg0
        local.tee $resultCode
        br_if $outerBlock
        i32.const 100
      else
        i32.const 200
      end
      local.set $resultCode

      drop
      drop
    end
    local.get $resultCode)

  (func $main (export "main") (type $t0) (param $arg0 i32) (result i32)
    i32.const 0
    local.get $arg0
    call $innerFunc
    i32.add)

  (memory $M0 1))
