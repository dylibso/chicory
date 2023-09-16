(module
  (memory 1)
  (func (export "run32") (param i32) (result i32)
   i32.const 0
   local.get 0
   i32.store  ;; store param0 at address 0
   i32.const 0
   i32.load
   return
  )
  (func (export "run64") (param i64) (result i64)
   i32.const 0
   local.get 0
   i64.store  ;; store param0 at address 0
   i32.const 0
   i64.load
   return
  )
)
