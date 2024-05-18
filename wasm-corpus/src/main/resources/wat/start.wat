(module
  (import "env" "gotit" (func $gotit (param i32)))
  (func $start
    i32.const 42
    call $gotit
  )
  (start $start)
  (memory 1)
  (data (i32.const 16) "\00\01\02\03")
)