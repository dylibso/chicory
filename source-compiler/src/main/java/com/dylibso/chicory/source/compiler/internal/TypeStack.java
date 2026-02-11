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
 * Minimal copy of the type stack from the ASM compiler.
 *
 * <p>This tracks the WASM operand stack types while {@link WasmAnalyzer} walks instructions. It is
 * independent of ASM and only uses WASM types.
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
        if (types().isEmpty()) {
            return;
        }
        var actual = types().pop();
        if (!ValType.matches(actual, expected)) {
            throw new IllegalArgumentException("Expected type " + expected + " <> " + actual);
        }
    }

    public void popRef() {
        if (types().isEmpty()) {
            return;
        }
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

    public void enterScope(Instruction scope, FunctionType scopeType) {
        scopes.put(scope, types().size());
        restore.push(new ArrayDeque<>(types()));
        if (scopeType == null) {
            return;
        }
        for (ValType type : scopeType.params()) {
            pop(type);
        }
        for (ValType type : scopeType.returns()) {
            push(type);
        }
    }

    public void exitScope(Instruction scope) {
        var expected = scopes.remove(scope);
        if (expected == null) {
            // For the minimal source compiler we are tolerant of scopes we did not explicitly
            // register (e.g., implicit END scopes). In that case, there is nothing to unwind.
            return;
        }
        while (types().size() > expected) {
            types().pop();
        }
    }

    public int scopeStackSize(Instruction scope) {
        Integer size = scopes.get(scope);
        if (size == null) {
            throw new IllegalStateException("Unknown scope: " + scope);
        }
        return size;
    }

    public Deque<ValType> types() {
        return types.peek();
    }

    public Deque<ValType> typesSnapshot() {
        return new ArrayDeque<>(types());
    }

    public void scopeRestore() {
        if (restore.isEmpty()) {
            throw new IllegalStateException("No scope to restore");
        }
        types.pop();
        types.push(new ArrayDeque<>(restore.pop()));
    }

    public void verifyEmpty() {
        if (!types().isEmpty()) {
            throw new IllegalStateException("Stack not empty: " + types());
        }
    }
}
