# Quick Start - Rust WASM Edition

Fast setup and modification guide.

---

## ⚡ 2-Minute Setup

### Prerequisites (one-time)

```bash
# 1. Install Rust
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# 2. Add WASM target
rustup target add wasm32-unknown-unknown
```

### Build & Test (repeatable)

```bash
# 1. Build WASM from Rust
cd example/
cargo build --release --target wasm32-unknown-unknown
cp target/wasm32-unknown-unknown/release/example.wasm .

# 2. Run tests
cd ..
java -cp "target/classes:..." \
  com.dylibso.chicory.component.validation.ExampleWasmValidator
```

**Expected:** `All tests passed! 4/4 ✅`

---

## What Each File Is

| File | Purpose | Edit? |
|------|---------|-------|
| `src/lib.rs` | Rust implementation | ✏️ YES |
| `example.wit` | Interface definition | ✏️ YES |
| `Cargo.toml` | Project config | ⚠️ Rarely |
| `example.wasm` | Compiled binary | ❌ NO (auto-generated) |

---

## How to Modify & Test

### Scenario 1: Change Function Implementation

**Step 1:** Edit `src/lib.rs`

```rust
impl Guest for Example {
    fn add(a: i32, b: i32) -> i32 {
        a + b + 10  // Changed: add 10 to result
    }
    // ... rest unchanged ...
}
```

**Step 2:** Rebuild & Test

```bash
cd example/
cargo build --release --target wasm32-unknown-unknown
cp target/wasm32-unknown-unknown/release/example.wasm .
cd ..
java -cp "..." com.dylibso.chicory.component.validation.ExampleWasmValidator
```

### Scenario 2: Add a New Function

**Step 1:** Update `example.wit`

```wit
package example:example;

world example {
  export add: func(a: s32, b: s32) -> s32;
  export multiply: func(a: s64, b: s64) -> s64;
  export is-positive: func(x: s32) -> bool;
  export abs: func(x: s32) -> s32;          # ← NEW
}
```

**Step 2:** Implement in `src/lib.rs`

```rust
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

    fn abs(x: i32) -> i32 {              # ← NEW
        if x < 0 { -x } else { x }
    }
}
```

**Step 3:** Rebuild

```bash
cd example/
cargo build --release --target wasm32-unknown-unknown
cp target/wasm32-unknown-unknown/release/example.wasm .
```

**Step 4:** Test in `ExampleWasmValidator.java`

```java
Object result = component.callExport("abs", -42);
System.out.println("abs(-42) = " + result);
assert result.equals(42L);
```

**Step 5:** Run tests

```bash
cd ..
java -cp "..." com.dylibso.chicory.component.validation.ExampleWasmValidator
```

### Scenario 3: Change Function Signature

**Example:** Change parameter type

**Step 1:** Update `example.wit`

```wit
# Before:
export process: func(x: s32) -> s32;

# After:
export process: func(x: s64) -> s64;     # ← Changed types
```

**Step 2:** Update `src/lib.rs`

```rust
# Before:
fn process(x: i32) -> i32 { ... }

# After:
fn process(x: i64) -> i64 { ... }       # ← Changed types
```

**Step 3:** Rebuild

```bash
cd example/
cargo build --release --target wasm32-unknown-unknown
cp target/wasm32-unknown-unknown/release/example.wasm .
```

---

## Common Issues

### "rustup not found"
→ Install Rust: `curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh`

### "error: can't find 'wasm32-unknown-unknown'"
→ Add target: `rustup target add wasm32-unknown-unknown`

### "No such file: example.wasm"
→ Build it: `cd example && cargo build --release --target wasm32-unknown-unknown && cp target/wasm32-unknown-unknown/release/example.wasm .`

### "error[E0412]: cannot find type `Guest`"
→ Let wit-bindgen regenerate: `cargo clean && cargo build --release --target wasm32-unknown-unknown`

### Build is slow
→ Normal first time (~30s), incremental should be fast. Check disk space: `df -h`

### "ClassNotFoundException: ExampleWasmValidator"
→ Run from chicory root: `cd /path/to/chicory && java -cp ...`

---

## Understanding Rust WASM Development

### The Three Files

**1. example.wit (Interface)**
```wit
export add: func(a: s32, b: s32) -> s32;
```
"There's a function called add that takes two s32s"

**2. src/lib.rs (Implementation)**
```rust
fn add(a: i32, b: i32) -> i32 {
    a + b
}
```
"Here's how add actually works"

**3. example.wasm (Binary)**
```
Binary WebAssembly (compiled from Rust)
```
"Executable code that Chicory runs"

### The Build Process

```
example.wit
    ↓ (wit-bindgen reads)
Generates Rust trait
    ↓ (Your code implements)
Your functions
    ↓ (rustc compiles)
example.wasm
    ↓ (Component Model loads)
Java can call functions
```

---

## Type Reference

When writing functions, use these Rust types:

| WIT Type | Rust Type | Example |
|----------|-----------|---------|
| s32 | i32 | -100, 0, 42 |
| s64 | i64 | -1000000 |
| u32 | u32 | 0, 100 |
| u64 | u64 | Large numbers |
| bool | bool | true, false |
| string | String | "hello" (Phase 9B) |

**Type mapping:** WIT s32 ↔ Rust i32 ↔ Java Long

---

## Performance Tips

### Size Optimization

Current: 9.6 KB (already optimized)

Already have in `Cargo.toml`:
```toml
[profile.release]
opt-level = "z"    # Optimize for size
lto = true         # Link-time optimization
strip = true       # Remove debug info
```

### Speed Optimization

Add to `Cargo.toml`:
```toml
[profile.release]
opt-level = 3      # Or 2, or "s" for size
codegen-units = 1  # Better optimization
```

### Build Time

- Clean build: ~2-3 seconds
- Incremental: ~1 second
- First build: ~30 seconds (downloads wit-bindgen)

---

## Useful Commands

```bash
# Build optimized
cargo build --release --target wasm32-unknown-unknown

# Build with debug info
cargo build --target wasm32-unknown-unknown

# Check without building
cargo check --target wasm32-unknown-unknown

# Clean build artifacts
cargo clean

# See generated code
cargo expand --target wasm32-unknown-unknown

# Check size
ls -lh target/wasm32-unknown-unknown/release/example.wasm

# Disassemble WASM
wasm2wat example.wasm -o example_disassembled.wat
```

---

## Template: Adding a Function

Copy this template and fill in:

1. **WIT:**
```wit
export YOUR_FUNC: func(PARAM: TYPE) -> RETURN_TYPE;
```

2. **Rust:**
```rust
fn your_func(param: RustType) -> RustType {
    // Your code here
}
```

3. **Test:**
```java
Object result = component.callExport("your_func", value);
System.out.println("Result: " + result);
```

---

## Debugging

### Check What Functions Are Exported

```bash
wasm-objdump -x example.wasm | grep export
```

### See Function Signatures

```bash
wasm-objdump -d example.wasm | head -50
```

### Disassemble to WAT

```bash
wasm2wat example.wasm -o example_debug.wat
cat example_debug.wat
```

### Print in Rust (Won't work in WASM - for reference)

```rust
// This won't work in WASM - no stdout
// println!("Debug: {}", x);

// Instead, return values:
fn debug_value(x: i32) -> i32 {
    // Check this value in Java:
    x
}
```

---

## Testing Strategy

### Quick Test

```bash
cargo build --release --target wasm32-unknown-unknown
cp target/wasm32-unknown-unknown/release/example.wasm .
java -cp "..." com.dylibso.chicory.component.validation.ExampleWasmValidator
```

### Add Your Own Test

Edit `ExampleWasmValidator.java`:

```java
Object result = component.callExport("your_func", arg1, arg2);
System.out.println("your_func(" + arg1 + ", " + arg2 + ") = " + result);
assert result.equals(expected);
```

### Rust Unit Tests

Create `src/lib.rs` tests:

```rust
#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_add() {
        assert_eq!(Example::add(5, 3), 8);
    }
}
```

Run tests:
```bash
cargo test --target wasm32-unknown-unknown
```

---

## Common Patterns

### Pattern 1: Math Function

```rust
fn power(base: i32, exp: i32) -> i32 {
    base.pow(exp as u32)
}
```

### Pattern 2: Comparison

```rust
fn max(a: i32, b: i32) -> i32 {
    if a > b { a } else { b }
}
```

### Pattern 3: State-like Function

```rust
fn fibonacci(n: i32) -> i32 {
    match n {
        0 => 0,
        1 => 1,
        _ => fibonacci(n - 1) + fibonacci(n - 2),
    }
}
```

### Pattern 4: Multiple Conditions

```rust
fn classify(x: i32) -> i32 {
    match x {
        x if x < 0 => -1,
        0 => 0,
        _ => 1,
    }
}
```

---

## Next Steps

1. **Modify a function** - See "Scenario 1"
2. **Add a function** - See "Scenario 2"
3. **Run tests** - Verify changes work
4. **Add complexity** - Implement real logic
5. **Phase 9B** - Add strings & memory
6. **Phase 9C** - Add bidirectional calls

---

## Summary

| Task | Commands |
|------|----------|
| Build | `cargo build --release --target wasm32-unknown-unknown` |
| Update | `cp target/wasm32-unknown-unknown/release/example.wasm .` |
| Test | `java -cp "..." ExampleWasmValidator` |
| Modify | Edit `src/lib.rs` or `example.wit`, rebuild, test |
| Clean | `cargo clean` |

---

**Ready?** Modify a function and test it! 🚀
