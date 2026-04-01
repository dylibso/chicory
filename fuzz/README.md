# fuzz

Differential fuzz testing module — compares Chicory's interpreter against its compiler using randomly generated WebAssembly modules.

## How it works

1. **wasm-smith** (via the in-process `wasm-tools` WASM module) generates random valid WASM modules from random seeds
2. Each module's exported functions are called with random parameters through both the **interpreter** (oracle) and the **compiler** (subject)
3. Results are compared — any difference is a bug

No external tools are required. Everything runs in-process using Chicory itself.

## Running

```bash
# Build everything first
mvn -B install -DskipTests -Dquickly

# Run fuzz tests with default 10 iterations per instruction type
mvn -B verify -pl fuzz -DskipTests=false

# Run with custom iteration count
mvn -B verify -pl fuzz -DskipTests=false -Dfuzz.test.iterations=50
```

## Crash reproducers

When a differential mismatch or parse failure is found, a crash reproducer folder is automatically saved to `src/test/resources/crash-{type}-{hash}/` containing:

- `test.wasm` — the binary module
- `seed.txt` — the seed used for generation
- `crash-info.properties` — metadata (instruction type, function name, expected/actual results)

These are automatically picked up by `RegressionTest` on subsequent runs to prevent regressions.

## Run a single reproducer

```bash
export CHICORY_FUZZ_SEED=path/to/seed.txt
export CHICORY_FUZZ_TYPES=numeric,table

mvn -B verify -pl fuzz -DskipTests=false -Dtest="SingleReproTest#singleReproducer"
```

## Configuration

Smith defaults are in `src/test/resources/smith.default.properties`:

```
min-exports=10
max-imports=0
max-modules=1
simd-enabled=false
relaxed-simd-enabled=false
```
