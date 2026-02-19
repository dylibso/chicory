package com.dylibso.chicory.source.compiler.internal;

import static com.dylibso.chicory.wasm.types.Instruction.EMPTY_OPERANDS;

import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.ValType;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Type stack tracking for WasmAnalyzer. Aligned with the ASM compiler's TypeStack behavior.
 *
 * <p>Key design: enterScope/exitScope do NOT modify the current type stack. enterScope pre-computes
 * a "restore" stack (for use after unreachable code), and exitScope just cleans up bookkeeping.
 */
final class TypeStack {

    public static final Instruction FUNCTION_SCOPE =
            new Instruction(-1, OpCode.NOP, EMPTY_OPERANDS);

    private final Deque<Deque<ValType>> types = new ArrayDeque<>();
    private final Deque<Deque<ValType>> restore = new ArrayDeque<>();
    private final Map<Instruction, Integer> scopes = new HashMap<>();

    public TypeStack() {
        this.types.push(new ArrayDeque<>());
    }

    public ValType peek() {
        return types().getFirst();
    }

    public void push(ValType type) {
        types().push(type);
    }

    public void pop(ValType expected) {
        var actual = types().pop();
        if (!ValType.matches(actual, expected)) {
            throw new IllegalArgumentException("Expected type " + expected + " <> " + actual);
        }
    }

    public void popRef() {
        var actual = types().pop();
        if (!actual.equals(ValType.FuncRef) && !actual.equals(ValType.ExternRef)) {
            throw new IllegalArgumentException("Expected reference type <> " + actual);
        }
    }

    public void pushTypes() {
        types.push(new ArrayDeque<>(types()));
    }

    public void popTypes() {
        types.pop();
    }

    /**
     * Enter a new scope. Does NOT modify the current type stack. Pre-computes the "restore" stack
     * which represents what the type stack should look like after the scope exits (with params
     * replaced by returns). This restore stack is used by scopeRestore() after unreachable code.
     */
    public void enterScope(Instruction scope, FunctionType scopeType) {
        scopes.put(scope, types().size());

        // Pre-compute the restored stack: copy current, remove params, add returns
        Deque<ValType> stack = new ArrayDeque<>(types());
        for (int i = 0; i < scopeType.params().size(); i++) {
            stack.pop();
        }
        for (ValType type : scopeType.returns()) {
            stack.push(type);
        }
        restore.push(stack);
    }

    /**
     * Exit a scope. Does NOT modify the current type stack. Just cleans up scope bookkeeping.
     */
    public void exitScope(Instruction scope) {
        scopes.remove(scope);
        restore.pop();
    }

    /**
     * Restore the type stack after unreachable code (e.g., after BR/UNREACHABLE followed by END).
     * Replaces the current type stack with the pre-computed restore stack.
     */
    public void scopeRestore() {
        types.pop();
        types.push(restore.getFirst());
    }

    public int scopeStackSize(Instruction scope) {
        Integer size = scopes.get(scope);
        if (size == null) {
            throw new IllegalStateException("Unknown scope: " + scope);
        }
        return size;
    }

    public Deque<ValType> types() {
        return types.getFirst();
    }

    public Deque<ValType> typesSnapshot() {
        return new ArrayDeque<>(types());
    }

    public void verifyEmpty() {
        if (!types().isEmpty()) {
            throw new IllegalStateException("Stack not empty: " + types());
        }
    }
}
