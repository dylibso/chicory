(module
  ;; Mirrors the exact GraalVM WebImage exception creation + throw + catch pattern:
  ;; 1. Create exception struct
  ;; 2. Call host function (genBacktrace) → returns externref
  ;; 3. any.convert_extern + ref.cast to wrap externref → anyref
  ;; 4. Store in struct field
  ;; 5. throw $tag
  ;; 6. try_table/catch should catch it

  ;; Host function that returns an externref (like genBacktrace)
  (import "host" "get_extern" (func $get_extern (result externref)))

  ;; Type hierarchy: Base > Mid > Leaf (like Throwable > IOException > NoSuchFileException)
  (type $Base (sub (struct (field $bval (mut i32)) (field $backtrace (mut (ref null $Base))))))
  (type $Mid (sub $Base (struct (field $bval (mut i32)) (field $backtrace (mut (ref null $Base))) (field $mval (mut i32)))))
  (type $Leaf (sub final $Mid (struct (field $bval (mut i32)) (field $backtrace (mut (ref null $Base))) (field $mval (mut i32)) (field $lval (mut i32)))))

  ;; WasmExtern wrapper (like GraalVM's $_WasmExtern)
  (type $WasmExtern (sub $Base (struct (field $bval (mut i32)) (field $backtrace (mut (ref null $Base))) (field $extern_val externref))))

  ;; Tag with base type parameter
  (tag $e (param (ref null $Base)))

  ;; extern.wrap: converts externref → (ref null $Base), mirrors GraalVM's func.extern.wrap
  (func $extern_wrap (param $p0 externref) (result (ref null $Base))
    local.get $p0
    ref.is_null
    if
      ref.null none
      return
    end
    ;; Try converting to internal ref first
    local.get $p0
    any.convert_extern
    ref.test (ref null $Base)
    if
      local.get $p0
      any.convert_extern
      ref.cast (ref null $Base)
      return
    end
    ;; Wrap as WasmExtern
    (struct.new $WasmExtern (i32.const 0) (ref.null none) (local.get $p0))
  )

  ;; Create exception struct, call host, wrap extern, store, throw
  ;; This mirrors the exact pattern in javac's exception creation sites
  (func $create_and_throw (param $val i32)
    (local $exc (ref null $Leaf))
    ;; Create Leaf exception struct
    (struct.new $Leaf (local.get $val) (ref.null none) (i32.const 100) (i32.const 200))
    (local.set $exc)
    ;; Call host function (genBacktrace)
    (call $get_extern)
    ;; Wrap externref → internal ref via any.convert_extern
    (call $extern_wrap)
    ;; Store wrapped result in exception struct's backtrace field
    drop
    ;; Throw the exception
    (local.get $exc)
    (throw $e)
  )

  ;; Inner function that calls the throw function (like FileTree.lookUp)
  (func $inner (param $val i32) (result i32)
    (call $create_and_throw (local.get $val))
    (i32.const 0)
  )

  ;; Middle function (like JimfsFileStore.lookUp) - no try_table
  (func $middle (param $val i32) (result i32)
    (call $inner (local.get $val))
  )

  ;; Test 1: basic extern-wrap + throw + catch
  (func (export "extern-wrap-catch") (result i32)
    (block $h (result (ref null $Base))
      (try_table (catch $e $h)
        (call $create_and_throw (i32.const 42))
      )
      (unreachable)
    )
    (struct.get $Base $bval)
  )

  ;; Test 2: extern-wrap + throw from called function + catch
  (func (export "extern-wrap-call-catch") (result i32)
    (block $h (result (ref null $Base))
      (try_table (catch $e $h)
        (drop (call $inner (i32.const 77)))
      )
      (unreachable)
    )
    (struct.get $Base $bval)
  )

  ;; Test 3: extern-wrap + throw from deep call chain + catch
  ;; Mirrors: FileSystemView.lookUpWithLock → FileStore.lookUp → FileTree.lookUp → throw
  (func (export "extern-wrap-deep-catch") (result i32)
    (block $h (result (ref null $Base))
      (try_table (catch $e $h)
        (drop (call $middle (i32.const 33)))
      )
      (unreachable)
    )
    (struct.get $Base $bval)
  )

  ;; Test 4: matches javac's exact block nesting pattern
  ;; block @6
  ;;   block (result (ref null $Base)) @7
  ;;     try_table (catch $e 0) @8        -- catch depth 0 = @7
  ;;       call $middle
  ;;       local.set $result
  ;;       br 2                           -- skip to @6
  ;;     end
  ;;     unreachable
  ;;   end
  ;;   local.set $scratch                 -- catch handler
  ;;   nop
  ;;   local.get $scratch
  ;;   local.set $exc
  ;;   br 2                               -- branch out of catch handler
  ;; end
  (func (export "extern-wrap-javac-pattern") (result i32)
    (local $result i32)
    (local $scratch (ref null $Base))
    (local $exc (ref null $Base))
    (block $outer                           ;; @1
      (block $normal                        ;; @2
        (block $catch_dest (result (ref null $Base))  ;; @3
          (try_table (catch $e $catch_dest)  ;; @4
            (local.set $result (call $middle (i32.const 99)))
            (br $normal)
          )
          (unreachable)
        )
        ;; catch handler: exception ref on stack
        (local.set $scratch)
        (nop)
        (local.get $scratch)
        (local.set $exc)
        ;; store value and branch out
        (local.get $exc)
        (struct.get $Base $bval)
        (local.set $result)
        (br $outer)
      )
      ;; normal path: result is from call (but call always throws, so shouldn't reach here)
      (unreachable)
    )
    (local.get $result)
  )

  ;; Test 5: sequential extern-wrap throws (like javac's multiple genBacktrace calls)
  (func (export "extern-wrap-sequential") (result i32)
    (local $sum i32)

    (block $h1 (result (ref null $Base))
      (try_table (catch $e $h1)
        (call $create_and_throw (i32.const 10))
      )
      (unreachable)
    )
    (struct.get $Base $bval)
    (local.set $sum)

    (block $h2 (result (ref null $Base))
      (try_table (catch $e $h2)
        (call $create_and_throw (i32.const 20))
      )
      (unreachable)
    )
    (struct.get $Base $bval)
    (i32.add (local.get $sum))
  )
)
