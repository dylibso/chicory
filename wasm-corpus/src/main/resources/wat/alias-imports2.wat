(module
  (type (;0;) (func (param i32)))
  (type (;1;) (func (param i64)))

  (import "env" "log" (func $log-i32 (type 0)))
  (import "env" "log" (func $log-i64 (type 1)))

  (export "log-i32" (func $log-i32))
  (export "log-i64" (func $log-i64))
)
