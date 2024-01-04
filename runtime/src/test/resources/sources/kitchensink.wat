(module
  (memory 1)
  (func (export "run") (param i32) (result i32)
    local.get 0
    i32.const 10
    i32.add
    i32.const 1
    i32.sub
    i32.const 6
    i32.mul
    i32.const 2
    i32.div_s
    i32.const 40
    i32.and
    i32.const 33
    i32.or
    i32.const 22
    i32.xor
    ;; 55

    i32.const 2
    i32.shl

    i32.const 3
    i32.shr_s

    i32.const 4
    i32.rotl

    i32.const 6
    i32.rotr

    i32.extend8_s
  )
)