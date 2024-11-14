;; The shared table has slots for functions:
;; - 0: $self (this module)
;; - 1: $other (another module)
;; - 2: $otherFail (another module)
;; The call-* functions each call one of the above slots.
(module
  (type $x (func (result i32)))
  (func $self (result i32)
    i32.const 42)
  (func $callSelf (result i32)
    i32.const 0
    call_indirect (type $x))
  (func $callOther (result i32)
    i32.const 1
    call_indirect (type $x))
  (func $callOtherFail (result i32)
    i32.const 2
    call_indirect (type $x))
  (table 5 funcref)
  (export "shared-table" (table 0))
  (export "call-self" (func $callSelf))
  (export "call-other" (func $callOther))
  (export "call-other-fail" (func $callOtherFail))
  (elem (i32.const 0) func $self))
