(module
  ;; Mutable global counter starting at 0
  (global $counter (mut i32) (i32.const 0))
  
  ;; Function to set the counter value
  (func $set (export "set") (param i32)
    local.get 0
    global.set $counter
  )
  
  ;; Function to get the counter value
  (func $get (export "get") (result i32)
    global.get $counter
  )
  
  ;; Function to increment the counter and return new value
  (func $increment (export "increment") (result i32)
    global.get $counter
    i32.const 1
    i32.add
    global.set $counter
    global.get $counter
  )
  
  ;; Function to decrement the counter and return new value
  (func $decrement (export "decrement") (result i32)
    global.get $counter
    i32.const 1
    i32.sub
    global.set $counter
    global.get $counter
  )
  
  ;; Function to add a value to the counter and return new value
  (func $add (export "add") (param i32) (result i32)
    global.get $counter
    local.get 0
    i32.add
    global.set $counter
    global.get $counter
  )
) 