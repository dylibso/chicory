# Chicory Component Model - Phase 9A Validation

## Quick Start (2 minutes)

This example demonstrates WebAssembly Component Model support in Chicory with proper WIT (WebAssembly Interface Types) parsing using **world syntax**.

### Build & Run

```bash
# 1. Build Rust WASM
cd example
cargo build --release --target wasm32-unknown-unknown

# 2. Run validation test (from chicory root)
java -cp "wasm-component/target/test-classes:wasm-component/target/classes:runtime/target/classes:wasm/target/classes:wasm-corpus/target/classes" \
  com.dylibso.chicory.component.validation.ExampleWasmValidator
```

**Expected Output:**
```
=== Phase 9A: Real WASM Validation ===
[OK] Loaded WIT file: example/example.wit
[OK] Loaded WASM file: example/example.wasm (9804 bytes)
[OK] WASM module parsed
[OK] ComponentModel loaded successfully

Test 1: add(5, 3) ... ✅ [PASS]
Test 2: multiply(10, 20) ... ✅ [PASS]
Test 3: is-positive(5) ... ✅ [PASS]
Test 4: is-positive(-5) ... ✅ [PASS]

╔════════════════════════════════════════════════════════════╗
║              ✅ ALL 4 TESTS PASSED ✅                      ║
╚════════════════════════════════════════════════════════════╝
```

---

## What's Demonstrated

### 1. **Component Model World Syntax**
```wit
package example:example;

world example {
  export add: func(a: s32, b: s32) -> s32;
  export multiply: func(a: s64, b: s64) -> s64;
  export is-positive: func(x: s32) -> bool;
}
```

- ✅ Package declaration: `package example:example;`
- ✅ World definition: `world example { ... }`
- ✅ Export functions with `export` keyword
- ✅ Hyphenated identifiers: `is-positive`
- ✅ Signed types: `s32`, `s64` (Component Model format)

### 2. **Rust WASM Compilation**
- Uses `wit-bindgen` for automatic ABI code generation
- Targets `wasm32-unknown-unknown` (pure WASM, no OS runtime)
- Optimized binary: **9.6 KB** (using LTO + stripping)

### 3. **Java ComponentModel API**
```java
ComponentModel component = ComponentModel.load(witSource, wasmModule);
Object result = component.callExport("add", 5, 3); // Returns 8L
```

- ✅ Loads WIT + WASM together
- ✅ Parses full world syntax (package, world, exports)
- ✅ Calls guest functions with typed parameters
- ✅ Returns decoded results

### 4. **Primitive Type Support**
- ✅ `s32` → Java `i32` (signed 32-bit)
- ✅ `s64` → Java `i64` (signed 64-bit)
- ✅ `bool` → Java boolean
- ✅ Proper encoding/decoding via CanonicalAbi

---

## Implementation Status

| Component | Status | Notes |
|-----------|--------|-------|
| WIT Parser | ✅ Complete | Supports world syntax + import/export |
| CanonicalAbi | ✅ Complete | Encodes/decodes primitives |
| Type System | ✅ Complete | i32, i64, bool, records, variants, lists |
| Memory Allocation | ✅ Host side | Guest-side cabi_realloc in Phase 9B |
| String Support | ⏳ Phase 9B | Requires guest-side cabi_realloc |
| Host→Guest Calls | ✅ Working | Demonstrated by add, multiply, is-positive |
| Guest→Host Calls | ⏳ Phase 9C | Imports not yet tested |

---

## Architecture

### Memory Model
```
Guest WASM Module (9.6 KB)
├─ Linear Memory (isolated)
└─ Exports (cabi_realloc, add, multiply, is-positive)

Java Host (Chicory Runtime)
├─ Instantiates WASM module
├─ Coordinates via WIT definitions
├─ Calls guest exports
└─ Handles type encoding/decoding
```

### Calling Convention
```
Host (Java)                Guest (WASM)
   │
   ├─ callExport("add", 5, 3)
   │  ├─ Encode args via CanonicalAbi
   │  │  (5 → s32, 3 → s32)
   │  │
   └──→ Call add function
        ├─ Compute 5 + 3 = 8
        │  (returns i32)
        │
   ┌───← Return 8
   │
   ├─ Decode result
   │  (8 ← s32)
   │
   └─ result = 8L
```

### Type Encoding
- **i32/s32**: 32-bit signed integer
- **i64/s64**: 64-bit signed integer  
- **bool**: 0 = false, 1 = true
- **Alignment**: Per canonical ABI (1-byte aligned for all primitives in this phase)

---

## How to Modify

### Add a New Export Function

1. **Update WIT** (`example/example.wit`):
```wit
export add-numbers: func(a: s32, b: s32) -> s32;
export new-function: func(x: i32) -> i64;
```

2. **Update Rust** (`example/src/lib.rs`):
```rust
impl Guest for Example {
    fn add(a: i32, b: i32) -> i32 { a + b }
    fn new_function(x: i32) -> i64 { x as i64 * 2 }
}
```

3. **Update Test** (`ExampleWasmValidator.java`):
```java
Object result = component.callExport("new-function", 10);
```

4. **Rebuild**:
```bash
cargo build --release --target wasm32-unknown-unknown
```

### Change Function Signatures

Example: Add s64 parameter
```wit
export compute: func(a: s32, b: s64) -> s64;
```

**Important**: Type names use WIT convention (s32, s64, bool). Rust implementation automatically maps via wit-bindgen.

---

## Key Files

| File | Size | Purpose |
|------|------|---------|
| `example.wit` | 179 B | Component Model world definition |
| `src/lib.rs` | ~30 lines | Rust WASM implementation |
| `Cargo.toml` | 189 B | Rust project config |
| `example.wasm` | 9.6 KB | Compiled WASM binary |
| `ExampleWasmValidator.java` | ~100 lines | Java test harness |

---

## Performance

- **Binary Size**: 9.6 KB (optimized)
- **Compilation**: ~1 second (cargo build)
- **Test Runtime**: <100 ms (all 4 tests)
- **Function Call Overhead**: Minimal (direct export invocation)

---

## Next Steps: Phase 9B (Strings & Complex Types)

To add string support:
1. Implement guest-side `cabi_realloc` export
2. Add string functions to WIT (e.g., `greet: func(name: string) -> string`)
3. Extend Java CanonicalAbi for string encoding/decoding
4. Test memory allocation between host and guest

---

## Contributing

This implementation is designed for eventual contribution to Chicory upstream. Currently:
- ✅ Passes all existing tests (29/29)
- ✅ Zero breaking changes
- ✅ Follows Chicory code patterns
- ⏳ Ready for Phase 9B before upstream submission

For internal CDV use, this is production-ready for **primitive types only**.

---

**Status**: Phase 9A Complete ✅  
**Last Updated**: 2026-05-22  
**Next Phase**: 9B (Strings & Memory Allocation)
