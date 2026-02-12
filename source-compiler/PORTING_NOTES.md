## Source compiler porting notes

### Goal

- **End goal**: make the **source compiler** feature-complete with the existing ASM-based compiler so that we can eventually compile *all* spec tests (and `wat2wasm`) to **Java source** instead of bytecode.
- **Strategy**: port the behavior of the old compiler **1:1** into `SourceCodeEmitter`, either:
  - **Inlining** the same primitive operations the ASM compiler emits, or
  - **Delegating** to `com.dylibso.chicory.runtime.OpcodeImpl` when the ASM compiler uses `.shared(...)` in `EmitterMap`.

### Where things live

- **Old ASM compiler (reference behavior)**:
  - Opcodes enum: `compiler/src/main/java/com/dylibso/chicory/compiler/internal/CompilerOpCode.java`
  - ASM emitters: `compiler/src/main/java/com/dylibso/chicory/compiler/internal/Emitters.java`
  - Opcode mapping: `compiler/src/main/java/com/dylibso/chicory/compiler/internal/EmitterMap.java`
  - Type analysis: `compiler/src/main/java/com/dylibso/chicory/compiler/internal/WasmAnalyzer.java`
- **New source compiler**:
  - Main entry: `source-compiler/src/main/java/com/dylibso/chicory/source/compiler/MachineFactorySourceCompiler.java`
  - Java AST emission: `source-compiler/src/main/java/com/dylibso/chicory/source/compiler/internal/SourceCodeEmitter.java`
  - WasmAnalyzer (structured control flow): `source-compiler/src/main/java/com/dylibso/chicory/source/compiler/internal/WasmAnalyzer.java`
  - Compiler opcodes: `source-compiler/src/main/java/com/dylibso/chicory/source/compiler/internal/CompilerOpCode.java`
  - Test module & source dumping: `source-compiler/src/test/java/com/dylibso/chicory/testing/TestModule.java`
  - Generated sources: `source-compiler/target/source-dump/com/dylibso/chicory/gen/...`
  - Test generation config: `source-compiler/pom.xml` (`test-gen-plugin` section).

### Porting principles (1:1 with ASM)

- **For each opcode in `CompilerOpCode`**:
  - Look up its mapping in `EmitterMap`:
    - **`.intrinsic(CompilerOpCode.X, Emitters::X)`**:
      - Reproduce the same logic **inline** in `SourceCodeEmitter` using JavaParser AST nodes.
      - Example: `I32_WRAP_I64` is `L2I` in ASM → in source compiler:
        - Pop a `long` expression and push `new CastExpr(PrimitiveType.intType(), value)`.
    - **`.shared(CompilerOpCode.X, OpcodeImpl.class)`**:
      - Call the matching static method on `OpcodeImpl` using `opcodeImplCall("X", args...)`.
  - Add a `case` for the opcode in `SourceCodeEmitter.emitInstruction` that dispatches to the helper.
- **Stack discipline**:
  - Mirror ASM evaluation order: **pop right operand first**, left second, then build expressions in `(a op b)` order.
  - Keep the Java expression stack (`Deque<Expression>`) consistent with `WasmAnalyzer`'s type stack.

### Control flow design

The source compiler uses **structured Java control flow** that mirrors WASM's structured control flow directly:

- **WasmAnalyzer** walks WASM instructions and emits structured `CompilerOpCode`s:
  - `BLOCK_ENTER` / `LOOP_ENTER` / `IF_ENTER` / `ELSE_ENTER` / `SCOPE_EXIT` — scope boundaries
  - `BREAK` / `BREAK_IF` — forward branches (`br` targeting `block`/`if`)
  - `CONTINUE` / `CONTINUE_IF` — backward branches (`br` targeting `loop`)
  - `DROP_KEEP` — stack unwinding for branches that cross scope boundaries
- **SourceCodeEmitter** maps these to Java:
  - `BLOCK_ENTER` → `label_N: { ... }`
  - `LOOP_ENTER` → `label_N: while(true) { ... }`
  - `IF_ENTER` → `label_N: { if (cond != 0) { ... } }` (wrapped in labeled block)
  - `ELSE_ENTER` → `} else { ... }`
  - `SCOPE_EXIT` → close block, assign results, push result vars to expression stack
  - `BREAK` → assign results + `break label_N;`
  - `BREAK_IF` → peek at stack values (no modification), emit `if (cond != 0) { assign results; break label_N; }`. Fall-through path keeps stack unchanged.
  - `CONTINUE` → assign loop params + interrupt check + `continue label_N;`
  - `CONTINUE_IF` → peek at stack values (reversed order), emit `if (cond != 0) { assign params with temps; continue label_N; }`. Fall-through path keeps stack unchanged.
- **Block results**: blocks with return types declare result variables at the method body level (so they're accessible regardless of nesting depth). Result values are assigned on break/exit and pushed onto the expression stack after SCOPE_EXIT.
- **If blocks**: wrapped in `LabeledStmt` + `BlockStmt` (like BLOCK_ENTER) so `break label_N` works. If-param variables declared with default values at function level, assigned inline. For if-without-else with params, an else branch is synthesized to pass params through to results.
- **Loop param assignment**: uses `assignParamsWithTemps` — single param: direct assign; multiple params: uses temp variables to avoid the "swap problem" where sequential assignments read already-modified values.
- **Conditional branches (BREAK_IF / CONTINUE_IF)**: use a **peek-based approach** — read values from the expression stack via `stack.iterator()` without popping. The iterator returns top-to-bottom, so for loop params the order is reversed (`paramVals[paramCount - 1 - i]`). The stack is left unchanged for the fall-through path.
- **Dead code tracking**: the analyzer uses `exitBlockDepth` + `exitTargetLabel` to skip instructions after unconditional branches. `exitTargetLabel` tracks which scope the dead code should stop at, handling `br N` where N > 0. For BLOCK/LOOP scopes, dead code mode is cleared on END (alternate paths via br_if make code after the block reachable).
- **Terminal detection for if/else**: an if/else is terminal for its parent only if BOTH branches terminate AND no break targets the if's own label. An if-without-else is never terminal.
- **Implicit return**: added at end of `generateFunctionMethod` for functions whose body doesn't end with return/throw but has stack values on the expression stack.
- **Function calls**: `instance.getMachine().call(funcId, args)` with boxing/unboxing via `SourceCompilerUtil.boxJvmToLong`/`unboxLongToJvm`. CALL and CALL_INDIRECT share a common `emitCallWithArgs` helper.

### Key implementation details

- **`isBlockTerminated`**: checks if the current block's last statement is a direct terminal (throw/return/break/continue) or a labeled block that is terminal for the parent scope. A labeled block is terminal only if it ends with abrupt completion (throw/return/break/continue) AND no break targets its own label (which would mean flow continues after it).
- **SCOPE_EXIT result push**: only pushes result variables when the scope isn't terminal for the parent, preventing phantom stack values from dead inner blocks.
- **Operator precedence**: all `BinaryExpr` results are wrapped in `EnclosedExpr` (parentheses) to prevent precedence errors in compound expressions.
- **Float/double constants**: `NaN` and `Infinity` values are emitted as `Float.intBitsToFloat(bits)` / `Double.longBitsToDouble(bits)` / `Float.POSITIVE_INFINITY` etc., not via `toString()` which produces invalid Java literals.
- **Globals**: `GLOBAL_GET`/`GLOBAL_SET` use `instance.global(idx).getValue()`/`.setValue()` with proper type boxing/unboxing via `globalTypes`.

### Memory and traps

- **Memory access helpers** in `SourceCodeEmitter`:
  - `getAddrExpr(base, offset)` matches the ASM pattern:
    - `(base < 0) ? base : base + offset`
  - `emitOutOfBoundsIfNeeded(offset, block)`:
    - If compile-time offset is `< 0` or `>= Integer.MAX_VALUE`, emits:
      - `throw new WasmRuntimeException("out of bounds memory access");`
      - and returns early from the emitter method.
    - This matches the ASM compiler's static offset trap behavior.
- **MEMORY_GROW** → `memory.grow(size)`, **MEMORY_SIZE** → `memory.pages()`.

### Naming and packages

- **Generated class names**:
  - Pattern: `com.dylibso.chicory.gen.<moduleDir_>.CompiledMachine_spec_<n>`.
  - `TestModule.extractMangledClassName` is the single place that constructs them.
- **Reserved words**:
  - If a module directory name is a Java keyword (e.g. `const`), we suffix it with `_` (e.g. `const_`) when building the package path.

### Current status

**Test results (14,382 tests, 0 failures, 14 errors, 28 skipped):**

| Test Suite | Pass | Errors | Notes |
|---|---|---|---|
| SpecV1AddressTest | 232/260 | 0 | 28 skipped (trap behavior) |
| SpecV1BlockTest | 220/223 | 3 | br_table not implemented |
| SpecV1BrTest | 97/97 | 0 | |
| SpecV1BrIfTest | 118/118 | 0 | |
| SpecV1ConstTest | 778/778 | 0 | |
| SpecV1F32Test | 2514/2514 | 0 | |
| SpecV1F32BitwiseTest | 364/364 | 0 | |
| SpecV1F32CmpTest | 2407/2407 | 0 | |
| SpecV1F64Test | 2514/2514 | 0 | |
| SpecV1F64BitwiseTest | 364/364 | 0 | |
| SpecV1F64CmpTest | 2407/2407 | 0 | |
| SpecV1FacTest | 8/8 | 0 | |
| SpecV1FloatLiteralsTest | 179/179 | 0 | |
| SpecV1FloatMemoryTest | 90/90 | 0 | |
| SpecV1FloatMiscTest | 471/471 | 0 | |
| SpecV1ForwardTest | 5/5 | 0 | |
| SpecV1I32Test | 460/460 | 0 | |
| SpecV1I64Test | 416/416 | 0 | |
| SpecV1IfTest | 237/241 | 4 | br_table not implemented |
| SpecV1IntExprsTest | 108/108 | 0 | |
| SpecV1IntLiteralsTest | 51/51 | 0 | |
| SpecV1LocalGetTest | 35/36 | 1 | br_table not implemented |
| SpecV1LocalSetTest | 53/53 | 0 | |
| SpecV1LocalTeeTest | 94/97 | 3 | br_table not implemented |
| SpecV1LoopTest | 117/120 | 3 | br_table not implemented |
| SourceCompilerTest | 1/1 | 0 | |

**All 14 remaining errors are from multi-entry `br_table` (SWITCH opcode), which throws "br_table not yet supported".**

### What's not yet implemented

- **Multi-entry `br_table`**: the analyzer emits a `SWITCH` opcode for multi-entry br_table, but the emitter throws. Single-entry br_table (just a default label) works via BREAK/CONTINUE.
- **More spec tests**: only a subset of spec tests are currently targeted. Adding more will likely surface missing opcodes or edge cases.

### Remaining spec tests to add

These are still in `excludedWasts` and should be added one by one:
- `br_table.wast` — requires implementing br_table (SWITCH opcode)
- `call.wast` — function calls
- `call_indirect.wast` — indirect calls
- `labels.wast` — label tests
- `nop.wast` — nop instruction
- `return.wast` — return tests
- `select.wast` — select instruction
- `unwind.wast` — stack unwinding
- `func.wast` — function definitions
- `global.wast` — global variables
- `memory.wast` / `memory_grow.wast` — memory operations
- `load.wast` / `store.wast` — load/store operations
- `conversions.wast` — type conversions
- `endianness.wast`
- `left-to-right.wast`
- `float_exprs.wast`

### Workflow for future porting sessions

- **1. Add wast files from `excludedWasts` to `includedWasts`**
  - Move a batch from excluded to included in `source-compiler/pom.xml`.
  - Keep the lists alphabetically sorted (the plugin enforces this).
  - Run: `mvn test-compile -f source-compiler/pom.xml -Dcheckstyle.skip=true`

- **2. Run focused tests to discover missing opcodes**
  - Example:
    - `mvn test -f source-compiler/pom.xml -Dcheckstyle.skip=true -Dtest=SpecV1FloatMiscTest`
  - Use a timeout to catch infinite loops:
    - `timeout 60 mvn test -f source-compiler/pom.xml -Dcheckstyle.skip=true -Dtest=SpecV1FloatExprsTest`
  - Look at errors like:
    - `IllegalArgumentException: Unsupported opcode: XYZ` in generated `CompiledMachine_spec_*.java` under `source-compiler/target/source-dump/...`.
    - `NoSuchElementException` = stack underflow (expression stack has fewer values than expected).
    - `ClassNotFoundException` = source generation failed entirely (check the source-dump file for the error comment).

- **3. Port the opcode 1:1**
  - Find entry in `EmitterMap` for `CompilerOpCode.X`.
  - If intrinsic:
    - Open `Emitters.X` and copy behavior into a new `public static void X(...)` helper in `SourceCodeEmitter`, using JavaParser AST.
  - If shared:
    - Add a helper that calls `opcodeImplCall("X", args...)`.
  - Wire a new `case X:` in `emitInstruction` to that helper.
  - **Check for duplicates** — some opcodes may already have method implementations but are missing the switch case (or vice versa).

- **4. Re-run the focused tests**
  - Ensure:
    - Generated source exists under `target/source-dump/...`.
    - The specific spec test passes (at least on the **happy path**, even if trap/error messages differ slightly).
  - If a test causes infinite loop, move it back to `excludedWasts` and note it in "Known issues" above.

- **5. Compare with ASM compiler output**
  - Compile the wasm with the ASM compiler: `/home/andreatp/workspace/chicory7/build-time-compiler-cli/target/chicory-compiler <wasm-file>`
  - Decompile with javap: `javap -c -p <class-file>`
  - Compare the JVM bytecode behavior with the generated Java source to debug discrepancies.

### Long-term objective

- Once all opcodes used by the spec tests are ported and the source compiler passes the same suite as the ASM compiler, we can:
  - Switch decompilation / `wat2wasm` style tooling to use **source generation**.
  - Remove reliance on the non-working decompilers and treat this source compiler as the canonical backend.

Keep using the ASM compiler (`Emitters` + `EmitterMap`) as the **ground truth**. For each new opcode or behavior:

- **Ask**: is it `.intrinsic(...)` (inline logic) or `.shared(..., OpcodeImpl.class)`?
- **Replicate** that choice in `SourceCodeEmitter`, maintaining stack order and side effects exactly 1:1.
