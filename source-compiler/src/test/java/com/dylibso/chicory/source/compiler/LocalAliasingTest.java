package com.dylibso.chicory.source.compiler;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wabt.Wat2Wasm;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for local variable aliasing.
 *
 * <p>Each test runs against three backends (interpreter, ASM compiler, source compiler) and asserts
 * they all produce the same result. The interpreter is the ground truth; the ASM compiler uses the
 * JVM operand stack (no aliasing issues); the source compiler uses symbolic NameExpr references
 * that require a snapshot mechanism to avoid aliasing bugs.
 *
 * <p>These tests are derived from patterns observed in WASI test failures (e.g.,
 * close_preopen.wasm, environ_get-multiple-variables.wasm).
 */
public class LocalAliasingTest {

    private static WasmModule parseWat(String wat) {
        return Parser.parse(Wat2Wasm.parse(wat));
    }

    private static Instance interpreterInstance(WasmModule module) {
        return Instance.builder(module).build();
    }

    private static Instance asmInstance(WasmModule module) {
        return Instance.builder(module).withMachineFactory(MachineFactoryCompiler::compile).build();
    }

    private static Instance sourceInstance(WasmModule module) {
        return Instance.builder(module)
                .withMachineFactory(
                        instance ->
                                MachineFactorySourceCompiler.builder(instance.module())
                                        .withClassName("com.dylibso.chicory.gen.AliasingTest")
                                        .withDumpSources(false)
                                        .compile()
                                        .apply(instance))
                .build();
    }

    /**
     * Runs the exported "test" function on all three backends and asserts they produce the same
     * result.
     */
    private static void assertAllBackendsAgree(String wat, long[]... argSets) {
        WasmModule module = parseWat(wat);
        Instance interp = interpreterInstance(module);
        Instance asm = asmInstance(module);
        Instance source = sourceInstance(module);

        for (long[] args : argSets) {
            long[] interpResult = interp.export("test").apply(args);
            long[] asmResult = asm.export("test").apply(args);
            long[] sourceResult = source.export("test").apply(args);

            String label = "args=" + java.util.Arrays.toString(args);
            assertArrayEquals(interpResult, asmResult, "ASM vs interpreter: " + label);
            assertArrayEquals(interpResult, sourceResult, "Source vs interpreter: " + label);
        }
    }

    /**
     * Basic aliasing: local.get pushes old value, local.set overwrites, then the old value is used.
     *
     * <pre>
     * local.get 0    ;; push original param (e.g., 10)
     * local.get 0    ;; push original param again
     * i32.const 1
     * i32.shl        ;; param << 1 = 20
     * local.set 0    ;; overwrite param with 20
     * ;; stack still has old value 10 (from first local.get)
     * ;; return old value (should be 10, not 20)
     * </pre>
     */
    @Test
    public void testBasicAliasing() {
        assertAllBackendsAgree(
                "(module\n"
                        + "  (func (export \"test\") (param i32) (result i32)\n"
                        + "    local.get 0\n"
                        + "    local.get 0\n"
                        + "    i32.const 1\n"
                        + "    i32.shl\n"
                        + "    local.set 0\n"
                        + "  )\n"
                        + ")\n",
                new long[] {10},
                new long[] {42},
                new long[] {0});
    }

    /**
     * Aliasing with call arguments: old value of local is passed as a call argument even after the
     * local has been modified.
     *
     * <p>Pattern from WASI tests: push call args, modify local in between, then call. The first
     * pushed arg should use the old value.
     */
    @Test
    public void testAliasingAcrossCallArgs() {
        assertAllBackendsAgree(
                "(module\n"
                        + "  (func $helper (param i32 i32) (result i32)\n"
                        + "    local.get 0\n"
                        + "    local.get 1\n"
                        + "    i32.add\n"
                        + "  )\n"
                        + "  (func (export \"test\") (param i32) (result i32)\n"
                        + "    local.get 0\n"
                        + "    local.get 0\n"
                        + "    i32.const 3\n"
                        + "    i32.mul\n"
                        + "    local.tee 0\n"
                        + "    call $helper\n"
                        + "  )\n"
                        + ")\n",
                new long[] {10},
                new long[] {7},
                new long[] {1});
    }

    /**
     * Multiple reads of the same local used across a loop, where the local is overwritten each
     * iteration. Each iteration loads a byte into a local, uses the old value in a computation,
     * then overwrites.
     */
    @Test
    public void testAliasingInLoop() {
        assertAllBackendsAgree(
                "(module\n"
                        + "  (memory (export \"mem\") 1)\n"
                        + "  (data (i32.const 0) \"\\01\\02\\03\\04\")\n"
                        + "  (func (export \"test\") (param $count i32) (result i32)\n"
                        + "    (local $i i32)\n"
                        + "    (local $sum i32)\n"
                        + "    (local $val i32)\n"
                        + "    (local.set $i (i32.const 0))\n"
                        + "    (local.set $sum (i32.const 0))\n"
                        + "    (block $done\n"
                        + "      (loop $loop\n"
                        + "        (br_if $done (i32.ge_u (local.get $i) (local.get $count)))\n"
                        + "        (local.set $val (i32.load8_u (local.get $i)))\n"
                        + "        (local.get $val)\n"
                        + "        (local.set $val (i32.xor (local.get $val) (i32.const 255)))\n"
                        + "        (local.set $sum (i32.add (local.get $sum)))\n"
                        + "        (local.set $i (i32.add (local.get $i) (i32.const 1)))\n"
                        + "        (br $loop)\n"
                        + "      )\n"
                        + "    )\n"
                        + "    (local.get $sum)\n"
                        + "  )\n"
                        + ")\n",
                new long[] {4},
                new long[] {1},
                new long[] {2});
    }

    /**
     * Two locals aliased simultaneously: local.get A, local.get B, then set both, use old values.
     * Implements a swap pattern.
     */
    @Test
    public void testSwapPattern() {
        assertAllBackendsAgree(
                "(module\n"
                        + "  (func (export \"test\") (param $a i32) (param $b i32) (result i32)\n"
                        + "    local.get $b\n"
                        + "    local.get $a\n"
                        + "    local.set $b\n"
                        + "    local.set $a\n"
                        + "    local.get $a\n"
                        + "    i32.const 1000\n"
                        + "    i32.mul\n"
                        + "    local.get $b\n"
                        + "    i32.add\n"
                        + "  )\n"
                        + ")\n",
                new long[] {3, 7},
                new long[] {100, 200});
    }

    /**
     * Aliasing with select instruction: old value of local used in select after local.set.
     */
    @Test
    public void testAliasingWithSelect() {
        assertAllBackendsAgree(
                "(module\n"
                        + "  (func (export \"test\") (param $x i32) (result i32)\n"
                        + "    local.get $x\n"
                        + "    local.get $x\n"
                        + "    i32.const 100\n"
                        + "    i32.add\n"
                        + "    local.tee $x\n"
                        + "    local.get $x\n"
                        + "    i32.const 150\n"
                        + "    i32.gt_u\n"
                        + "    select\n"
                        + "  )\n"
                        + ")\n",
                new long[] {10},
                new long[] {60},
                new long[] {50});
    }

    /**
     * Aliasing with local.tee: tee sets and pushes, but previously pushed value should be old.
     */
    @Test
    public void testAliasingWithTee() {
        assertAllBackendsAgree(
                "(module\n"
                        + "  (func (export \"test\") (param $a i32) (param $b i32) (result i32)\n"
                        + "    local.get $a\n"
                        + "    local.get $a\n"
                        + "    local.get $b\n"
                        + "    i32.add\n"
                        + "    local.tee $a\n"
                        + "    i32.add\n"
                        + "  )\n"
                        + ")\n",
                new long[] {10, 5},
                new long[] {3, 4},
                new long[] {0, 0});
    }

    /**
     * Chain of aliased sets: multiple local.set to the same variable in sequence, with old values
     * still on the stack.
     */
    @Test
    public void testChainedAliasing() {
        assertAllBackendsAgree(
                "(module\n"
                        + "  (func (export \"test\") (param $x i32) (result i32)\n"
                        + "    local.get $x\n"
                        + "    (local.set $x (i32.add (local.get $x) (i32.const 1)))\n"
                        + "    local.get $x\n"
                        + "    (local.set $x (i32.add (local.get $x) (i32.const 1)))\n"
                        + "    local.get $x\n"
                        + "    (local.set $x (i32.add (local.get $x) (i32.const 1)))\n"
                        + "    i32.add\n"
                        + "    i32.add\n"
                        + "  )\n"
                        + ")\n",
                new long[] {10},
                new long[] {0},
                new long[] {100});
    }

    /**
     * Aliasing across block boundaries: local.get in outer scope, local.set in inner block, use
     * old value after block.
     */
    @Test
    public void testAliasingAcrossBlocks() {
        assertAllBackendsAgree(
                "(module\n"
                        + "  (func (export \"test\") (param $x i32) (result i32)\n"
                        + "    local.get $x\n"
                        + "    (block\n"
                        + "      (local.set $x (i32.const 999))\n"
                        + "    )\n"
                        + "  )\n"
                        + ")\n",
                new long[] {42},
                new long[] {7},
                new long[] {0});
    }

    /**
     * Aliasing with memory store: push address from local, modify local, store using old address.
     * This pattern is common in WASI allocator code (dlmalloc).
     */
    @Test
    public void testAliasingWithMemoryStore() {
        assertAllBackendsAgree(
                "(module\n"
                        + "  (memory (export \"mem\") 1)\n"
                        + "  (func (export \"test\") (param $ptr i32) (param $val i32) (result"
                        + " i32)\n"
                        + "    local.get $ptr\n"
                        + "    (local.set $ptr (i32.add (local.get $ptr) (i32.const 100)))\n"
                        + "    local.get $val\n"
                        + "    i32.store\n"
                        + "    local.get $ptr\n"
                        + "    (i32.add (local.get $val) (i32.const 1))\n"
                        + "    i32.store\n"
                        + "    (i32.load (local.get $ptr))\n"
                        + "    (i32.load (i32.sub (local.get $ptr) (i32.const 100)))\n"
                        + "    i32.add\n"
                        + "  )\n"
                        + ")\n",
                new long[] {0, 42});
    }

    /**
     * Aliasing inside an if-else: local.get in outer scope, local.set inside conditional branch,
     * use old value after the if-else completes.
     */
    @Test
    public void testAliasingInsideIfElse() {
        assertAllBackendsAgree(
                "(module\n"
                        + "  (func (export \"test\") (param $x i32) (param $flag i32) (result"
                        + " i32)\n"
                        + "    local.get $x\n"
                        + "    (if (local.get $flag)\n"
                        + "      (then (local.set $x (i32.const 777)))\n"
                        + "      (else (local.set $x (i32.const 888)))\n"
                        + "    )\n"
                        + "  )\n"
                        + ")\n",
                new long[] {42, 1},
                new long[] {42, 0},
                new long[] {99, 1});
    }

    /**
     * Deeply nested aliasing: value pushed at outer level, modified in multiple nested blocks.
     * This pattern triggers multiple scope boundaries.
     */
    @Test
    public void testDeeplyNestedAliasing() {
        assertAllBackendsAgree(
                "(module\n"
                        + "  (func (export \"test\") (param $x i32) (result i32)\n"
                        + "    local.get $x\n"
                        + "    (block $b1\n"
                        + "      (block $b2\n"
                        + "        (block $b3\n"
                        + "          (local.set $x (i32.add (local.get $x) (i32.const 1000)))\n"
                        + "        )\n"
                        + "      )\n"
                        + "    )\n"
                        + "  )\n"
                        + ")\n",
                new long[] {5},
                new long[] {0},
                new long[] {999});
    }
}
