(module
  (import "env" "get_host_object" (func $get_host_object (result externref)))

  (import "env" "is_null" (func $is_null (param $input externref) (result i32)))
  
  (func $process_externref (param $input externref) (result externref)
    local.get $input
  )
  
  (export "process_externref" (func $process_externref))
  (export "is_null" (func $is_null))
  (export "get_host_object" (func $get_host_object))
)
