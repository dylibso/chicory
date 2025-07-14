
@_extern(wasm, module: "env", name: "operation")
@_extern(c)
func operation(_ a: Int, _ b: Int) -> Int

@_expose(wasm, "run")
@_cdecl("run") // This is still required to call the function with C ABI
func run(_ lhs: Int, _ rhs: Int) -> Int {
    return operation(lhs, rhs)
}
