(module
  ;; Import a host function to call from catch handlers
  (import "host" "on_catch" (func $on_catch (param i32) (result i32)))

  ;; Struct type for exception payload
  (type $Obj (struct (field $value i32)))

  ;; Exception tag with GC ref parameter
  (tag $e (param (ref null $Obj)))

  ;; Helper that always throws with a new struct
  (func $do_throw (param $val i32)
    (throw $e (struct.new $Obj (local.get $val)))
  )

  ;; Test 1: basic catch with GC ref payload
  (func (export "basic-catch-gc") (result i32)
    (block $h (result (ref null $Obj))
      (try_table (catch $e $h)
        (call $do_throw (i32.const 42))
      )
      (unreachable)
    )
    (struct.get $Obj $value)
  )

  ;; Test 2: sequential catches with GC ref
  (func (export "sequential-catches-gc") (result i32)
    (local $sum i32)

    (block $h1 (result (ref null $Obj))
      (try_table (catch $e $h1)
        (call $do_throw (i32.const 10))
      )
      (unreachable)
    )
    (struct.get $Obj $value)
    (call $on_catch)
    (local.set $sum)

    (block $h2 (result (ref null $Obj))
      (try_table (catch $e $h2)
        (call $do_throw (i32.const 20))
      )
      (unreachable)
    )
    (struct.get $Obj $value)
    (call $on_catch)
    (i32.add (local.get $sum))
  )

  ;; Test 3: exception thrown from a called function
  (func $inner_throw (param $val i32) (result i32)
    (call $do_throw (local.get $val))
    (i32.const 0)
  )

  (func (export "catch-from-call-gc") (result i32)
    (local $sum i32)

    (block $h1 (result (ref null $Obj))
      (try_table (catch $e $h1)
        (drop (call $inner_throw (i32.const 10)))
      )
      (unreachable)
    )
    (struct.get $Obj $value)
    (call $on_catch)
    (local.set $sum)

    (block $h2 (result (ref null $Obj))
      (try_table (catch $e $h2)
        (drop (call $inner_throw (i32.const 20)))
      )
      (unreachable)
    )
    (struct.get $Obj $value)
    (call $on_catch)
    (i32.add (local.get $sum))
  )

  ;; Test 4: deeply nested throw (function calls function that throws)
  (func $middle (param $val i32) (result i32)
    (call $inner_throw (local.get $val))
  )

  (func (export "deep-catch-gc") (result i32)
    (local $sum i32)

    (block $h1 (result (ref null $Obj))
      (try_table (catch $e $h1)
        (drop (call $middle (i32.const 10)))
      )
      (unreachable)
    )
    (struct.get $Obj $value)
    (call $on_catch)
    (local.set $sum)

    (block $h2 (result (ref null $Obj))
      (try_table (catch $e $h2)
        (drop (call $middle (i32.const 20)))
      )
      (unreachable)
    )
    (struct.get $Obj $value)
    (call $on_catch)
    (i32.add (local.get $sum))
  )

  ;; Test 5: try_table inside a loop — catch, call host, loop back and catch again
  (func $maybe_throw (param $counter i32) (result i32)
    (if (i32.lt_u (local.get $counter) (i32.const 4))
      (then (call $do_throw (local.get $counter)))
    )
    (i32.const 0)
  )

  (func (export "catch-in-loop-gc") (result i32)
    (local $i i32)
    (local $caught i32)

    (local.set $i (i32.const 0))
    (local.set $caught (i32.const 0))

    (block $exit
      (loop $retry
        (block $h (result (ref null $Obj))
          (try_table (catch $e $h)
            (drop (call $maybe_throw (local.get $i)))
            (br $exit)
          )
          (unreachable)
        )
        (struct.get $Obj $value)
        (call $on_catch)
        (drop)
        (local.set $caught (i32.add (local.get $caught) (i32.const 1)))
        (local.set $i (i32.add (local.get $i) (i32.const 1)))
        (br $retry)
      )
    )
    (local.get $caught)
  )

  ;; Test 6: try_table in loop with throw from deep call chain
  (func $deep_maybe_throw (param $counter i32) (result i32)
    (call $maybe_throw (local.get $counter))
  )

  (func (export "deep-catch-in-loop-gc") (result i32)
    (local $i i32)
    (local $caught i32)

    (local.set $i (i32.const 0))
    (local.set $caught (i32.const 0))

    (block $exit
      (loop $retry
        (block $h (result (ref null $Obj))
          (try_table (catch $e $h)
            (drop (call $deep_maybe_throw (local.get $i)))
            (br $exit)
          )
          (unreachable)
        )
        (struct.get $Obj $value)
        (call $on_catch)
        (drop)
        (local.set $caught (i32.add (local.get $caught) (i32.const 1)))
        (local.set $i (i32.add (local.get $i) (i32.const 1)))
        (br $retry)
      )
    )
    (local.get $caught)
  )
)
