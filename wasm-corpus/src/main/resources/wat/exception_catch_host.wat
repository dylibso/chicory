(module
  ;; Import a host function to call from catch handlers
  (import "host" "on_catch" (func $on_catch (param i32) (result i32)))

  ;; Exception tag with i32 payload
  (tag $e (param i32))

  ;; Helper that always throws
  (func $do_throw (param i32)
    (throw $e (local.get 0))
  )

  ;; Test 1: basic catch (no host interaction)
  (func (export "basic-catch") (result i32)
    (block $h (result i32)
      (try_table (result i32) (catch $e $h)
        (call $do_throw (i32.const 42))
        (i32.const 0)
      )
      (return)
    )
    ;; caught: i32=42 on stack
  )

  ;; Test 2: catch then call host function
  (func (export "catch-call-host") (result i32)
    (block $h (result i32)
      (try_table (result i32) (catch $e $h)
        (call $do_throw (i32.const 7))
        (i32.const 0)
      )
      (return)
    )
    ;; caught: i32=7 on stack
    (call $on_catch)
  )

  ;; Test 3: catch, call host, then catch another exception
  ;; This is the pattern from javac-in-wasm that fails in Chicory
  (func (export "sequential-catches") (result i32)
    (local $sum i32)

    ;; First exception
    (block $h1 (result i32)
      (try_table (result i32) (catch $e $h1)
        (call $do_throw (i32.const 10))
        (i32.const 0)
      )
      (return (i32.const -1))
    )
    ;; caught: i32=10 on stack
    (call $on_catch)
    (local.set $sum)

    ;; Second exception
    (block $h2 (result i32)
      (try_table (result i32) (catch $e $h2)
        (call $do_throw (i32.const 20))
        (i32.const 0)
      )
      (return (i32.const -2))
    )
    ;; caught: i32=20 on stack
    (call $on_catch)
    (i32.add (local.get $sum))
  )

  ;; Test 4: nested try_table - inner try doesn't catch, outer does
  (func (export "nested-catch") (result i32)
    (block $outer (result i32)
      (try_table (result i32) (catch $e $outer)
        ;; inner try_table without a catch for $e
        (try_table (result i32)
          (call $do_throw (i32.const 99))
          (i32.const 0)
        )
      )
      (return)
    )
    ;; outer caught: i32=99
    (call $on_catch)
  )
)
