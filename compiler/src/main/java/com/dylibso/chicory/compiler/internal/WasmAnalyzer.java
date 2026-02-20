package com.dylibso.chicory.compiler.internal;

import static com.dylibso.chicory.compiler.internal.CompilerUtil.localType;
import static com.dylibso.chicory.compiler.internal.TypeStack.FUNCTION_SCOPE;
import static java.util.Collections.reverse;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toUnmodifiableList;

import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.AnnotatedInstruction;
import com.dylibso.chicory.wasm.types.CatchOpCode;
import com.dylibso.chicory.wasm.types.ExternalType;
import com.dylibso.chicory.wasm.types.FieldType;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionImport;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Global;
import com.dylibso.chicory.wasm.types.GlobalImport;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.TableImport;
import com.dylibso.chicory.wasm.types.ValType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

final class WasmAnalyzer {

    private final WasmModule module;
    private final List<ValType> globalTypes;
    private final List<FunctionType> functionTypes;
    private final List<ValType> tableTypes;
    private final int functionImports;

    public WasmAnalyzer(WasmModule module) {
        this.module = module;
        this.globalTypes = getGlobalTypes(module);
        this.functionTypes = getFunctionTypes(module);
        this.tableTypes = getTableTypes(module);
        this.functionImports = module.importSection().count(ExternalType.FUNCTION);
    }

    public List<ValType> globalTypes() {
        return globalTypes;
    }

    public List<FunctionType> functionTypes() {
        return functionTypes;
    }

    public static class TryCatchBlock {
        final AnnotatedInstruction ins;
        final long start;
        final long end;
        final long handler;
        final long after;
        final long[] afterCatch;

        public TryCatchBlock(
                AnnotatedInstruction ins,
                long start,
                long end,
                long handler,
                long after,
                long[] afterCatch) {
            this.ins = ins;
            this.start = start;
            this.end = end;
            this.handler = handler;
            this.after = after;
            this.afterCatch = afterCatch;
        }
    }

    @SuppressWarnings("checkstyle:modifiedcontrolvariable")
    public List<CompilerInstruction> analyze(int funcId) {
        var functionType = functionTypes.get(funcId);
        var body = module.codeSection().getFunctionBody(funcId - functionImports);
        var stack = new TypeStack(module.typeSection());
        int nextLabel = body.instructions().size();
        List<CompilerInstruction> result = new ArrayList<>();

        // find label targets
        Set<Integer> labels = new HashSet<>();

        HashMap<Integer, TryCatchBlock> tryCatchBlocks = new HashMap<>();

        for (int idx = body.instructions().size() - 1; idx >= 0; idx--) {
            AnnotatedInstruction ins = body.instructions().get(idx);

            if (ins.labelTrue() != AnnotatedInstruction.UNDEFINED_LABEL) {
                labels.add(ins.labelTrue());
            }
            if (ins.labelFalse() != AnnotatedInstruction.UNDEFINED_LABEL) {
                labels.add(ins.labelFalse());
            }
            labels.addAll(ins.labelTable());
            labels.addAll(
                    Optional.ofNullable(ins.catches())
                            .map(
                                    catches ->
                                            catches.stream()
                                                    .map(CatchOpCode.Catch::resolvedLabel)
                                                    .collect(Collectors.toList()))
                            .orElse(List.of()));

            if (ins.opcode() == OpCode.TRY_TABLE
                    && body.instructions().get(idx + 1).opcode() != OpCode.END) {
                var start = nextLabel++;
                var end = nextLabel++;
                var handle = nextLabel++;
                var after = nextLabel++;

                var afterCatchLabels = new long[ins.catches().size()];
                for (int i = 0; i < ins.catches().size(); i++) {
                    afterCatchLabels[i] = nextLabel++;
                }

                var block = new TryCatchBlock(ins, start, end, handle, after, afterCatchLabels);
                tryCatchBlocks.put(ins.address(), block);
                result.add(
                        new CompilerInstruction(
                                CompilerOpCode.TRY_CATCH_BLOCK,
                                block.start,
                                block.end,
                                block.handler));
            }
        }

        // implicit block for the function
        stack.enterScope(FUNCTION_SCOPE, FunctionType.of(List.of(), functionType.returns()));

        int exitBlockDepth = -1;
        for (int idx = 0; idx < body.instructions().size(); idx++) {
            AnnotatedInstruction ins = body.instructions().get(idx);

            if (labels.contains(idx)) {
                result.add(new CompilerInstruction(CompilerOpCode.LABEL, idx));
            }

            // skip instructions after unconditional control transfer
            if (exitBlockDepth >= 0) {
                if (ins.depth() > exitBlockDepth
                        || (ins.opcode() != OpCode.ELSE && ins.opcode() != OpCode.END)) {
                    continue;
                }

                exitBlockDepth = -1;
                if (ins.opcode() == OpCode.END) {
                    stack.scopeRestore();
                }
            }

            switch (ins.opcode()) {
                case NOP:
                    break;
                case UNREACHABLE:
                    exitBlockDepth = ins.depth();
                    result.add(new CompilerInstruction(CompilerOpCode.TRAP));
                    break;
                case BLOCK:
                case LOOP:
                    stack.enterScope(ins.scope(), blockType(ins));
                    break;
                case RETURN:
                    exitBlockDepth = ins.depth();
                    for (var type : reversed(functionType.returns())) {
                        stack.pop(type);
                    }
                    result.add(
                            new CompilerInstruction(
                                    CompilerOpCode.RETURN, ids(functionType.returns())));
                    break;
                case RETURN_CALL:
                    // The JVM does not support proper tail calls, so we desugar RETURN_CALL
                    // into a CALL + RETURN.

                    // [p*] -> [r*]
                    result.add(
                            new CompilerInstruction(
                                    CompilerOpCode.of(OpCode.CALL), ins.operands()));
                    updateStack(stack, functionTypes.get((int) ins.operand(0)));

                    exitBlockDepth = ins.depth();
                    for (var type : reversed(functionType.returns())) {
                        stack.pop(type);
                    }
                    result.add(
                            new CompilerInstruction(
                                    CompilerOpCode.RETURN, ids(functionType.returns())));
                    break;

                case RETURN_CALL_INDIRECT:
                    // The JVM does not support proper tail calls, so we desugar
                    // RETURN_CALL_INDIRECT into a CALL_INDIRECT + RETURN.

                    // [p* I32] -> [r*]
                    stack.pop(ValType.I32);
                    updateStack(stack, module.typeSection().getType((int) ins.operand(0)));
                    result.add(
                            new CompilerInstruction(
                                    CompilerOpCode.of(OpCode.CALL_INDIRECT), ins.operands()));

                    exitBlockDepth = ins.depth();
                    for (var type : reversed(functionType.returns())) {
                        stack.pop(type);
                    }

                    result.add(
                            new CompilerInstruction(
                                    CompilerOpCode.RETURN, ids(functionType.returns())));
                    break;

                case IF:
                    stack.pop(ValType.I32);
                    stack.enterScope(ins.scope(), blockType(ins));
                    // use the same starting stack sizes for both sides of the branch
                    if (body.instructions().get(ins.labelFalse() - 1).opcode() == OpCode.ELSE) {
                        stack.pushTypes();
                    }
                    result.add(new CompilerInstruction(CompilerOpCode.IFEQ, ins.labelFalse()));
                    break;
                case ELSE:
                    stack.popTypes();
                    result.add(new CompilerInstruction(CompilerOpCode.GOTO, ins.labelTrue()));
                    break;
                case BR:
                    exitBlockDepth = ins.depth();
                    unwindStack(functionType, body, ins, ins.labelTrue(), stack)
                            .ifPresent(result::add);
                    result.add(new CompilerInstruction(CompilerOpCode.GOTO, ins.labelTrue()));
                    break;
                case BR_IF:
                    stack.pop(ValType.I32);
                    var ifUnwind = unwindStack(functionType, body, ins, ins.labelTrue(), stack);
                    if (ifUnwind.isPresent()) {
                        result.add(new CompilerInstruction(CompilerOpCode.IFEQ, ins.labelFalse()));
                        result.add(ifUnwind.get());
                        result.add(new CompilerInstruction(CompilerOpCode.GOTO, ins.labelTrue()));
                    } else {
                        result.add(new CompilerInstruction(CompilerOpCode.IFNE, ins.labelTrue()));
                    }
                    break;
                case BR_TABLE:
                    exitBlockDepth = ins.depth();
                    stack.pop(ValType.I32);
                    // convert to jump if it only has a default
                    if (ins.labelTable().size() == 1) {
                        result.add(new CompilerInstruction(CompilerOpCode.DROP, ValType.I32.id()));
                        unwindStack(functionType, body, ins, ins.labelTable().get(0), stack)
                                .ifPresent(result::add);
                        result.add(
                                new CompilerInstruction(
                                        CompilerOpCode.GOTO, ins.labelTable().get(0)));
                        break;
                    }
                    // extract unique targets and generate unwind for each
                    List<CompilerInstruction> unwinds = new ArrayList<>();
                    Map<Integer, Integer> targets = new HashMap<>();
                    for (var target : ins.labelTable()) {
                        if (!targets.containsKey(target)) {
                            int label = target;
                            var unwind = unwindStack(functionType, body, ins, target, stack);
                            if (unwind.isPresent()) {
                                label = nextLabel;
                                nextLabel++;
                                unwinds.add(new CompilerInstruction(CompilerOpCode.LABEL, label));
                                unwinds.add(unwind.get());
                                unwinds.add(new CompilerInstruction(CompilerOpCode.GOTO, target));
                            }
                            targets.put(target, label);
                        }
                    }
                    // Note: some stricter compilers (e.g., Android ART) do not perform
                    // unboxing+widening
                    // int->long with a method reference, so instead of mapToLong(targets::get),
                    // we prefer an explicit lambda+cast.
                    long[] operands =
                            ins.labelTable().stream()
                                    .mapToLong(x -> (long) targets.get(x))
                                    .toArray();
                    result.add(new CompilerInstruction(CompilerOpCode.SWITCH, operands));
                    result.addAll(unwinds);
                    break;

                case TRY_TABLE:
                    {
                        // Is this an empty TRY_TABLE?
                        if (body.instructions().get(idx + 1).opcode() == OpCode.END) {
                            idx++; // skip the END instruction too
                            break;
                        }

                        stack.enterScope(ins.scope(), blockType(ins));
                        var tryCatchBlock = tryCatchBlocks.get(ins.address());
                        result.add(
                                new CompilerInstruction(CompilerOpCode.LABEL, tryCatchBlock.start));
                        break;
                    }

                case END:
                    // Check if this is the end of a TRY_TABLE block
                    if (ins.scope().opcode() == OpCode.TRY_TABLE) {
                        var tryCatchBlock = tryCatchBlocks.remove(ins.scope().address());

                        // Weird: sometimes we see END occur multiple times for
                        if (tryCatchBlock != null) {
                            analyzeTryCatchEnd(result, tryCatchBlock);
                        }
                    }
                    stack.exitScope(ins.scope());
                    break;

                case THROW:
                    {
                        // Add the THROW instruction
                        result.add(
                                new CompilerInstruction(
                                        CompilerOpCode.of(OpCode.THROW), ins.operands()));

                        // Mark as "unreachable" by emptying the stack for this block
                        exitBlockDepth = ins.depth();
                        break;
                    }
                case THROW_REF:
                    {
                        // Add instruction for THROW_REF
                        result.add(
                                new CompilerInstruction(
                                        CompilerOpCode.of(OpCode.THROW_REF), ins.operands()));

                        // Mark as "unreachable" by emptying the stack for this block
                        exitBlockDepth = ins.depth();
                        break;
                    }

                case CALL_REF:
                    {
                        // [p* funcref] -> [r*]
                        stack.popRef(); // funcref
                        int typeIdx = (int) ins.operand(0);
                        var callRefType = module.typeSection().getType(typeIdx);
                        updateStack(stack, callRefType);
                        result.add(
                                new CompilerInstruction(CompilerOpCode.CALL_REF, ins.operands()));
                        break;
                    }
                case RETURN_CALL_REF:
                    {
                        // Desugar into CALL_REF + RETURN
                        stack.popRef(); // funcref
                        int typeIdx = (int) ins.operand(0);
                        var callRefType = module.typeSection().getType(typeIdx);
                        updateStack(stack, callRefType);
                        result.add(
                                new CompilerInstruction(CompilerOpCode.CALL_REF, ins.operands()));

                        exitBlockDepth = ins.depth();
                        for (var type : reversed(functionType.returns())) {
                            stack.pop(type);
                        }
                        result.add(
                                new CompilerInstruction(
                                        CompilerOpCode.RETURN, ids(functionType.returns())));
                        break;
                    }
                case BR_ON_NULL:
                    {
                        // br_on_null: pop ref, if null -> branch (ref consumed),
                        //             else fall through with ref on stack
                        var ref = stack.peek();
                        stack.pop(ref);

                        // BR_ON_NULL_CHECK: DUPs ref, pushes 1 if null, 0 if not null
                        // JVM stack after check: [ref, 0_or_1]
                        result.add(new CompilerInstruction(CompilerOpCode.BR_ON_NULL_CHECK));

                        // IFEQ: pops boolean; if 0 (not null) -> jump to notNullLabel
                        // JVM stack after IFEQ: [ref]
                        var notNullLabel = nextLabel++;
                        result.add(new CompilerInstruction(CompilerOpCode.IFEQ, notNullLabel));

                        // null path: drop the DUP'd ref, then branch
                        result.add(new CompilerInstruction(CompilerOpCode.DROP, ref.id()));
                        var brUnwind = unwindStack(functionType, body, ins, ins.labelTrue(), stack);
                        brUnwind.ifPresent(result::add);
                        result.add(new CompilerInstruction(CompilerOpCode.GOTO, ins.labelTrue()));

                        // not-null path: ref stays on JVM stack from DUP
                        result.add(new CompilerInstruction(CompilerOpCode.LABEL, notNullLabel));

                        // Type stack: ref stays on stack for fall-through (not-null)
                        stack.push(ref);
                        break;
                    }
                case BR_ON_NON_NULL:
                    {
                        // br_on_non_null: pop ref, if non-null -> push ref and branch,
                        //                 else fall through with ref consumed
                        var ref = stack.peek();
                        stack.pop(ref);

                        // BR_ON_NON_NULL_CHECK: DUPs ref, pushes 1 if non-null, 0 if null
                        // JVM stack after check: [ref, 0_or_1]
                        result.add(new CompilerInstruction(CompilerOpCode.BR_ON_NON_NULL_CHECK));

                        // IFEQ: if 0 (null) -> jump to nullLabel
                        var nullLabel = nextLabel++;
                        result.add(new CompilerInstruction(CompilerOpCode.IFEQ, nullLabel));

                        // non-null path: ref is on JVM stack from DUP, branch with it
                        stack.push(ref);
                        var brUnwind = unwindStack(functionType, body, ins, ins.labelTrue(), stack);
                        stack.pop(ref);
                        brUnwind.ifPresent(result::add);
                        result.add(new CompilerInstruction(CompilerOpCode.GOTO, ins.labelTrue()));

                        // null path: drop the DUP'd ref
                        result.add(new CompilerInstruction(CompilerOpCode.LABEL, nullLabel));
                        result.add(new CompilerInstruction(CompilerOpCode.DROP, ref.id()));

                        // Type stack: ref is consumed on fall-through (null path)
                        break;
                    }
                case BR_ON_CAST:
                    {
                        // br_on_cast: pop ref, if cast matches -> branch with rt2,
                        //             else fall through with diffType
                        var ref = stack.peek();
                        stack.pop(ref);

                        var flags = (int) ins.operand(0);
                        var ht1 = (int) ins.operand(2);
                        var ht2 = (int) ins.operand(3);
                        boolean null1 = (flags & 1) != 0;
                        boolean null2 = (flags & 2) != 0;

                        // Types following the Validator pattern:
                        // rt2: target cast type (branch type for br_on_cast)
                        var rt2 = valType(null2 ? ValType.ID.RefNull : ValType.ID.Ref, ht2);
                        // diffType: fall-through type
                        var diffType =
                                valType(
                                        null2
                                                ? ValType.ID.Ref
                                                : (null1 ? ValType.ID.RefNull : ValType.ID.Ref),
                                        ht1);

                        // BR_ON_CAST_CHECK: DUPs ref, tests cast, pushes 1 if matches
                        // JVM stack after check: [ref, 0_or_1]
                        var sourceHeapType = ins.operandCount() > 4 ? (int) ins.operand(4) : ht1;
                        result.add(
                                new CompilerInstruction(
                                        CompilerOpCode.BR_ON_CAST_CHECK,
                                        null2 ? 1L : 0L,
                                        ht2,
                                        sourceHeapType));

                        // IFEQ: if 0 (no match) -> jump to noMatchLabel
                        var noMatchLabel = nextLabel++;
                        result.add(new CompilerInstruction(CompilerOpCode.IFEQ, noMatchLabel));

                        // match path: ref is on JVM stack from DUP, branch with rt2
                        stack.push(rt2);
                        var brUnwind = unwindStack(functionType, body, ins, ins.labelTrue(), stack);
                        stack.pop(rt2);
                        brUnwind.ifPresent(result::add);
                        result.add(new CompilerInstruction(CompilerOpCode.GOTO, ins.labelTrue()));

                        // no match path: ref stays on JVM stack for fall-through
                        result.add(new CompilerInstruction(CompilerOpCode.LABEL, noMatchLabel));

                        // Type stack: push diffType for fall-through
                        stack.push(diffType);
                        break;
                    }
                case BR_ON_CAST_FAIL:
                    {
                        // br_on_cast_fail: pop ref, if cast does NOT match -> branch with diffType,
                        //                  else fall through with rt2
                        var ref = stack.peek();
                        stack.pop(ref);

                        var flags = (int) ins.operand(0);
                        var ht1 = (int) ins.operand(2);
                        var ht2 = (int) ins.operand(3);
                        boolean null1 = (flags & 1) != 0;
                        boolean null2 = (flags & 2) != 0;

                        // Types following the Validator pattern:
                        var rt2 = valType(null2 ? ValType.ID.RefNull : ValType.ID.Ref, ht2);
                        var diffType =
                                valType(
                                        null2
                                                ? ValType.ID.Ref
                                                : (null1 ? ValType.ID.RefNull : ValType.ID.Ref),
                                        ht1);

                        // BR_ON_CAST_FAIL_CHECK: DUPs ref, tests, pushes 1 if NOT matching
                        // JVM stack after check: [ref, 0_or_1]
                        var sourceHeapType = ins.operandCount() > 4 ? (int) ins.operand(4) : ht1;
                        result.add(
                                new CompilerInstruction(
                                        CompilerOpCode.BR_ON_CAST_FAIL_CHECK,
                                        null2 ? 1L : 0L,
                                        ht2,
                                        sourceHeapType));

                        // IFEQ: if 0 (matches) -> jump to matchLabel (fall-through)
                        var matchLabel = nextLabel++;
                        result.add(new CompilerInstruction(CompilerOpCode.IFEQ, matchLabel));

                        // no match path: ref is on JVM stack from DUP, branch with diffType
                        stack.push(diffType);
                        var brUnwind = unwindStack(functionType, body, ins, ins.labelTrue(), stack);
                        stack.pop(diffType);
                        brUnwind.ifPresent(result::add);
                        result.add(new CompilerInstruction(CompilerOpCode.GOTO, ins.labelTrue()));

                        // match path: ref stays on JVM stack for fall-through
                        result.add(new CompilerInstruction(CompilerOpCode.LABEL, matchLabel));

                        // Type stack: push rt2 for fall-through (cast succeeded)
                        stack.push(rt2);
                        break;
                    }

                case SELECT:
                case SELECT_T:
                    // [t t I32] -> [t]
                    stack.pop(ValType.I32);
                    var selectType = stack.peek();
                    stack.pop(selectType);
                    stack.pop(selectType);
                    stack.push(selectType);
                    result.add(new CompilerInstruction(CompilerOpCode.SELECT, selectType.id()));
                    break;
                case DROP:
                    // [t] -> []
                    var dropType = stack.peek();
                    stack.pop(dropType);
                    result.add(new CompilerInstruction(CompilerOpCode.DROP, dropType.id()));
                    break;
                case LOCAL_TEE:
                    // [t] -> [t]
                    var teeType = stack.peek();
                    stack.pop(teeType);
                    stack.push(teeType);
                    long[] teeOperands = {ins.operand(0), teeType.id()};
                    result.add(new CompilerInstruction(CompilerOpCode.LOCAL_TEE, teeOperands));
                    break;
                case CAST_TEST:
                    {
                        // ref.cast: [ref] -> [ref(heapType)]
                        var srcType = stack.peek();
                        stack.popRef();
                        var heapType = (int) ins.operand(0);
                        stack.push(valType(ValType.ID.Ref, heapType));
                        long[] castOperands = {heapType, srcType.typeIdx()};
                        result.add(new CompilerInstruction(CompilerOpCode.CAST_TEST, castOperands));
                        break;
                    }
                case CAST_TEST_NULL:
                    {
                        // ref.cast null: [ref] -> [refnull(heapType)]
                        var srcType = stack.peek();
                        stack.popRef();
                        var heapType = (int) ins.operand(0);
                        stack.push(valType(ValType.ID.RefNull, heapType));
                        long[] castNullOperands = {heapType, srcType.typeIdx()};
                        result.add(
                                new CompilerInstruction(
                                        CompilerOpCode.CAST_TEST_NULL, castNullOperands));
                        break;
                    }
                default:
                    analyzeSimple(result, stack, ins, functionType, body);
            }
        }

        // implicit return at end of function
        for (var type : reversed(functionType.returns())) {
            stack.pop(type);
        }
        result.add(new CompilerInstruction(CompilerOpCode.RETURN, ids(functionType.returns())));

        stack.verifyEmpty();
        return result;
    }

    private static void analyzeTryCatchEnd(
            List<CompilerInstruction> result, TryCatchBlock tryCatchBlock) {

        // Mark the end of the try block
        result.add(new CompilerInstruction(CompilerOpCode.LABEL, tryCatchBlock.end));

        // Jump over the exception handler if since no exception was thrown
        result.add(new CompilerInstruction(CompilerOpCode.GOTO, tryCatchBlock.after));

        // Mark the start of the exception handler
        result.add(new CompilerInstruction(CompilerOpCode.LABEL, tryCatchBlock.handler));

        // store the exception in a temporary slot
        result.add(new CompilerInstruction(CompilerOpCode.CATCH_START));

        for (int i = 0; i < tryCatchBlock.ins.catches().size(); i++) {
            var catchCondition = tryCatchBlock.ins.catches().get(i);
            long afterCatchLabel = tryCatchBlock.afterCatch[i];

            switch (catchCondition.opcode()) {
                case CATCH:
                    result.add(
                            new CompilerInstruction(
                                    CompilerOpCode.CATCH_COMPARE_TAG, catchCondition.tag()));
                    result.add(new CompilerInstruction(CompilerOpCode.IFEQ, afterCatchLabel));
                    result.add(
                            new CompilerInstruction(
                                    CompilerOpCode.CATCH_UNBOX_PARAMS, catchCondition.tag()));
                    break;
                case CATCH_REF:
                    result.add(
                            new CompilerInstruction(
                                    CompilerOpCode.CATCH_COMPARE_TAG, catchCondition.tag()));
                    result.add(new CompilerInstruction(CompilerOpCode.IFEQ, afterCatchLabel));
                    result.add(
                            new CompilerInstruction(
                                    CompilerOpCode.CATCH_UNBOX_PARAMS, catchCondition.tag()));
                    result.add(new CompilerInstruction(CompilerOpCode.CATCH_REGISTER_EXCEPTION));
                    break;
                case CATCH_ALL:
                    // Always matches, no tag comparison needed
                    break;
                case CATCH_ALL_REF:
                    // Always matches, register exception
                    // and push its index
                    result.add(new CompilerInstruction(CompilerOpCode.CATCH_REGISTER_EXCEPTION));
                    break;
            }
            result.add(
                    new CompilerInstruction(CompilerOpCode.GOTO, catchCondition.resolvedLabel()));
            result.add(new CompilerInstruction(CompilerOpCode.LABEL, afterCatchLabel));
        }

        // Default case: re-throw the exception
        result.add(new CompilerInstruction(CompilerOpCode.CATCH_END));

        // Mark the end of exception handler
        result.add(new CompilerInstruction(CompilerOpCode.LABEL, tryCatchBlock.after));
    }

    private void analyzeSimple(
            List<CompilerInstruction> out,
            TypeStack stack,
            Instruction ins,
            FunctionType functionType,
            FunctionBody body) {
        switch (ins.opcode()) {
            case I32_CLZ:
            case I32_CTZ:
            case I32_EQZ:
            case I32_EXTEND_16_S:
            case I32_EXTEND_8_S:
            case I32_LOAD16_S:
            case I32_LOAD16_U:
            case I32_LOAD8_S:
            case I32_LOAD8_U:
            case I32_LOAD:
            case I32_POPCNT:
            case MEMORY_GROW:
            case I32_ATOMIC_LOAD:
            case I32_ATOMIC_LOAD8_U:
            case I32_ATOMIC_LOAD16_U:
                // [I32] -> [I32]
                stack.pop(ValType.I32);
                stack.push(ValType.I32);
                break;
            case F32_CONVERT_I32_S:
            case F32_CONVERT_I32_U:
            case F32_LOAD:
            case F32_REINTERPRET_I32:
                // [I32] -> [F32]
                stack.pop(ValType.I32);
                stack.push(ValType.F32);
                break;
            case F32_ABS:
            case F32_CEIL:
            case F32_FLOOR:
            case F32_NEAREST:
            case F32_NEG:
            case F32_SQRT:
            case F32_TRUNC:
                // [F32] -> [F32]
                stack.pop(ValType.F32);
                stack.push(ValType.F32);
                break;
            case I32_REINTERPRET_F32:
            case I32_TRUNC_F32_S:
            case I32_TRUNC_F32_U:
            case I32_TRUNC_SAT_F32_S:
            case I32_TRUNC_SAT_F32_U:
                // [F32] -> [I32]
                stack.pop(ValType.F32);
                stack.push(ValType.I32);
                break;
            case I32_WRAP_I64:
            case I64_EQZ:
                // [I64] -> [I32]
                stack.pop(ValType.I64);
                stack.push(ValType.I32);
                break;
            case F32_CONVERT_I64_S:
            case F32_CONVERT_I64_U:
                // [I64] -> [F32]
                stack.pop(ValType.I64);
                stack.push(ValType.F32);
                break;
            case F32_DEMOTE_F64:
                // [F64] -> [F32]
                stack.pop(ValType.F64);
                stack.push(ValType.F32);
                break;
            case I32_TRUNC_F64_S:
            case I32_TRUNC_F64_U:
            case I32_TRUNC_SAT_F64_S:
            case I32_TRUNC_SAT_F64_U:
                // [F64] -> [I32]
                stack.pop(ValType.F64);
                stack.push(ValType.I32);
                break;
            case I32_ADD:
            case I32_AND:
            case I32_DIV_S:
            case I32_DIV_U:
            case I32_EQ:
            case I32_GE_S:
            case I32_GE_U:
            case I32_GT_S:
            case I32_GT_U:
            case I32_LE_S:
            case I32_LE_U:
            case I32_LT_S:
            case I32_LT_U:
            case I32_MUL:
            case I32_NE:
            case I32_OR:
            case I32_REM_S:
            case I32_REM_U:
            case I32_ROTL:
            case I32_ROTR:
            case I32_SHL:
            case I32_SHR_S:
            case I32_SHR_U:
            case I32_SUB:
            case I32_XOR:
            case I32_ATOMIC_RMW_ADD:
            case I32_ATOMIC_RMW_SUB:
            case I32_ATOMIC_RMW_AND:
            case I32_ATOMIC_RMW_OR:
            case I32_ATOMIC_RMW_XOR:
            case I32_ATOMIC_RMW_XCHG:
            case MEM_ATOMIC_NOTIFY:
            case I32_ATOMIC_RMW8_ADD_U:
            case I32_ATOMIC_RMW8_SUB_U:
            case I32_ATOMIC_RMW8_AND_U:
            case I32_ATOMIC_RMW8_OR_U:
            case I32_ATOMIC_RMW8_XOR_U:
            case I32_ATOMIC_RMW8_XCHG_U:
            case I32_ATOMIC_RMW16_ADD_U:
            case I32_ATOMIC_RMW16_SUB_U:
            case I32_ATOMIC_RMW16_AND_U:
            case I32_ATOMIC_RMW16_OR_U:
            case I32_ATOMIC_RMW16_XOR_U:
            case I32_ATOMIC_RMW16_XCHG_U:
                // [I32 I32] -> [I32]
                stack.pop(ValType.I32);
                stack.pop(ValType.I32);
                stack.push(ValType.I32);
                break;
            case I64_EQ:
            case I64_GE_S:
            case I64_GE_U:
            case I64_GT_S:
            case I64_GT_U:
            case I64_LE_S:
            case I64_LE_U:
            case I64_LT_S:
            case I64_LT_U:
            case I64_NE:
                // [I64 I64] -> [I32]
                stack.pop(ValType.I64);
                stack.pop(ValType.I64);
                stack.push(ValType.I32);
                break;
            case F32_ADD:
            case F32_COPYSIGN:
            case F32_DIV:
            case F32_MAX:
            case F32_MIN:
            case F32_MUL:
            case F32_SUB:
                // [F32 F32] -> [F32]
                stack.pop(ValType.F32);
                stack.pop(ValType.F32);
                stack.push(ValType.F32);
                break;
            case F32_EQ:
            case F32_GE:
            case F32_GT:
            case F32_LE:
            case F32_LT:
            case F32_NE:
                // [F32 F32] -> [I32]
                stack.pop(ValType.F32);
                stack.pop(ValType.F32);
                stack.push(ValType.I32);
                break;
            case F64_EQ:
            case F64_GE:
            case F64_GT:
            case F64_LE:
            case F64_LT:
            case F64_NE:
                // [F64 F64] -> [I32]
                stack.pop(ValType.F64);
                stack.pop(ValType.F64);
                stack.push(ValType.I32);
                break;
            case I64_CLZ:
            case I64_CTZ:
            case I64_EXTEND_16_S:
            case I64_EXTEND_32_S:
            case I64_EXTEND_8_S:
            case I64_POPCNT:
                // [I64] -> [I64]
                stack.pop(ValType.I64);
                stack.push(ValType.I64);
                break;
            case I64_REINTERPRET_F64:
            case I64_TRUNC_F64_S:
            case I64_TRUNC_F64_U:
            case I64_TRUNC_SAT_F64_S:
            case I64_TRUNC_SAT_F64_U:
                // [F64] -> [I64]
                stack.pop(ValType.F64);
                stack.push(ValType.I64);
                break;
            case F64_TRUNC:
            case F64_SQRT:
            case F64_NEAREST:
            case F64_ABS:
            case F64_CEIL:
            case F64_FLOOR:
            case F64_NEG:
                // [F64] -> [F64]
                stack.pop(ValType.F64);
                stack.push(ValType.F64);
                break;
            case F64_CONVERT_I64_S:
            case F64_CONVERT_I64_U:
            case F64_REINTERPRET_I64:
                // [I64] -> [F64]
                stack.pop(ValType.I64);
                stack.push(ValType.F64);
                break;
            case I64_EXTEND_I32_S:
            case I64_EXTEND_I32_U:
            case I64_LOAD16_S:
            case I64_LOAD16_U:
            case I64_LOAD32_S:
            case I64_LOAD32_U:
            case I64_LOAD8_S:
            case I64_LOAD8_U:
            case I64_LOAD:
            case I64_ATOMIC_LOAD:
            case I64_ATOMIC_LOAD8_U:
            case I64_ATOMIC_LOAD16_U:
            case I64_ATOMIC_LOAD32_U:
                // [I32] -> [I64]
                stack.pop(ValType.I32);
                stack.push(ValType.I64);
                break;
            case I64_TRUNC_F32_S:
            case I64_TRUNC_F32_U:
            case I64_TRUNC_SAT_F32_S:
            case I64_TRUNC_SAT_F32_U:
                // [F32] -> [I64]
                stack.pop(ValType.F32);
                stack.push(ValType.I64);
                break;
            case F64_CONVERT_I32_S:
            case F64_CONVERT_I32_U:
            case F64_LOAD:
                // [I32] -> [F64]
                stack.pop(ValType.I32);
                stack.push(ValType.F64);
                break;
            case F64_PROMOTE_F32:
                // [F32] -> [F64]
                stack.pop(ValType.F32);
                stack.push(ValType.F64);
                break;
            case I64_ADD:
            case I64_AND:
            case I64_DIV_S:
            case I64_DIV_U:
            case I64_MUL:
            case I64_OR:
            case I64_REM_S:
            case I64_REM_U:
            case I64_ROTL:
            case I64_ROTR:
            case I64_SHL:
            case I64_SHR_S:
            case I64_SHR_U:
            case I64_SUB:
            case I64_XOR:
                // [I64 I64] -> [I64]
                stack.pop(ValType.I64);
                stack.pop(ValType.I64);
                stack.push(ValType.I64);
                break;
            case F64_ADD:
            case F64_COPYSIGN:
            case F64_DIV:
            case F64_MAX:
            case F64_MIN:
            case F64_MUL:
            case F64_SUB:
                // [F64 F64] -> [F64]
                stack.pop(ValType.F64);
                stack.pop(ValType.F64);
                stack.push(ValType.F64);
                break;
            case I32_STORE:
            case I32_STORE8:
            case I32_STORE16:
            case I32_ATOMIC_STORE:
            case I32_ATOMIC_STORE8:
            case I32_ATOMIC_STORE16:
                // [I32 I32] -> []
                stack.pop(ValType.I32);
                stack.pop(ValType.I32);
                break;
            case F32_STORE:
                // [I32 F32] -> []
                stack.pop(ValType.F32);
                stack.pop(ValType.I32);
                break;
            case I64_STORE:
            case I64_STORE8:
            case I64_STORE16:
            case I64_STORE32:
            case I64_ATOMIC_STORE:
            case I64_ATOMIC_STORE8:
            case I64_ATOMIC_STORE16:
            case I64_ATOMIC_STORE32:
                // [I32 I64] -> []
                stack.pop(ValType.I64);
                stack.pop(ValType.I32);
                break;
            case F64_STORE:
                // [I32 F64] -> []
                stack.pop(ValType.F64);
                stack.pop(ValType.I32);
                break;
            case I32_CONST:
            case MEMORY_SIZE:
            case TABLE_SIZE:
                // [] -> [I32]
                stack.push(ValType.I32);
                break;
            case F32_CONST:
                // [] -> [F32]
                stack.push(ValType.F32);
                break;
            case I64_CONST:
                // [] -> [I64]
                stack.push(ValType.I64);
                break;
            case F64_CONST:
                // [] -> [F64]
                stack.push(ValType.F64);
                break;
            case REF_FUNC:
                {
                    // [] -> [ref]
                    // ref.func produces a non-nullable reference with the function's type
                    int funcIdx = (int) ins.operand(0);
                    int typeIdx = functionTypeIndex(funcIdx);
                    stack.push(valType(ValType.ID.Ref, typeIdx));
                    break;
                }
            case REF_NULL:
                // [] -> [ref]
                stack.push(
                        ValType.builder()
                                .withOpcode(ValType.ID.RefNull)
                                .withTypeIdx((int) ins.operand(0))
                                .build()
                                .resolve(module.typeSection()));
                break;
            case REF_IS_NULL:
                // [ref] -> [I32]
                stack.popRef();
                stack.push(ValType.I32);
                break;
            case MEMORY_COPY:
            case MEMORY_FILL:
            case MEMORY_INIT:
            case TABLE_COPY:
            case TABLE_INIT:
                // [I32 I32 I32] -> []
                stack.pop(ValType.I32);
                stack.pop(ValType.I32);
                stack.pop(ValType.I32);
                break;
            case TABLE_FILL:
                // [I32 ref I32] -> []
                stack.pop(ValType.I32);
                stack.pop(stack.peek());
                stack.pop(ValType.I32);
                break;
            case TABLE_GET:
                // [I32] -> [ref]
                stack.pop(ValType.I32);
                stack.push(tableTypes.get((int) ins.operand(0)));
                break;
            case TABLE_GROW:
                // [ref I32] -> [I32]
                stack.pop(ValType.I32);
                stack.pop(tableTypes.get((int) ins.operand(0)));
                stack.push(ValType.I32);
                break;
            case TABLE_SET:
                // [I32 ref] -> []
                stack.pop(tableTypes.get((int) ins.operand(0)));
                stack.pop(ValType.I32);
                break;
            case CALL:
                // [p*] -> [r*]
                updateStack(stack, functionTypes.get((int) ins.operand(0)));
                break;
            case CALL_INDIRECT:
                // [p* I32] -> [r*]
                stack.pop(ValType.I32);
                updateStack(stack, module.typeSection().getType((int) ins.operand(0)));
                break;
            case GLOBAL_SET:
            case LOCAL_SET:
                // [t] -> []
                stack.pop(stack.peek());
                break;
            case LOCAL_GET:
                // [] -> [t]
                stack.push(localType(functionType, body, (int) ins.operand(0)));
                break;
            case GLOBAL_GET:
                // [] -> [t]
                stack.push(globalTypes.get((int) ins.operand(0)));
                break;
            case ATOMIC_FENCE:
            case DATA_DROP:
            case ELEM_DROP:
                // [] -> []
                break;
            case I32_ATOMIC_RMW_CMPXCHG:
            case I32_ATOMIC_RMW8_CMPXCHG_U:
            case I32_ATOMIC_RMW16_CMPXCHG_U:
                // [I32 I32 I32] -> [I32]
                stack.pop(ValType.I32);
                stack.pop(ValType.I32);
                stack.pop(ValType.I32);
                stack.push(ValType.I32);
                break;
            case I64_ATOMIC_RMW_ADD:
            case I64_ATOMIC_RMW_SUB:
            case I64_ATOMIC_RMW_AND:
            case I64_ATOMIC_RMW_OR:
            case I64_ATOMIC_RMW_XOR:
            case I64_ATOMIC_RMW_XCHG:
            case I64_ATOMIC_RMW8_ADD_U:
            case I64_ATOMIC_RMW8_SUB_U:
            case I64_ATOMIC_RMW8_AND_U:
            case I64_ATOMIC_RMW8_OR_U:
            case I64_ATOMIC_RMW8_XOR_U:
            case I64_ATOMIC_RMW8_XCHG_U:
            case I64_ATOMIC_RMW16_ADD_U:
            case I64_ATOMIC_RMW16_SUB_U:
            case I64_ATOMIC_RMW16_AND_U:
            case I64_ATOMIC_RMW16_OR_U:
            case I64_ATOMIC_RMW16_XOR_U:
            case I64_ATOMIC_RMW16_XCHG_U:
            case I64_ATOMIC_RMW32_ADD_U:
            case I64_ATOMIC_RMW32_SUB_U:
            case I64_ATOMIC_RMW32_AND_U:
            case I64_ATOMIC_RMW32_OR_U:
            case I64_ATOMIC_RMW32_XOR_U:
            case I64_ATOMIC_RMW32_XCHG_U:
                // [I32 I64] -> [I64]
                stack.pop(ValType.I64);
                stack.pop(ValType.I32);
                stack.push(ValType.I64);
                break;
            case I64_ATOMIC_RMW_CMPXCHG:
            case I64_ATOMIC_RMW8_CMPXCHG_U:
            case I64_ATOMIC_RMW16_CMPXCHG_U:
            case I64_ATOMIC_RMW32_CMPXCHG_U:
                // [I32 I64 I64] -> [I64]
                stack.pop(ValType.I64);
                stack.pop(ValType.I64);
                stack.pop(ValType.I32);
                stack.push(ValType.I64);
                break;
            case MEM_ATOMIC_WAIT32:
                // [I32 I32 I64] -> [I32]
                stack.pop(ValType.I64);
                stack.pop(ValType.I32);
                stack.pop(ValType.I32);
                stack.push(ValType.I32);
                break;
            case MEM_ATOMIC_WAIT64:
                // [I32 I64 I64] -> [I32]
                stack.pop(ValType.I64);
                stack.pop(ValType.I64);
                stack.pop(ValType.I32);
                stack.push(ValType.I32);
                break;
            // ====== GC ======
            case REF_EQ:
                // [eqref eqref] -> [I32]
                stack.pop(ValType.EqRef);
                stack.pop(ValType.EqRef);
                stack.push(ValType.I32);
                break;
            case REF_AS_NON_NULL:
                {
                    // [ref] -> [ref]
                    var rt = stack.peek();
                    stack.popRef();
                    stack.push(valType(ValType.ID.Ref, rt.typeIdx()));
                    break;
                }
            case STRUCT_NEW:
                {
                    var typeIdx = (int) ins.operand(0);
                    var st = module.typeSection().getSubType(typeIdx).compType().structType();
                    for (int i = st.fieldTypes().length - 1; i >= 0; i--) {
                        stack.pop(unpackFieldType(st.fieldTypes()[i]));
                    }
                    stack.push(valType(ValType.ID.Ref, typeIdx));
                    break;
                }
            case STRUCT_NEW_DEFAULT:
                {
                    var typeIdx = (int) ins.operand(0);
                    stack.push(valType(ValType.ID.Ref, typeIdx));
                    break;
                }
            case STRUCT_GET:
                {
                    var typeIdx = (int) ins.operand(0);
                    var fieldIdx = (int) ins.operand(1);
                    stack.pop(valType(ValType.ID.RefNull, typeIdx));
                    var st = module.typeSection().getSubType(typeIdx).compType().structType();
                    stack.push(unpackFieldType(st.fieldTypes()[fieldIdx]));
                    break;
                }
            case STRUCT_GET_S:
            case STRUCT_GET_U:
                {
                    var typeIdx = (int) ins.operand(0);
                    stack.pop(valType(ValType.ID.RefNull, typeIdx));
                    stack.push(ValType.I32);
                    break;
                }
            case STRUCT_SET:
                {
                    var typeIdx = (int) ins.operand(0);
                    var fieldIdx = (int) ins.operand(1);
                    var st = module.typeSection().getSubType(typeIdx).compType().structType();
                    stack.pop(unpackFieldType(st.fieldTypes()[fieldIdx]));
                    stack.pop(valType(ValType.ID.RefNull, typeIdx));
                    break;
                }
            case ARRAY_NEW:
                {
                    var typeIdx = (int) ins.operand(0);
                    stack.pop(ValType.I32); // len
                    var at = module.typeSection().getSubType(typeIdx).compType().arrayType();
                    stack.pop(unpackFieldType(at.fieldType())); // initVal
                    stack.push(valType(ValType.ID.Ref, typeIdx));
                    break;
                }
            case ARRAY_NEW_DEFAULT:
                {
                    var typeIdx = (int) ins.operand(0);
                    stack.pop(ValType.I32); // len
                    stack.push(valType(ValType.ID.Ref, typeIdx));
                    break;
                }
            case ARRAY_NEW_FIXED:
                {
                    var typeIdx = (int) ins.operand(0);
                    var len = (int) ins.operand(1);
                    var at = module.typeSection().getSubType(typeIdx).compType().arrayType();
                    var elemType = unpackFieldType(at.fieldType());
                    for (int i = 0; i < len; i++) {
                        stack.pop(elemType);
                    }
                    stack.push(valType(ValType.ID.Ref, typeIdx));
                    break;
                }
            case ARRAY_NEW_DATA:
            case ARRAY_NEW_ELEM:
                {
                    var typeIdx = (int) ins.operand(0);
                    stack.pop(ValType.I32); // len
                    stack.pop(ValType.I32); // offset
                    stack.push(valType(ValType.ID.Ref, typeIdx));
                    break;
                }
            case ARRAY_GET:
                {
                    var typeIdx = (int) ins.operand(0);
                    stack.pop(ValType.I32); // idx
                    stack.pop(valType(ValType.ID.RefNull, typeIdx)); // arrayref
                    var at = module.typeSection().getSubType(typeIdx).compType().arrayType();
                    stack.push(unpackFieldType(at.fieldType()));
                    break;
                }
            case ARRAY_GET_S:
            case ARRAY_GET_U:
                {
                    var typeIdx = (int) ins.operand(0);
                    stack.pop(ValType.I32); // idx
                    stack.pop(valType(ValType.ID.RefNull, typeIdx)); // arrayref
                    stack.push(ValType.I32);
                    break;
                }
            case ARRAY_SET:
                {
                    var typeIdx = (int) ins.operand(0);
                    var at = module.typeSection().getSubType(typeIdx).compType().arrayType();
                    stack.pop(unpackFieldType(at.fieldType())); // val
                    stack.pop(ValType.I32); // idx
                    stack.pop(valType(ValType.ID.RefNull, typeIdx)); // arrayref
                    break;
                }
            case ARRAY_LEN:
                stack.pop(ValType.ArrayRef); // arrayref
                stack.push(ValType.I32);
                break;
            case ARRAY_FILL:
                {
                    var typeIdx = (int) ins.operand(0);
                    stack.pop(ValType.I32); // len
                    var at = module.typeSection().getSubType(typeIdx).compType().arrayType();
                    stack.pop(unpackFieldType(at.fieldType())); // val
                    stack.pop(ValType.I32); // offset
                    stack.pop(valType(ValType.ID.RefNull, typeIdx)); // arrayref
                    break;
                }
            case ARRAY_COPY:
                {
                    // [dstRef, dstOff, srcRef, srcOff, len]
                    var dstTypeIdx = (int) ins.operand(0);
                    var srcTypeIdx = (int) ins.operand(1);
                    stack.pop(ValType.I32); // len
                    stack.pop(ValType.I32); // srcOff
                    stack.pop(valType(ValType.ID.RefNull, srcTypeIdx)); // srcRef
                    stack.pop(ValType.I32); // dstOff
                    stack.pop(valType(ValType.ID.RefNull, dstTypeIdx)); // dstRef
                    break;
                }
            case ARRAY_INIT_DATA:
            case ARRAY_INIT_ELEM:
                {
                    // [ref, dstOff, srcOff, len]
                    var typeIdx = (int) ins.operand(0);
                    stack.pop(ValType.I32); // len
                    stack.pop(ValType.I32); // srcOff
                    stack.pop(ValType.I32); // dstOff
                    stack.pop(valType(ValType.ID.RefNull, typeIdx)); // ref
                    break;
                }
            case REF_TEST:
            case REF_TEST_NULL:
                {
                    stack.popRef();
                    stack.push(ValType.I32);
                    break;
                }
            case REF_I31:
                stack.pop(ValType.I32);
                stack.push(
                        ValType.builder()
                                .withOpcode(ValType.ID.Ref)
                                .withTypeIdx(ValType.TypeIdxCode.I31.code())
                                .build());
                break;
            case I31_GET_S:
            case I31_GET_U:
                stack.popRef();
                stack.push(ValType.I32);
                break;
            case ANY_CONVERT_EXTERN:
                stack.popRef();
                stack.push(
                        ValType.builder()
                                .withOpcode(ValType.ID.Ref)
                                .withTypeIdx(ValType.TypeIdxCode.ANY.code())
                                .build());
                break;
            case EXTERN_CONVERT_ANY:
                stack.popRef();
                stack.push(
                        ValType.builder()
                                .withOpcode(ValType.ID.Ref)
                                .withTypeIdx(ValType.TypeIdxCode.EXTERN.code())
                                .build());
                break;
            default:
                throw new ChicoryException("Unhandled opcode: " + ins.opcode());
        }
        out.add(new CompilerInstruction(CompilerOpCode.of(ins.opcode()), ins.operands()));
    }

    private static void updateStack(TypeStack stack, FunctionType functionType) {
        for (ValType type : reversed(functionType.params())) {
            stack.pop(type);
        }
        for (ValType type : functionType.returns()) {
            stack.push(type);
        }
    }

    private Optional<CompilerInstruction> unwindStack(
            FunctionType functionType,
            FunctionBody body,
            AnnotatedInstruction ins,
            int label,
            TypeStack stack) {

        boolean forward = true;

        var target = body.instructions().get(label);
        if (target.address() <= ins.address()) {
            target = body.instructions().get(label - 1);
            forward = false;
        }
        var scope = target.scope();

        FunctionType blockType;
        if (scope.opcode() == OpCode.END) {
            // special scope for the function's implicit block
            scope = FUNCTION_SCOPE;
            blockType = functionType;
        } else {
            blockType = blockType(scope);
        }

        var types = forward ? blockType.returns() : blockType.params();
        int keep = types.size();

        // for a backward jump, the initial loop parameters are dropped
        int drop = stack.types().size() - stack.scopeStackSize(scope);

        // do not drop the return values for a forward jump
        if (forward) {
            drop -= types.size();
        }

        if (drop <= 0) {
            return Optional.empty();
        }

        // operands: [drop, drop_types..., keep_types...]
        var operands = LongStream.builder();
        operands.add(drop);

        List<ValType> dropKeepTypes =
                stack.types().stream().limit(drop + keep).collect(toCollection(ArrayList::new));
        reverse(dropKeepTypes);
        dropKeepTypes.stream().mapToLong(ValType::id).forEach(operands::add);

        return Optional.of(
                new CompilerInstruction(CompilerOpCode.DROP_KEEP, operands.build().toArray()));
    }

    private FunctionType blockType(Instruction ins) {
        var typeId = ins.operand(0);
        if (typeId == 0x40) {
            return FunctionType.empty();
        }
        if (ValType.isValid(typeId)) {
            return FunctionType.returning(
                    ValType.builder().fromId(typeId).build().resolve(module.typeSection()));
        }
        return module.typeSection().getType((int) typeId);
    }

    private static List<ValType> getGlobalTypes(WasmModule module) {
        var importedGlobals =
                module.importSection().stream()
                        .filter(GlobalImport.class::isInstance)
                        .map(GlobalImport.class::cast)
                        .map(GlobalImport::type);

        var globals = module.globalSection();
        var moduleGlobals =
                IntStream.range(0, globals.globalCount())
                        .mapToObj(globals::getGlobal)
                        .map(Global::valueType);

        return Stream.concat(importedGlobals, moduleGlobals).collect(toUnmodifiableList());
    }

    private static List<FunctionType> getFunctionTypes(WasmModule module) {
        var importedFunctions =
                module.importSection().stream()
                        .filter(FunctionImport.class::isInstance)
                        .map(FunctionImport.class::cast)
                        .map(function -> module.typeSection().getType(function.typeIndex()));

        var functions = module.functionSection();
        var moduleFunctions =
                IntStream.range(0, functions.functionCount())
                        .mapToObj(i -> functions.getFunctionType(i, module.typeSection()));

        return Stream.concat(importedFunctions, moduleFunctions).collect(toUnmodifiableList());
    }

    private static List<ValType> getTableTypes(WasmModule module) {
        var importedTables =
                module.importSection().stream()
                        .filter(TableImport.class::isInstance)
                        .map(TableImport.class::cast)
                        .map(TableImport::entryType);

        var tables = module.tableSection();
        var moduleTables =
                IntStream.range(0, tables.tableCount())
                        .mapToObj(tables::getTable)
                        .map(Table::elementType);

        return Stream.concat(importedTables, moduleTables).collect(toUnmodifiableList());
    }

    private static <T> List<T> reversed(List<T> list) {
        if (list.size() <= 1) {
            return list;
        }
        List<T> reversed = new ArrayList<>(list);
        reverse(reversed);
        return reversed;
    }

    private static long[] ids(List<ValType> types) {
        return types.stream().mapToLong(ValType::id).toArray();
    }

    int functionTypeIndex(int funcIdx) {
        if (funcIdx < functionImports) {
            var imports =
                    module.importSection().stream()
                            .filter(FunctionImport.class::isInstance)
                            .map(FunctionImport.class::cast)
                            .collect(Collectors.toList());
            return imports.get(funcIdx).typeIndex();
        }
        return module.functionSection().getFunctionType(funcIdx - functionImports);
    }

    private ValType valType(int opcode, int typeIdx) {
        return ValType.builder()
                .withOpcode(opcode)
                .withTypeIdx(typeIdx)
                .build()
                .resolve(module.typeSection());
    }

    private static ValType unpackFieldType(FieldType ft) {
        if (ft.storageType().packedType() != null) {
            return ValType.I32;
        }
        return ft.storageType().valType();
    }
}
