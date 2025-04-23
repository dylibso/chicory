package com.dylibso.chicory.experimental.aot;

import static com.dylibso.chicory.experimental.aot.AotUtil.localType;
import static com.dylibso.chicory.experimental.aot.TypeStack.FUNCTION_SCOPE;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

final class AotAnalyzer {

    private final WasmModule module;
    private final List<ValType> globalTypes;
    private final List<FunctionType> functionTypes;
    private final List<ValType> tableTypes;
    private final int functionImports;

    public AotAnalyzer(WasmModule module) {
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

    public List<AotInstruction> analyze(int funcId) {
        var functionType = functionTypes.get(funcId);
        var body = module.codeSection().getFunctionBody(funcId - functionImports);
        var stack = new TypeStack();
        int nextLabel = body.instructions().size();
        List<AotInstruction> result = new ArrayList<>();

        // find label targets
        Set<Integer> labels = new HashSet<>();
        for (AnnotatedInstruction ins : body.instructions()) {
            if (ins.labelTrue() != AnnotatedInstruction.UNDEFINED_LABEL) {
                labels.add(ins.labelTrue());
            }
            if (ins.labelFalse() != AnnotatedInstruction.UNDEFINED_LABEL) {
                labels.add(ins.labelFalse());
            }
            labels.addAll(ins.labelTable());
        }

        // implicit block for the function
        stack.enterScope(FUNCTION_SCOPE, FunctionType.of(List.of(), functionType.returns()));

        int exitBlockDepth = -1;
        for (int idx = 0; idx < body.instructions().size(); idx++) {
            AnnotatedInstruction ins = body.instructions().get(idx);

            if (labels.contains(idx)) {
                result.add(new AotInstruction(AotOpCode.LABEL, idx));
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
                    result.add(new AotInstruction(AotOpCode.TRAP));
                    break;
                case BLOCK:
                case LOOP:
                    stack.enterScope(ins.scope(), blockType(ins));
                    break;
                case END:
                    stack.exitScope(ins.scope());
                    break;
                case RETURN:
                    exitBlockDepth = ins.depth();
                    for (var type : reversed(functionType.returns())) {
                        stack.pop(type);
                    }
                    result.add(new AotInstruction(AotOpCode.RETURN, ids(functionType.returns())));
                    break;
                case RETURN_CALL:
                    // The JVM does not support proper tail calls, so we desugar RETURN_CALL
                    // into a CALL + RETURN.

                    // [p*] -> [r*]
                    result.add(new AotInstruction(AotOpCode.of(OpCode.CALL), ins.operands()));
                    updateStack(stack, functionTypes.get((int) ins.operand(0)));

                    exitBlockDepth = ins.depth();
                    for (var type : reversed(functionType.returns())) {
                        stack.pop(type);
                    }
                    result.add(new AotInstruction(AotOpCode.RETURN, ids(functionType.returns())));
                    break;

                case RETURN_CALL_INDIRECT:
                    // The JVM does not support proper tail calls, so we desugar
                    // RETURN_CALL_INDIRECT into a CALL_INDIRECT + RETURN.

                    // [p* I32] -> [r*]
                    stack.pop(ValType.I32);
                    updateStack(stack, module.typeSection().getType((int) ins.operand(0)));
                    result.add(
                            new AotInstruction(AotOpCode.of(OpCode.CALL_INDIRECT), ins.operands()));

                    exitBlockDepth = ins.depth();
                    for (var type : reversed(functionType.returns())) {
                        stack.pop(type);
                    }

                    result.add(new AotInstruction(AotOpCode.RETURN, ids(functionType.returns())));
                    break;

                case IF:
                    stack.pop(ValType.I32);
                    stack.enterScope(ins.scope(), blockType(ins));
                    // use the same starting stack sizes for both sides of the branch
                    if (body.instructions().get(ins.labelFalse() - 1).opcode() == OpCode.ELSE) {
                        stack.pushTypes();
                    }
                    result.add(new AotInstruction(AotOpCode.IFEQ, ins.labelFalse()));
                    break;
                case ELSE:
                    stack.popTypes();
                    result.add(new AotInstruction(AotOpCode.GOTO, ins.labelTrue()));
                    break;
                case BR:
                    exitBlockDepth = ins.depth();
                    unwindStack(functionType, body, ins, ins.labelTrue(), stack)
                            .ifPresent(result::add);
                    result.add(new AotInstruction(AotOpCode.GOTO, ins.labelTrue()));
                    break;
                case BR_IF:
                    stack.pop(ValType.I32);
                    var ifUnwind = unwindStack(functionType, body, ins, ins.labelTrue(), stack);
                    if (ifUnwind.isPresent()) {
                        result.add(new AotInstruction(AotOpCode.IFEQ, ins.labelFalse()));
                        result.add(ifUnwind.get());
                        result.add(new AotInstruction(AotOpCode.GOTO, ins.labelTrue()));
                    } else {
                        result.add(new AotInstruction(AotOpCode.IFNE, ins.labelTrue()));
                    }
                    break;
                case BR_TABLE:
                    exitBlockDepth = ins.depth();
                    stack.pop(ValType.I32);
                    // convert to jump if it only has a default
                    if (ins.labelTable().size() == 1) {
                        result.add(new AotInstruction(AotOpCode.DROP, ValType.I32.id()));
                        unwindStack(functionType, body, ins, ins.labelTable().get(0), stack)
                                .ifPresent(result::add);
                        result.add(new AotInstruction(AotOpCode.GOTO, ins.labelTable().get(0)));
                        break;
                    }
                    // extract unique targets and generate unwind for each
                    List<AotInstruction> unwinds = new ArrayList<>();
                    Map<Integer, Integer> targets = new HashMap<>();
                    for (var target : ins.labelTable()) {
                        if (!targets.containsKey(target)) {
                            int label = target;
                            var unwind = unwindStack(functionType, body, ins, target, stack);
                            if (unwind.isPresent()) {
                                label = nextLabel;
                                nextLabel++;
                                unwinds.add(new AotInstruction(AotOpCode.LABEL, label));
                                unwinds.add(unwind.get());
                                unwinds.add(new AotInstruction(AotOpCode.GOTO, target));
                            }
                            targets.put(target, label);
                        }
                    }
                    long[] operands = ins.labelTable().stream().mapToLong(targets::get).toArray();
                    result.add(new AotInstruction(AotOpCode.SWITCH, operands));
                    result.addAll(unwinds);
                    break;
                case SELECT:
                case SELECT_T:
                    // [t t I32] -> [t]
                    stack.pop(ValType.I32);
                    var selectType = stack.peek();
                    stack.pop(selectType);
                    stack.pop(selectType);
                    stack.push(selectType);
                    result.add(new AotInstruction(AotOpCode.SELECT, selectType.id()));
                    break;
                case DROP:
                    // [t] -> []
                    var dropType = stack.peek();
                    stack.pop(dropType);
                    result.add(new AotInstruction(AotOpCode.DROP, dropType.id()));
                    break;
                case LOCAL_TEE:
                    // [t] -> [t]
                    var teeType = stack.peek();
                    stack.pop(teeType);
                    stack.push(teeType);
                    long[] teeOperands = {ins.operand(0), teeType.id()};
                    result.add(new AotInstruction(AotOpCode.LOCAL_TEE, teeOperands));
                    break;
                default:
                    analyzeSimple(result, stack, ins, functionType, body);
            }
        }

        // implicit return at end of function
        for (var type : reversed(functionType.returns())) {
            stack.pop(type);
        }
        result.add(new AotInstruction(AotOpCode.RETURN, ids(functionType.returns())));

        stack.verifyEmpty();
        return result;
    }

    private void analyzeSimple(
            List<AotInstruction> out,
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
                stack.push(ValType.refTypeForId(ins.operand(0)));
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
            case DATA_DROP:
            case ELEM_DROP:
                // [] -> []
                break;
            default:
                throw new ChicoryException("Unhandled opcode: " + ins.opcode());
        }
        out.add(new AotInstruction(AotOpCode.of(ins.opcode()), ins.operands()));
    }

    private static void updateStack(TypeStack stack, FunctionType functionType) {
        for (ValType type : reversed(functionType.params())) {
            stack.pop(type);
        }
        for (ValType type : functionType.returns()) {
            stack.push(type);
        }
    }

    private Optional<AotInstruction> unwindStack(
            FunctionType functionType,
            FunctionBody body,
            AnnotatedInstruction ins,
            int label,
            TypeStack stack) {

        boolean forward = true;
        var target = body.instructions().get(label);
        if (target.address() <= ins.address()) {
            // the loop block is the instruction before the target
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

        return Optional.of(new AotInstruction(AotOpCode.DROP_KEEP, operands.build().toArray()));
    }

    private FunctionType blockType(Instruction ins) {
        var typeId = ins.operand(0);
        if (typeId == 0x40) {
            return FunctionType.empty();
        }
        if (ValType.isValid(typeId)) {
            return FunctionType.returning(ValType.forId(typeId));
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
