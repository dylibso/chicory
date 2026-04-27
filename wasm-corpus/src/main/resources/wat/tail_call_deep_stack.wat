(module
  (type $counter (func (param i64 i64) (result i64)))
  (table funcref (elem $even $odd))
  (func $even (type $counter) (param $n i64) (param $acc i64) (result i64)
    local.get $n
    i64.eqz
    if (result i64)
      local.get $acc
    else
      local.get $n
      i64.const 1
      i64.sub
      local.get $acc
      i64.const 1
      i64.add
      i32.const 1
      return_call_indirect (type $counter)
    end)
  (func $odd (type $counter) (param $n i64) (param $acc i64) (result i64)
    local.get $n
    i64.eqz
    if (result i64)
      local.get $acc
    else
      local.get $n
      i64.const 1
      i64.sub
      local.get $acc
      i64.const 1
      i64.add
      i32.const 0
      return_call_indirect (type $counter)
    end)
  (export "run" (func $even)))
