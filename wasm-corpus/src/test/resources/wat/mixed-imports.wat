(module
  (type (;0;) (func (param i32) (result f64)))
  (type (;1;) (func (param f64) (result f64)))
  (type (;2;) (func (param i32 f64)))
  (type (;3;) (func (param f64 f64) (result f64)))
  (type (;4;) (func))

  (import "env" "memory" (memory 1))
  (import "env" "cbrt" (func $cbrt (type 0)))
  (import "env" "log" (func $log (type 2)))

  (func $pow2 (type 1) (param f64) (result f64)
    local.get 0
    local.get 0
    f64.mul
  )
  (func $add (type 3) (param f64 f64) (result f64)
    local.get 0
    local.get 1
    f64.add
  )
  (func $main (type 4)
    i32.const 1
    i32.const 512
    call $cbrt
    call $pow2
    f64.const 100.0
    call $add
    call $log
  )

  (export "pow2" (func $pow2))
  (export "main" (func $main))
)

