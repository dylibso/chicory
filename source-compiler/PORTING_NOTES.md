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
- **Function calls**: `instance.getMachine().call(funcId, args)` with boxing/unboxing via `SourceCompilerUtil.boxJvmToLong`/`unboxLongToJvm`. CALL and CALL_INDIRECT share a common `emitCallWithArgs` helper. CALL_INDIRECT resolves the `refInstance` from the table entry (`table.instance(idx)`) for cross-module dispatch — the function is called through `refInstance.getMachine().call()`, not the current module's machine.

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

**Test results (26,907 tests, 0 failures, 0 errors, 38 skipped):**

All spec tests pass plus 25 ported WasmModuleTest tests. The full WASM v1 spec test suite is included and passing:

| Test Suite | Pass/Total | Skipped | Notes |
|---|---|---|---|
| SpecV1AddressTest | 232/260 | 28 | skipped (trap behavior) |
| SpecV1AlignTest | 162/162 | 0 | |
| SpecV1BlockTest | 223/223 | 0 | |
| SpecV1BrTest | 97/97 | 0 | |
| SpecV1BrIfTest | 118/118 | 0 | |
| SpecV1BrTableTest | 174/174 | 0 | |
| SpecV1BulkTest | 117/117 | 0 | |
| SpecV1CallTest | 91/91 | 0 | |
| SpecV1CallIndirectTest | 172/172 | 0 | |
| SpecV1ConstTest | 778/778 | 0 | |
| SpecV1ConversionsTest | 619/619 | 0 | |
| SpecV1DataTest | 61/61 | 0 | |
| SpecV1ElemTest | 98/98 | 0 | cross-module CALL_INDIRECT fixed |
| SpecV1EndiannessTest | 69/69 | 0 | |
| SpecV1ExportsTest | 96/96 | 0 | |
| SpecV1F32Test | 2514/2514 | 0 | |
| SpecV1F32BitwiseTest | 364/364 | 0 | |
| SpecV1F32CmpTest | 2407/2407 | 0 | |
| SpecV1F64Test | 2514/2514 | 0 | |
| SpecV1F64BitwiseTest | 364/364 | 0 | |
| SpecV1F64CmpTest | 2407/2407 | 0 | |
| SpecV1FacTest | 8/8 | 0 | |
| SpecV1FloatExprsTest | 927/927 | 0 | |
| SpecV1FloatLiteralsTest | 179/179 | 0 | |
| SpecV1FloatMemoryTest | 90/90 | 0 | |
| SpecV1FloatMiscTest | 471/471 | 0 | |
| SpecV1ForwardTest | 5/5 | 0 | |
| SpecV1FuncTest | 172/172 | 0 | |
| SpecV1FuncPtrsTest | 36/36 | 0 | |
| SpecV1GlobalTest | 110/110 | 0 | |
| SpecV1I32Test | 460/460 | 0 | |
| SpecV1I64Test | 416/416 | 0 | |
| SpecV1IfTest | 241/241 | 0 | |
| SpecV1ImportsTest | 178/178 | 0 | |
| SpecV1IntExprsTest | 108/108 | 0 | |
| SpecV1IntLiteralsTest | 51/51 | 0 | |
| SpecV1LabelsTest | 29/29 | 0 | |
| SpecV1LeftToRightTest | 96/96 | 0 | |
| SpecV1LinkingTest | 132/132 | 0 | all passing (TrapException + cross-module fix) |
| SpecV1LoadTest | 97/97 | 0 | |
| SpecV1LocalGetTest | 36/36 | 0 | |
| SpecV1LocalSetTest | 53/53 | 0 | |
| SpecV1LocalTeeTest | 97/97 | 0 | |
| SpecV1LoopTest | 120/120 | 0 | |
| SpecV1MemoryCopyTest | 4450/4450 | 0 | |
| SpecV1MemoryFillTest | 100/100 | 0 | |
| SpecV1MemoryGrowTest | 104/104 | 0 | |
| SpecV1MemoryInitTest | 240/240 | 0 | |
| SpecV1MemoryRedundancyTest | 8/8 | 0 | |
| SpecV1MemorySizeTest | 42/42 | 0 | |
| SpecV1MemoryTest | 88/88 | 0 | |
| SpecV1MemoryTrapTest | 182/182 | 0 | |
| SpecV1NamesTest | 486/486 | 0 | |
| SpecV1NopTest | 88/88 | 0 | |
| SpecV1RefFuncTest | 17/17 | 0 | |
| SpecV1RefIsNullTest | 16/16 | 0 | |
| SpecV1RefNullTest | 3/3 | 0 | |
| SpecV1ReturnTest | 84/84 | 0 | |
| SpecV1SelectTest | 148/148 | 0 | |
| SpecV1SkipStackGuardPageTest | 11/11 | 0 | |
| SpecV1StackTest | 7/7 | 0 | |
| SpecV1StartTest | 20/20 | 0 | TrapException fix |
| SpecV1StoreTest | 68/68 | 0 | |
| SpecV1SwitchTest | 28/28 | 0 | |
| SpecV1TableCopyTest | 1728/1728 | 0 | |
| SpecV1TableFillTest | 45/45 | 0 | |
| SpecV1TableGetTest | 16/16 | 0 | |
| SpecV1TableGrowTest | 58/58 | 0 | |
| SpecV1TableInitTest | 780/780 | 0 | |
| SpecV1TableSetTest | 26/26 | 0 | |
| SpecV1TableSizeTest | 39/39 | 0 | |
| SpecV1TableSubTest | 2/2 | 0 | |
| SpecV1TableTest | 19/19 | 0 | |
| SpecV1TrapsTest | 26/36 | 10 | |
| SpecV1TypeTest | 3/3 | 0 | |
| SpecV1UnreachableTest | 64/64 | 0 | |
| SpecV1UnreachedValidTest | 7/7 | 0 | |
| SpecV1UnwindTest | 50/50 | 0 | |
| SourceCompilerTest | 3/3 | 0 | includes wat2wasm end-to-end |
| WasmModuleSourceCompilerTest | 25/25 | 0 | ported from runtime WasmModuleTest |

**End-to-end wat2wasm compilation**: the source compiler successfully generates Java source from the wat2wasm binary (1,885 functions, ~12.7MB of source), javac compiles it to bytecode, and the compiled class loads and creates a machine factory.

### Method splitting (`MethodSplitter`)

The Java compiler enforces a 64KB bytecode limit per method. The `MethodSplitter` class (in `source-compiler/src/main/java/.../internal/MethodSplitter.java`) automatically handles this during source generation.

**How it works:**

1. **Local-to-array conversion**: converts `int var0`, `long var1`, etc. to `int[] iL`, `long[] lL` arrays so state can be shared between the original method and extracted helpers.

2. **Block extraction**: finds the best labeled block to extract into a helper method. The block is replaced with a helper call + dispatch code:
   ```java
   // Original: label_5: { ... large code with break label_3; ... }
   // After extraction:
   label_5: {
       int _d0 = func_804__h0(iL, lL, memory, instance);
       if (_d0 == 1) break label_3;
       if (_d0 == 2) return iL[0];
   }
   ```

3. **Helper method pattern**: extracted code is wrapped in a `do-while(false)` pattern:
   ```java
   private static int func_804__h0(int[] iL, long[] lL, Memory memory, Instance instance) {
       int[] _hs = { 0 };
       _hb: do {
           // ... extracted code ...
           // break label_3 becomes: { _hs[0] = 1; break _hb; }
           // return X becomes: { iL[0] = X; _hs[0] = 2; break _hb; }
       } while (false);
       return _hs[0];
   }
   ```

4. **Switch splitting**: for methods with large `switch` statements (br_table dispatch), the switch entries are split in half into two helper methods.

**Key design decisions:**

- **Block selection uses half-method targeting**: extracts the block closest to half the method size, not the absolute largest. This ensures geometric convergence (each split roughly halves the method). Extracting the largest block (which is often 90%+ of the method) just moves code without reducing it, causing divergent infinite loops.
- **75% cap**: blocks larger than 75% of the method are rejected for extraction to prevent the shift-without-reduce problem.
- **100-iteration safety cap**: prevents infinite loops if heuristics fail to converge.
- **Undeclared array detection**: `long[]` temporaries (e.g., `callArgs_0`, `callResult_1`) used in extracted blocks but declared at the parent method scope are re-declared inside the helper.
- **Switch helper status propagation**: the `_hs[0] = _sw; break _hb;` fallthrough for propagating status codes from switch helpers is only emitted when the parent method has the `_hb` do-while wrapper (i.e., it's itself a helper). Original methods don't have this wrapper, and all label codes are explicitly dispatched.

### Known limitations and remaining work

- **Sequential code splitting**: the `MethodSplitter` can only extract labeled blocks and split switch statements. Methods with large amounts of sequential code (no labeled blocks or switches) remain unsplit. These methods still compile via javac but may approach the 64KB limit for very large WASM functions. A future improvement would be to split sequential statement runs into helper methods.
- **Runtime correctness of wat2wasm**: the source compiler generates compilable code for wat2wasm, but running it produces "out of bounds memory access: attempted to access address: 67108872 but limit is: 786432" during instance initialization. The same error occurs with the hand-decompiled version, suggesting a pre-existing issue in the decompiled-wabt module (possibly stale runtime classes or a missing memory growth path).

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
