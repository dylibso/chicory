# Rust WASM Compilation Guide

How to compile Rust to WebAssembly for use with Chicory Component Model.

---

## Prerequisites

### 1. Install Rust

**macOS/Linux:**
```bash
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
source "$HOME/.cargo/env"    # Load Rust in current shell
```

**Windows:**
- Download installer: https://rustup.rs/
- Run `.exe` file
- Follow prompts

**Verify:**
```bash
rustc --version
cargo --version
```

### 2. Add WASM Target

```bash
rustup target add wasm32-unknown-unknown
```

**Verify:**
```bash
rustup target list | grep wasm32-unknown-unknown
# Should show: wasm32-unknown-unknown (installed)
```

---

## Quick Compile

For the example project:

```bash
cd example/
cargo build --release --target wasm32-unknown-unknown
cp target/wasm32-unknown-unknown/release/example.wasm .
```

**Result:** `example.wasm` (9.6 KB) ready to use ✅

---

## Complete Build Workflow

### 1. Define WIT Interface

Create `example.wit`:

```wit
package example:example;

world example {
  export add: func(a: s32, b: s32) -> s32;
  export multiply: func(a: s64, b: s64) -> s64;
  export is-positive: func(x: s32) -> bool;
}
```

### 2. Configure Cargo.toml

```toml
[package]
name = "example"
version = "0.1.0"
edition = "2021"

[lib]
crate-type = ["cdylib"]

[dependencies]
wit-bindgen = "0.11"

[profile.release]
opt-level = "z"
lto = true
strip = true
```

### 3. Implement in Rust

Create `src/lib.rs`:

```rust
wit_bindgen::generate!({
    path: "example.wit",
    world: "example",
    exports: {
        world: Example,
    }
});

pub struct Example;

impl Guest for Example {
    fn add(a: i32, b: i32) -> i32 {
        a + b
    }

    fn multiply(a: i64, b: i64) -> i64 {
        a * b
    }

    fn is_positive(x: i32) -> bool {
        x > 0
    }
}
```

### 4. Build

```bash
cargo build --release --target wasm32-unknown-unknown
```

### 5. Extract

```bash
cp target/wasm32-unknown-unknown/release/example.wasm .
ls -lh example.wasm
```

---

## Optimization Profiles

### Size Optimization (Default)

```toml
[profile.release]
opt-level = "z"     # Smallest size
lto = true          # Link-time optimization
strip = true        # Remove debug symbols
```

**Result:** ~9.6 KB

### Speed Optimization

```toml
[profile.release]
opt-level = 3       # Fastest execution
codegen-units = 1   # Better optimization
```

**Result:** Larger binary, faster runtime

### Debug Builds (Development)

```bash
cargo build --target wasm32-unknown-unknown  # Debug
# Result: ~50 KB, but compiles faster (1s vs 3s)
```

---

## Common Errors & Solutions

| Error | Cause | Solution |
|-------|-------|----------|
| `unknown target wasm32-unknown-unknown` | Target not installed | `rustup target add wasm32-unknown-unknown` |
| `cannot find type Guest` | wit-bindgen didn't generate | `cargo clean && cargo build ...` |
| `name i32 is not defined` (in .wit) | Using Rust type names in WIT | Use WIT types: s32, s64, u32, u64, bool |
| `No such file: example.wasm` | Not built yet | Run `cargo build --release --target wasm32-unknown-unknown` |
| `expected world, interface or use` (in .wit) | Invalid WIT syntax | Use `world { export ... }` format |

---

## Type Reference

WIT types → Rust types:

| WIT | Rust | Size |
|-----|------|------|
| s32 | i32 | 32-bit signed |
| s64 | i64 | 64-bit signed |
| u32 | u32 | 32-bit unsigned |
| u64 | u64 | 64-bit unsigned |
| bool | bool | 1-bit (0 or 1) |
| string | String | UTF-8 (Phase 9B) |

---

## File Locations

```
example/
├── Cargo.toml                      ← Config
├── src/lib.rs                      ← Your code
├── example.wit                     ← Interface
├── example.wasm                    ← Output
└── target/
    └── wasm32-unknown-unknown/
        ├── debug/example.wasm      ← Debug build
        └── release/example.wasm    ← Optimized build (copy this)
```

---

## Build Commands Cheat Sheet

```bash
# Build release (optimized)
cargo build --release --target wasm32-unknown-unknown

# Build debug (faster compile, larger binary)
cargo build --target wasm32-unknown-unknown

# Check without building
cargo check --target wasm32-unknown-unknown

# Clean build artifacts
cargo clean

# Verify size
ls -lh target/wasm32-unknown-unknown/release/example.wasm

# Disassemble to WAT (human-readable)
wasm2wat example.wasm -o example.wat

# Check exports
wasm-objdump -x example.wasm | grep export
```

---

## Performance

| Build Type | Time | Size | Optimization |
|-----------|------|------|-------------|
| Debug | 1-2s | 50 KB | None |
| Release | 3-5s | 15 KB | Medium |
| Release + LTO | 3-5s | 10 KB | High (size) |
| First build | ~30s | - | Slower (downloads deps) |
| Incremental | 1-3s | - | Fast |

---

## CI/CD Example (GitHub Actions)

```yaml
name: Build WASM

on: [push]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - uses: actions-rs/toolchain@v1
        with:
          toolchain: stable
          target: wasm32-unknown-unknown
      
      - name: Build
        run: |
          cd example/
          cargo build --release --target wasm32-unknown-unknown
          cp target/wasm32-unknown-unknown/release/example.wasm .
      
      - name: Upload
        uses: actions/upload-artifact@v2
        with:
          name: example.wasm
          path: example/example.wasm
```

---

## Next Steps

1. Build: `cargo build --release --target wasm32-unknown-unknown`
2. Extract: `cp target/wasm32-unknown-unknown/release/example.wasm .`
3. Test: Run `ExampleWasmValidator.java`
4. Modify: Edit `src/lib.rs` or `example.wit`, rebuild

---

**More info:** See README.md for full details
