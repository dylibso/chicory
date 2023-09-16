(module
  (memory 1)
    (func (export "run") (param i32) (result i32)
      i32.const 0
      local.get 0
      i32.const 1312398398
      i32.add
      i32.store
      i32.const 0
      i32.load16_s
    )
)