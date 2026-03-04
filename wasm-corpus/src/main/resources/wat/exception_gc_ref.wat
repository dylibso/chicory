(module
  ;; Import a host function to call from catch handlers
  (import "host" "on_catch" (func $on_catch (param i32) (result i32)))

  ;; Function type for call_indirect: (i32) -> void (throws)
  (type $throw_fn_type (func (param i32)))

  ;; Struct type (represents a Java object like Throwable)
  (type $Obj (struct (field $value i32)))

  ;; Subtype hierarchy (matches GraalVM WebImage: NoSuchFileException extends ... extends Throwable)
  (type $Base (sub (struct (field $bval i32))))
  (type $Mid (sub $Base (struct (field $bval i32) (field $mval i32))))
  (type $Leaf (sub final $Mid (struct (field $bval i32) (field $mval i32) (field $lval i32))))

  ;; Exception tag with GC ref parameter (matches GraalVM WebImage pattern:
  ;;   (tag $tag0 (param (ref null $Throwable)))
  ;; )
  (tag $e (param (ref null $Obj)))

  ;; Exception tag with BASE type param — thrown value will be a subtype
  (tag $esub (param (ref null $Base)))

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

  ;; Test 5: try_table inside a loop - catch, call host, loop back and catch again
  ;; This is the common Java pattern: while (...) { try { ... } catch (Throwable t) { ... } }
  ;; Mirrors the javac init code that catches 4 exceptions in sequence
  (func $maybe_throw (param $counter i32) (result i32)
    ;; Throw if counter < 4, otherwise return 0
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
        ;; Try calling maybe_throw - it throws for i < 4
        (block $h (result (ref null $Obj))
          (try_table (catch $e $h)
            (drop (call $maybe_throw (local.get $i)))
            ;; No exception: exit the loop
            (br $exit)
          )
          (unreachable)
        )
        ;; Exception caught - extract value and call host
        (struct.get $Obj $value)
        (call $on_catch)
        (drop)
        ;; Increment caught count
        (local.set $caught (i32.add (local.get $caught) (i32.const 1)))
        ;; Increment loop counter
        (local.set $i (i32.add (local.get $i) (i32.const 1)))
        ;; Loop back
        (br $retry)
      )
    )
    ;; Return number of caught exceptions (should be 4)
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

  ;; --- GC subtype exception tests ---
  ;; In GraalVM WebImage: tag $tag0 has (param (ref null $_Throwable))
  ;; but the thrown value is $_NoSuchFileException (a subtype of $_Throwable).
  ;; The catch block result type is (ref null $_Throwable).

  ;; Helper: throws a $Leaf (subtype of $Base) using tag $esub (param $Base)
  (func $do_throw_leaf (param $val i32)
    (throw $esub (struct.new $Leaf (local.get $val) (i32.const 100) (i32.const 200)))
  )

  ;; Test 10: basic catch with subtype — throw Leaf, catch expects Base
  (func (export "subtype-catch-gc") (result i32)
    (block $h (result (ref null $Base))
      (try_table (catch $esub $h)
        (call $do_throw_leaf (i32.const 55))
      )
      (unreachable)
    )
    ;; caught: (ref null $Base) on stack — but runtime value is $Leaf
    (struct.get $Base $bval)
  )

  ;; Test 11: subtype throw from called function (mirrors javac pattern exactly)
  (func $inner_throw_leaf (param $val i32) (result i32)
    (call $do_throw_leaf (local.get $val))
    (i32.const 0)
  )

  (func (export "subtype-from-call-gc") (result i32)
    (block $h (result (ref null $Base))
      (try_table (catch $esub $h)
        (drop (call $inner_throw_leaf (i32.const 77)))
      )
      (unreachable)
    )
    (struct.get $Base $bval)
  )

  ;; Test 12: subtype throw from deep call chain (FileSystemView -> FileStore -> FileTree -> throw)
  (func $mid_throw_leaf (param $val i32) (result i32)
    (call $inner_throw_leaf (local.get $val))
  )

  (func (export "subtype-deep-call-gc") (result i32)
    (block $h (result (ref null $Base))
      (try_table (catch $esub $h)
        (drop (call $mid_throw_leaf (i32.const 33)))
      )
      (unreachable)
    )
    (struct.get $Base $bval)
  )

  ;; Test 13: subtype with sequential catches (first catch, then another)
  (func (export "subtype-sequential-gc") (result i32)
    (local $sum i32)

    (block $h1 (result (ref null $Base))
      (try_table (catch $esub $h1)
        (call $do_throw_leaf (i32.const 10))
      )
      (unreachable)
    )
    (struct.get $Base $bval)
    (call $on_catch)
    (local.set $sum)

    (block $h2 (result (ref null $Base))
      (try_table (catch $esub $h2)
        (call $do_throw_leaf (i32.const 20))
      )
      (unreachable)
    )
    (struct.get $Base $bval)
    (call $on_catch)
    (i32.add (local.get $sum))
  )

  ;; Test 14: subtype in loop with deep call (closest to javac pattern)
  (func $maybe_throw_leaf (param $counter i32) (result i32)
    (if (i32.lt_u (local.get $counter) (i32.const 4))
      (then (call $do_throw_leaf (local.get $counter)))
    )
    (i32.const 0)
  )
  (func $deep_maybe_throw_leaf (param $counter i32) (result i32)
    (call $maybe_throw_leaf (local.get $counter))
  )

  (func (export "subtype-loop-deep-gc") (result i32)
    (local $i i32)
    (local $caught i32)

    (local.set $i (i32.const 0))
    (local.set $caught (i32.const 0))

    (block $exit
      (loop $retry
        (block $h (result (ref null $Base))
          (try_table (catch $esub $h)
            (drop (call $deep_maybe_throw_leaf (local.get $i)))
            (br $exit)
          )
          (unreachable)
        )
        (struct.get $Base $bval)
        (call $on_catch)
        (drop)
        (local.set $caught (i32.add (local.get $caught) (i32.const 1)))
        (local.set $i (i32.add (local.get $i) (i32.const 1)))
        (br $retry)
      )
    )
    (local.get $caught)
  )

  ;; --- call_indirect tests ---
  ;; GraalVM WebImage uses call_indirect (virtual dispatch) to call throwException.
  ;; The throw function is in a table and called indirectly.

  ;; Table with throw function
  (table $ftable 1 funcref)
  (elem (table $ftable) (i32.const 0) func $do_throw)

  ;; Test 7: throw via call_indirect, catch with GC ref
  (func (export "indirect-catch-gc") (result i32)
    (block $h (result (ref null $Obj))
      (try_table (catch $e $h)
        (call_indirect $ftable (type $throw_fn_type) (i32.const 42) (i32.const 0))
      )
      (unreachable)
    )
    (struct.get $Obj $value)
  )

  ;; Test 8: sequential catches via call_indirect
  (func (export "indirect-sequential-gc") (result i32)
    (local $sum i32)

    (block $h1 (result (ref null $Obj))
      (try_table (catch $e $h1)
        (call_indirect $ftable (type $throw_fn_type) (i32.const 10) (i32.const 0))
      )
      (unreachable)
    )
    (struct.get $Obj $value)
    (call $on_catch)
    (local.set $sum)

    (block $h2 (result (ref null $Obj))
      (try_table (catch $e $h2)
        (call_indirect $ftable (type $throw_fn_type) (i32.const 20) (i32.const 0))
      )
      (unreachable)
    )
    (struct.get $Obj $value)
    (call $on_catch)
    (i32.add (local.get $sum))
  )

  ;; Test 9: loop with call_indirect throw (closest to javac pattern)
  (func $indirect_maybe_throw (param $counter i32) (result i32)
    (if (i32.lt_u (local.get $counter) (i32.const 4))
      (then
        (call_indirect $ftable (type $throw_fn_type) (local.get $counter) (i32.const 0))
      )
    )
    (i32.const 0)
  )

  (func (export "indirect-loop-gc") (result i32)
    (local $i i32)
    (local $caught i32)

    (local.set $i (i32.const 0))
    (local.set $caught (i32.const 0))

    (block $exit
      (loop $retry
        (block $h (result (ref null $Obj))
          (try_table (catch $e $h)
            (drop (call $indirect_maybe_throw (local.get $i)))
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
