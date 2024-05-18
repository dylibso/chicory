(module
  ;; Import console.log. Takes a ptr and a length to the string in memory
  (import "console" "log" (func $log (param i32) (param i32)))
  (func (export "logIt")
    (local $var i32)
    ;; some random nops and drop, not important
    nop
    nop
    i32.const 1
    drop
    ;; start implementation
    (local.set $var (i32.const 10))
    ;; call console.log("Hello, World!") 10 times
    (loop
      i32.const 13
      i32.const 0
      call $log
      local.get $var
      i32.const 1
      i32.sub
      local.tee $var
      br_if 0
    )
  )

  (memory 1)
  (data $.rodata (i32.const 0) "Hello, World!\00")
)

