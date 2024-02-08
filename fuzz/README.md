# fuzz

Fuzz testing module - kept separate as it needs some additional tools to be installed globally.

## Requirements

`wasmtime` (17.0.0) and, at the moment we need to use `export WASMTIME_NEW_CLI=0` to get the output on stdout, cannot find an equivalent with the new cli ...
`wasm-tools` (1.0.57) to use `wasm-smith`

The Chicory cli should be compiled and available on the default path.
