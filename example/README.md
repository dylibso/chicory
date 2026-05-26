# Component Model Example - Rust WASM Implementation

Complete real-world validation of the Chicory Component Model MVP with a **Rust-compiled WASM module**.

## Overview

This example demonstrates the entire flow:

```
1. Rust Source Code (src/lib.rs)
   ↓ (cargo build with wit-bindgen)
2. Rust Compilation to WASM (example.wasm)
   ↓ (Component Model loads)
3. Java Application (ExampleWasmValidator.java)
   ↓
4. Call WASM Functions from Java
   ↓
5. Validated Results (4/4 tests passing ✅)
```

## Quick Start

### 1. Build WASM from Rust

```bash
cd example/
cargo build --release --target wasm32-unknown-unknown
cp target/wasm32-unknown-unknown/release/example.wasm .
```

**Output:**
- `example.wasm` (9.6 KB) - Compiled WebAssembly binary

### 2. Validate Wit Interface in Wasm

```bash
cd example/
wasm-tools validate example.wasm              # Ensure the WASM module is valid
wasm-tools component wit example.wasm         # Validate against the expected WIT interface
wasm-tools component wit example.wasm --json  # Optional: Get JSON output for debugging
```
