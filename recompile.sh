
(
    cd zig-source
    ../zig-install/zig test --test-no-exec -target wasm32-wasi --zig-lib-dir ./lib ./lib/std/std.zig
)

rm -f zig-testsuite/test-opt.wasm


./binaryen-install/bin/wasm-opt zig-source/.zig-cache/o/af9532cec1404e07c3a2418d07b4d49f/test.wasm -O3 --strip-dwarf -o zig-testsuite/test-opt.wasm
