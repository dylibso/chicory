package com.dylibso.chicory.source.compiler.internal;

import static com.dylibso.chicory.source.compiler.internal.CompilerUtil.localType;
import static com.dylibso.chicory.source.compiler.internal.TypeStack.FUNCTION_SCOPE;
import static java.util.Collections.reverse;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toUnmodifiableList;

import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.AnnotatedInstruction;
import com.dylibso.chicory.wasm.types.ExternalType;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

final class WasmAnalyzer {

    private final WasmModule module;
    private final List<ValType> globalTypes;
    private final List<FunctionType> functionTypes;
    private final List<ValType> tableTypes;
    private final int functionImports;

    /** Tracks scope nesting for structured control flow emission. */
    static final class ScopeInfo {
        final int label;
        final OpCode opcode; // BLOCK, LOOP, IF, or NOP (function scope)
        final FunctionType blockType;
        final Instruction scopeInstruction;
        boolean elseEncountered; // tracks whether ELSE was seen for IF scopes

        ScopeInfo(int label, OpCode opcode, FunctionType blockType, Instruction scopeInstruction) {
            this.label = label;
            this.opcode = opcode;
            this.blockType = blockType;
            this.scopeInstruction = scopeInstruction;
        }
    }

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

    @SuppressWarnings("checkstyle:modifiedcontrolvariable")
    public List<CompilerInstruction> analyze(int funcId) {
        var functionType = functionTypes.get(funcId);
        var body = module.codeSection().getFunctionBody(funcId - functionImports);
        var stack = new TypeStack();
        int nextLabel = 0;
        List<CompilerInstruction> result = new ArrayList<>();

        // Scope stack for structured control flow
        Deque<ScopeInfo> scopeStack = new ArrayDeque<>();

        // Function scope (outermost)
        int funcLabel = nextLabel++;
        stack.enterScope(FUNCTION_SCOPE, FunctionType.of(List.of(), functionType.returns()));
        scopeStack.push(new ScopeInfo(funcLabel, OpCode.NOP, functionType, FUNCTION_SCOPE));

        int exitBlockDepth = -1;
        int exitTargetLabel = -1;
        for (int idx = 0; idx < body.instructions().size(); idx++) {
            AnnotatedInstruction ins = body.instructions().get(idx);

            // Skip instructions after unconditional control transfer
            if (exitBlockDepth >= 0) {
                if (ins.depth() > exitBlockDepth
                        || (ins.opcode() != OpCode.ELSE && ins.opcode() != OpCode.END)) {
                    continue;
                }

                if (ins.opcode() == OpCode.END) {
                    stack.scopeRestore();
                    // Check if we've reached the target scope
                    if (!scopeStack.isEmpty() && scopeStack.peek().label == exitTargetLabel) {
                        // Function scope: don't process END, let implicit return handle it
                        if (scopeStack.peek().opcode == OpCode.NOP) {
                            continue;
                        }
                        exitBlockDepth = -1;
                        exitTargetLabel = -1;
                        // Fall through to normal END processing
                    } else if (!scopeStack.isEmpty()
                            && scopeStack.peek().opcode == OpCode.IF
                            && !scopeStack.peek().elseEncountered) {
                        // IF without ELSE: implicit else path makes code after
                        // the if reachable, so clear dead code and fall through
                        // to normal END processing
                        exitBlockDepth = -1;
                        exitTargetLabel = -1;
                        // Fall through to normal END processing
                    } else {
                        // Intermediate scope: pop and emit SCOPE_EXIT, stay in dead code
                        ScopeInfo scope = scopeStack.pop();
                        if (scope.opcode != OpCode.NOP) {
                            result.add(
                                    new CompilerInstruction(
                                            CompilerOpCode.SCOPE_EXIT,
                                            prependLabel(scope.label, scope.blockType.returns())));
                        }
                        continue;
                    }
                } else {
                    // ELSE: clear dead code mode, fall through
                    if (!scopeStack.isEmpty() && scopeStack.peek().opcode == OpCode.IF) {
                        scopeStack.peek().elseEncountered = true;
                    }
                    exitBlockDepth = -1;
                    exitTargetLabel = -1;
                }
            }

            switch (ins.opcode()) {
                case NOP:
                    break;

                case UNREACHABLE:
                    exitBlockDepth = ins.depth();
                    exitTargetLabel = scopeStack.peek().label;
                    result.add(new CompilerInstruction(CompilerOpCode.TRAP));
                    break;

                case BLOCK:
                    {
                        int label = nextLabel++;
                        var bt = blockType(ins);
                        stack.enterScope(ins.scope(), bt);
                        scopeStack.push(new ScopeInfo(label, OpCode.BLOCK, bt, ins.scope()));
                        // operands: [label, ...result_type_ids]
                        result.add(
                                new CompilerInstruction(
                                        CompilerOpCode.BLOCK_ENTER,
                                        prependLabel(label, bt.returns())));
                        break;
                    }

                case LOOP:
                    {
                        int label = nextLabel++;
                        var bt = blockType(ins);
                        stack.enterScope(ins.scope(), bt);
                        scopeStack.push(new ScopeInfo(label, OpCode.LOOP, bt, ins.scope()));
                        // operands: [label, ...param_type_ids]
                        result.add(
                                new CompilerInstruction(
                                        CompilerOpCode.LOOP_ENTER,
                                        prependLabel(label, bt.params())));
                        break;
                    }

                case IF:
                    {
                        stack.pop(ValType.I32);
                        int label = nextLabel++;
                        var bt = blockType(ins);
                        stack.enterScope(ins.scope(), bt);
                        // Save stack for else branch if ELSE exists
                        boolean hasElse =
                                body.instructions().get(ins.labelFalse() - 1).opcode()
                                        == OpCode.ELSE;
                        if (hasElse) {
                            stack.pushTypes();
                        }
                        scopeStack.push(new ScopeInfo(label, OpCode.IF, bt, ins.scope()));
                        // operands: [label, ...result_type_ids]
                        result.add(
                                new CompilerInstruction(
                                        CompilerOpCode.IF_ENTER,
                                        prependLabel(label, bt.returns())));
                        break;
                    }

                case ELSE:
                    if (!scopeStack.isEmpty() && scopeStack.peek().opcode == OpCode.IF) {
                        scopeStack.peek().elseEncountered = true;
                    }
                    stack.popTypes();
                    result.add(new CompilerInstruction(CompilerOpCode.ELSE_ENTER));
                    break;

                case END:
                    {
                        if (scopeStack.isEmpty() || scopeStack.peek().opcode == OpCode.NOP) {
                            break;
                        }
                        ScopeInfo scope = scopeStack.pop();
                        stack.exitScope(scope.scopeInstruction);
                        // Don't emit SCOPE_EXIT for function scope (handled by RETURN)
                        if (scope.opcode != OpCode.NOP) {
                            result.add(
                                    new CompilerInstruction(
                                            CompilerOpCode.SCOPE_EXIT,
                                            prependLabel(scope.label, scope.blockType.returns())));
                        }
                        break;
                    }

                case BR:
                    {
                        exitBlockDepth = ins.depth();
                        int depth = (int) ins.operand(0);
                        ScopeInfo target = getScopeAtDepth(scopeStack, depth);
                        exitTargetLabel = target.label;

                        if (target.opcode == OpCode.NOP) {
                            // BR targeting function scope = RETURN
                            for (var type : reversed(functionType.returns())) {
                                stack.pop(type);
                            }
                            result.add(
                                    new CompilerInstruction(
                                            CompilerOpCode.RETURN, ids(functionType.returns())));
                        } else {
                            emitUnwind(result, stack, target);
                            if (target.opcode == OpCode.LOOP) {
                                result.add(
                                        new CompilerInstruction(
                                                CompilerOpCode.CONTINUE, target.label));
                            } else {
                                result.add(
                                        new CompilerInstruction(
                                                CompilerOpCode.BREAK, target.label));
                            }
                        }
                        break;
                    }

                case BR_IF:
                    {
                        stack.pop(ValType.I32);
                        int depth = (int) ins.operand(0);
                        ScopeInfo target = getScopeAtDepth(scopeStack, depth);

                        // Embed unwind data into BREAK_IF/CONTINUE_IF operands
                        // Simple: [label]
                        // With unwind: [label, drop, type1, type2, ...]
                        var unwindOpt = computeUnwind(stack, target);
                        long[] breakOperands;
                        if (unwindOpt.isPresent()) {
                            long[] unwindOps = unwindOpt.get().operands().toArray();
                            breakOperands = new long[1 + unwindOps.length];
                            breakOperands[0] = target.label;
                            System.arraycopy(unwindOps, 0, breakOperands, 1, unwindOps.length);
                        } else {
                            breakOperands = new long[] {target.label};
                        }

                        if (target.opcode == OpCode.LOOP) {
                            result.add(
                                    new CompilerInstruction(
                                            CompilerOpCode.CONTINUE_IF, breakOperands));
                        } else {
                            result.add(
                                    new CompilerInstruction(
                                            CompilerOpCode.BREAK_IF, breakOperands));
                        }
                        break;
                    }

                case BR_TABLE:
                    {
                        exitBlockDepth = ins.depth();
                        exitTargetLabel = scopeStack.peek().label;
                        stack.pop(ValType.I32);

                        // Use operands (WASM depths), not labelTable (instruction indices)
                        int entryCount = ins.operandCount();

                        // Single-entry table: just unconditional branch
                        if (entryCount == 1) {
                            int depth0 = (int) ins.operand(0);
                            ScopeInfo target0 = getScopeAtDepth(scopeStack, depth0);
                            result.add(
                                    new CompilerInstruction(CompilerOpCode.DROP, ValType.I32.id()));
                            emitUnwind(result, stack, target0);
                            if (target0.opcode == OpCode.LOOP) {
                                result.add(
                                        new CompilerInstruction(
                                                CompilerOpCode.CONTINUE, target0.label));
                            } else {
                                result.add(
                                        new CompilerInstruction(
                                                CompilerOpCode.BREAK, target0.label));
                            }
                            break;
                        }

                        // Multi-entry: emit SWITCH for now (not yet fully supported)
                        long[] depths = new long[entryCount];
                        for (int i = 0; i < entryCount; i++) {
                            depths[i] = ins.operand(i);
                        }
                        result.add(new CompilerInstruction(CompilerOpCode.SWITCH, depths));
                        break;
                    }

                case RETURN:
                    exitBlockDepth = ins.depth();
                    exitTargetLabel = funcLabel;
                    for (var type : reversed(functionType.returns())) {
                        stack.pop(type);
                    }
                    result.add(
                            new CompilerInstruction(
                                    CompilerOpCode.RETURN, ids(functionType.returns())));
                    break;

                case RETURN_CALL:
                    result.add(
                            new CompilerInstruction(
                                    CompilerOpCode.of(OpCode.CALL), ins.operands()));
                    updateStack(stack, functionTypes.get((int) ins.operand(0)));

                    exitBlockDepth = ins.depth();
                    exitTargetLabel = funcLabel;
                    for (var type : reversed(functionType.returns())) {
                        stack.pop(type);
                    }
                    result.add(
                            new CompilerInstruction(
                                    CompilerOpCode.RETURN, ids(functionType.returns())));
                    break;

                case RETURN_CALL_INDIRECT:
                    stack.pop(ValType.I32);
                    updateStack(stack, module.typeSection().getType((int) ins.operand(0)));
                    result.add(
                            new CompilerInstruction(
                                    CompilerOpCode.of(OpCode.CALL_INDIRECT), ins.operands()));

                    exitBlockDepth = ins.depth();
                    exitTargetLabel = funcLabel;
                    for (var type : reversed(functionType.returns())) {
                        stack.pop(type);
                    }
                    result.add(
                            new CompilerInstruction(
                                    CompilerOpCode.RETURN, ids(functionType.returns())));
                    break;

                case THROW:
                    result.add(
                            new CompilerInstruction(
                                    CompilerOpCode.of(OpCode.THROW), ins.operands()));
                    exitBlockDepth = ins.depth();
                    exitTargetLabel = scopeStack.peek().label;
                    break;

                case THROW_REF:
                    result.add(
                            new CompilerInstruction(
                                    CompilerOpCode.of(OpCode.THROW_REF), ins.operands()));
                    exitBlockDepth = ins.depth();
                    exitTargetLabel = scopeStack.peek().label;
                    break;

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

                default:
                    analyzeSimple(result, stack, ins, functionType, body);
            }
        }

        // implicit return at end of function (skip if already terminated)
        if (exitBlockDepth < 0) {
            for (var type : reversed(functionType.returns())) {
                stack.pop(type);
            }
            result.add(new CompilerInstruction(CompilerOpCode.RETURN, ids(functionType.returns())));
        }

        return result;
    }

    /** Get the scope at the given depth from the top of the scope stack. */
    private static ScopeInfo getScopeAtDepth(Deque<ScopeInfo> scopeStack, int depth) {
        int i = 0;
        for (ScopeInfo scope : scopeStack) {
            if (i == depth) {
                return scope;
            }
            i++;
        }
        throw new ChicoryException("BR depth " + depth + " exceeds scope stack size");
    }

    /** Emit DROP_KEEP if needed for a branch to the given target scope. */
    private void emitUnwind(List<CompilerInstruction> result, TypeStack stack, ScopeInfo target) {
        computeUnwind(stack, target).ifPresent(result::add);
    }

    /** Compute DROP_KEEP for a branch to the given target scope. */
    private Optional<CompilerInstruction> computeUnwind(TypeStack stack, ScopeInfo target) {
        boolean forward = (target.opcode != OpCode.LOOP);
        var types = forward ? target.blockType.returns() : target.blockType.params();
        int keep = types.size();

        int drop = stack.types().size() - stack.scopeStackSize(target.scopeInstruction);
        if (forward) {
            drop -= types.size();
        }

        if (drop <= 0) {
            return Optional.empty();
        }

        var operands = LongStream.builder();
        operands.add(drop);

        List<ValType> dropKeepTypes =
                stack.types().stream().limit(drop + keep).collect(toCollection(ArrayList::new));
        reverse(dropKeepTypes);
        dropKeepTypes.stream().mapToLong(ValType::id).forEach(operands::add);

        return Optional.of(
                new CompilerInstruction(CompilerOpCode.DROP_KEEP, operands.build().toArray()));
    }

    private static long[] prependLabel(int label, List<ValType> types) {
        long[] operands = new long[1 + types.size()];
        operands[0] = label;
        for (int i = 0; i < types.size(); i++) {
            operands[i + 1] = types.get(i).id();
        }
        return operands;
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
                // [] -> [ref]
                stack.push(ValType.FuncRef);
                break;
            case REF_NULL:
                // [] -> [ref]
                stack.push(
                        ValType.builder()
                                .withOpcode(ValType.ID.RefNull)
                                .withTypeIdx((int) ins.operand(0))
                                .build(module.typeSection()::getType));
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

    private FunctionType blockType(Instruction ins) {
        var typeId = ins.operand(0);
        if (typeId == 0x40) {
            return FunctionType.empty();
        }
        if (ValType.isValid(typeId)) {
            return FunctionType.returning(
                    ValType.builder().fromId(typeId).build(module.typeSection()::getType));
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
}
