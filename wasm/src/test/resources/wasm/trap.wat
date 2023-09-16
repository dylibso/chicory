(module
  (func $trap
    unreachable
  )
  (func $innerFunc
    call $trap
  )
  (func $start
    call $innerFunc
  )
  (start $start)
)
