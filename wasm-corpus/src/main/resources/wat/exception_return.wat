(module
  ;; Reproducer for: callStack.clear() on RETURN wipes the entire call stack.
  ;; The bug manifests when:
  ;; 1. Function A has try_table/catch
  ;; 2. A calls B, B calls C, C uses `return` → callStack.clear() wipes ALL frames
  ;; 3. C returns normally (but callStack is now empty)
  ;; 4. B continues, calls D which throws
  ;; 5. The throw can't find A's handler because callStack was cleared

  (type $Obj (struct (field $value i32)))
  (tag $e (param (ref null $Obj)))

  ;; Function that throws
  (func $do_throw (param $val i32)
    (throw $e (struct.new $Obj (local.get $val)))
  )

  ;; Function that uses `return` instruction (clears callStack in buggy interpreter)
  (func $func_with_return (param $val i32) (result i32)
    (return (local.get $val))
  )

  ;; Function that first calls func_with_return (which clears callStack),
  ;; then calls do_throw
  (func $call_return_then_throw (param $val i32) (result i32)
    ;; This call completes normally, but `return` inside clears the callStack
    (drop (call $func_with_return (i32.const 0)))
    ;; Now throw — but the callStack has been wiped
    (call $do_throw (local.get $val))
    (i32.const 0)
  )

  ;; Test 1: catch exception after a prior function used `return`
  (func (export "catch-after-return") (result i32)
    (block $h (result (ref null $Obj))
      (try_table (catch $e $h)
        (drop (call $call_return_then_throw (i32.const 42)))
      )
      (unreachable)
    )
    (struct.get $Obj $value)
  )

  ;; Test 2: deeper chain - A calls B, B calls C (return), B calls D (throw)
  (func $deep_return_then_throw (param $val i32) (result i32)
    (drop (call $func_with_return (i32.const 0)))
    (call $do_throw (local.get $val))
    (i32.const 0)
  )
  (func $wrapper (param $val i32) (result i32)
    (call $deep_return_then_throw (local.get $val))
  )

  (func (export "catch-deep-after-return") (result i32)
    (block $h (result (ref null $Obj))
      (try_table (catch $e $h)
        (drop (call $wrapper (i32.const 77)))
      )
      (unreachable)
    )
    (struct.get $Obj $value)
  )

  ;; Test 3: multiple returns before throw
  (func $multi_return_then_throw (param $val i32) (result i32)
    (drop (call $func_with_return (i32.const 1)))
    (drop (call $func_with_return (i32.const 2)))
    (drop (call $func_with_return (i32.const 3)))
    (call $do_throw (local.get $val))
    (i32.const 0)
  )

  (func (export "catch-multi-return") (result i32)
    (block $h (result (ref null $Obj))
      (try_table (catch $e $h)
        (drop (call $multi_return_then_throw (i32.const 33)))
      )
      (unreachable)
    )
    (struct.get $Obj $value)
  )

  ;; Test 4: sequential catches with returns in between
  (func (export "catch-sequential-with-return") (result i32)
    (local $sum i32)

    (block $h1 (result (ref null $Obj))
      (try_table (catch $e $h1)
        (drop (call $call_return_then_throw (i32.const 10)))
      )
      (unreachable)
    )
    (struct.get $Obj $value)
    (local.set $sum)

    (block $h2 (result (ref null $Obj))
      (try_table (catch $e $h2)
        (drop (call $call_return_then_throw (i32.const 20)))
      )
      (unreachable)
    )
    (struct.get $Obj $value)
    (i32.add (local.get $sum))
  )
)
