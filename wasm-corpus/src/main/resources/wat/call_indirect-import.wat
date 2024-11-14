;; Import the shared table and reference the functions in this module.
(module
  (import "test" "shared-table" (table 3 funcref))
  (func $other (result i32)
    i32.const 88)
  (func $otherFail (result i32)
    unreachable)
  (elem (i32.const 1) func $other)
  (elem (i32.const 2) func $otherFail))
