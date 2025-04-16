(module
#foreach ($func in $functions)
  (func $func_$func (export "func_$func") (param i32) (result i32)
    local.get 0
    i32.const $func
    i32.add

    #foreach ($instr in $instructions)
    i32.const 1
    i32.add
    i32.const 1
    i32.sub
    #end

    #if ($func != 1)
    #set($prev = $func - 1)
    call $func_$prev
    #end
)
#end
)
