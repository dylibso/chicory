(module
  ;; Import a host function to call from catch handlers
  (import "host" "on_catch" (func $on_catch (param i32) (result i32)))

  ;; Struct type (represents a Java object like Throwable)
  (type $Obj (struct (field $value i32)))

  ;; Exception tag with GC ref parameter (matches GraalVM WebImage pattern:
  ;;   (tag $tag0 (param (ref null $Throwable)))
  ;; )
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
    ;; caught: (ref null $Obj) on stack
    (struct.get $Obj $value)
  )

  ;; Test 2: catch with GC ref, call host, then catch again
  (func (export "sequential-catches-gc") (result i32)
    (local $sum i32)

    ;; First exception
    (block $h1 (result (ref null $Obj))
      (try_table (catch $e $h1)
        (call $do_throw (i32.const 10))
      )
      (unreachable)
    )
    (struct.get $Obj $value)
    (call $on_catch)
    (local.set $sum)

    ;; Second exception
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

  ;; Test 3: exception thrown from a called function (not directly in try body)
  ;; Mirrors: try { someJavaCode() } catch (Throwable t) { genBacktrace(); }
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
)
