(module
  ;; shared memory is required for atomic instructions
  (memory 1 1 shared)

  (func $fence_example
    ;; atomic store: write 42 to address 0
    i32.const 0        ;; address
    i32.const 42       ;; value
    i32.atomic.store align=4

    ;; memory fence
    atomic.fence

    ;; atomic load: read back the value at address 0
    i32.const 0
    i32.atomic.load align=4
    drop
  )

  (export "fence_example" (func $fence_example))
)
