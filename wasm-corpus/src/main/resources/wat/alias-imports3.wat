(module
  (type (;0;) (func (param i32)))
  (type (;1;) (func (param i64)))

  (import "env" "table" (table 1 funcref))
  (import "env" "table" (table 2 externref))

  (import "env" "global" (global i32))
  (import "env" "global" (global i64))

  (import "env" "tag" (tag (param i32)))
  (import "env" "tag" (tag (param i64)))
)
