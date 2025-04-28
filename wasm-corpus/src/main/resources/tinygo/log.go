package main

//go:wasmimport env log
func log(x int32) int32

//go:wasm-module sum
//export add
func add(x, y int32) int32 {
	r := x + y
	log(r)
	return r
}

// main is required for the `wasi` target, even if it isn't used.
func main() {}
