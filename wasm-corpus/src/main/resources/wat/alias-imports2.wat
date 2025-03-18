(module
  (type (;0;) (func (param i32)))
  (type (;1;) (func (param i64)))
  (type (;2;) (func))

  (import "env" "log" (func $log (type 0)))
  (import "env" "log" (func $log-alias (type 1)))

  (export "log" (func $log))
  (export "log-alias" (func $log-alias))
)
