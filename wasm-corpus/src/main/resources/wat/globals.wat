(module
;; TODO - we don't suppor this yet, only i32.const and i64.const
;;(global $from_env (import "env" "from_env") i32)
(global $from_wasm i32 (i32.const 10))

(func $doit (param i32) (result i32)
    local.get 0
    global.get $from_wasm
    i32.add
)

(export "doit" (func $doit))
)