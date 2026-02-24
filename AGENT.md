# Working with this repository

Chicory is a WebAssembly runtime written in Java. This document captures practical knowledge for working in the codebase.

## Prerequisites

- Java 11+ (build targets Java 11 compatibility)
- Maven (or use `./mvnw` / `./mvnw.cmd`)

## Key build commands

```bash
# Full build with all tests
mvn clean install

# Quick install, skip all tests and checks
mvn -Dquickly

# Skip tests only
mvn install -DskipTests

# Disable linters and enforcers during development
mvn -Ddev <goals>

# Autoformat code (required before committing)
mvn spotless:apply
```

## Module dependency graph

Maven plugins are defined in the same build, so modules do not build in isolation. Always use `-pl` and `-am` to include dependencies:

```
wasm-corpus (test resources)
wasm (parser, validator, types)
  └── runtime (interpreter, Instance, Store)
        ├── wasi (WASI preview1)
        │     └── wasm-tools (wat2wasm, wast2json via WASI)
        ├── compiler (JVM bytecode compiler)
        ├── simd (SIMD opcodes, pluggable machine)
        └── log
```

Other modules: `annotations`, `annotations/processor`, `build-time-compiler`, `compiler-maven-plugin`, `dircache`.

## Building and testing a single module

After changing code in a module, you must `mvn install` it (and its dependencies) before downstream modules can see the changes. Use `-DskipTests` when you only need to propagate artifacts:

```bash
# Build runtime and everything it depends on, skip tests
mvn install -pl runtime -am -DskipTests

# Run only runtime unit tests (after install)
mvn test -pl runtime

# Build everything up to wasm-tools
mvn install -pl wasm-tools -am -DskipTests
```

## Spec tests (runtime-tests)

The WebAssembly spec testsuite lives in `testsuite/`. JUnit tests are **generated** from `.wast` files by the `test-gen-plugin` Maven plugin at build time.

### Adding a new spec test

1. Add the `.wast` filename to `<includedWasts>` in `runtime-tests/pom.xml`
2. Run `mvn install -pl runtime-tests -am` to regenerate the JUnit test classes
3. Run `mvn surefire:test -pl runtime-tests` to execute them

Individual tests can be excluded via `<excludedTests>` in the same pom.

### Running spec tests

```bash
# Full spec test suite (interpreter) — install first to generate test classes, then run
mvn install -pl runtime-tests -am -DskipTests
mvn surefire:test -pl runtime-tests

# Or in one shot (install runs tests too)
mvn install -pl runtime-tests -am

# Compiler spec tests
mvn surefire:test -pl compiler-tests

# WASI spec tests
mvn surefire:test -pl wasi-tests
```

### Running a single test class

```bash
mvn surefire:test -pl runtime-tests -Dtest=SpecV1GcStructTest
```

## Test modules

| Module | What it tests |
|---|---|
| `runtime-tests` | Interpreter against the WebAssembly spec testsuite |
| `compiler-tests` | JVM bytecode compiler against the spec testsuite |
| `machine-tests` | Shared tests for both interpreter and compiler |
| `wasi-tests` | WASI preview1 against the WASI testsuite |

## Code style

- No wildcard imports (configure your IDE accordingly)
- Run `mvn spotless:apply` before committing
- Approval tests: set `APPROVAL_TESTS_USE_REPORTER=AutoApproveReporter` to auto-approve golden samples

## Module architecture overview

### `wasm` module
- `Parser.java` — binary format parser
- `Validator.java` — type checking and validation (see spec appendix for the algorithm)
- `types/` — all Wasm types: `ValType`, `FunctionType`, `SubType`, `RecType`, `CompType`, `StructType`, `ArrayType`, `FieldType`, `StorageType`, `PackedType`, `TypeSection`, `OpCode`

### `runtime` module
- `Instance.java` — module instantiation, GC ref storage, heap type matching
- `InterpreterMachine.java` — opcode interpreter (the main execution loop)
- `Store.java` — cross-module linking
- `ImportFunction.java` — imported function representation with cross-module type validation
- `ConstantEvaluators.java` — constant expression evaluation (globals, element/data segments)
- `WasmStruct.java`, `WasmArray.java`, `WasmI31Ref.java` — GC object types
- `internal/GcRefStore.java` — auto-keyed store for Wasm GC references with mark-sweep collection

### `compiler` module
- `MachineFactoryCompiler.java` — entry point for the JVM bytecode compiler
- `internal/Compiler.java` — translates Wasm opcodes to JVM bytecode

### `wasi` module
- `WasiPreview1.java` — WASI preview1 host function implementations
- `WasiOptions.java` — configuration (stdin/stdout/stderr, directories, env vars)

## Performance considerations

- Types should NOT add computation at runtime. Subtyping checks and type lookups should be pre-computed or cached where feasible.
- The hot path in the interpreter (`InterpreterMachine`) must remain fast — avoid per-opcode type section lookups when they can be resolved at validation time.
- The validator enriches instruction operands with type hints (e.g., source heap type for `ref.test`/`ref.cast`/`br_on_cast`) so the interpreter can dispatch without guessing.

## Specification references

- Official WebAssembly spec: https://webassembly.github.io/spec/core/
- Validation algorithm appendix: https://webassembly.github.io/spec/core/appendix/algorithm.html
- GC proposal: https://github.com/WebAssembly/gc/blob/main/proposals/gc/MVP.md
