(module
  ;; Tests that exception handling works correctly when `return` is used
  ;; in the call chain between the thrower and the try_table/catch handler.

  (type $Obj (struct (field $value i32)))
  (tag $e (param (ref null $Obj)))

  ;; Function that throws
  (func $do_throw (param $val i32)
    (throw $e (struct.new $Obj (local.get $val)))
  )

  ;; Function that uses `return` instruction
  (func $func_with_return (param $val i32) (result i32)
    (return (local.get $val))
  )

  ;; Calls func_with_return, then throws
  (func $call_return_then_throw (param $val i32) (result i32)
    (drop (call $func_with_return (i32.const 0)))
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

  ;; Test 2: deeper call chain with return then throw
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
