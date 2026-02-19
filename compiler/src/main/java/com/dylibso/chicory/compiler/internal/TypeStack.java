package com.dylibso.chicory.compiler.internal;

import static com.dylibso.chicory.wasm.types.Instruction.EMPTY_OPERANDS;

import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.OpCode;
import com.dylibso.chicory.wasm.types.TypeSection;
import com.dylibso.chicory.wasm.types.ValType;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

final class TypeStack {

    public static final Instruction FUNCTION_SCOPE =
            new Instruction(-1, OpCode.NOP, EMPTY_OPERANDS);

    private final Deque<Deque<ValType>> types = new ArrayDeque<>();
    private final Deque<Deque<ValType>> restore = new ArrayDeque<>();
    private final Map<Instruction, Integer> scopes = new HashMap<>();
    private final TypeSection typeSection;

    public TypeStack(TypeSection typeSection) {
        this.typeSection = typeSection;
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
        if (!ValType.matches(actual, expected, typeSection)) {
            throw new IllegalArgumentException("Expected type " + expected + " <> " + actual);
        }
    }

    public void popRef() {
        var actual = types().pop();
        if (!actual.isReference()) {
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

        // restored stack when exiting "polymorphic" blocks after unconditional control transfer
        Deque<ValType> stack = new ArrayDeque<>(types());
        for (int i = 0; i < scopeType.params().size(); i++) {
            stack.pop();
        }
        for (ValType type : scopeType.returns()) {
            stack.push(type);
        }
        restore.push(stack);
    }

    public void exitScope(Instruction scope) {
        scopes.remove(scope);
        restore.pop();
    }

    public void scopeRestore() {
        types.pop();
        types.push(restore.getFirst());
    }

    public int scopeStackSize(Instruction scope) {
        return scopes.get(scope);
    }

    public Deque<ValType> types() {
        return types.getFirst();
    }

    public void verifyEmpty() {
        if (types.size() != 1) {
            throw new RuntimeException("Bad types stack: " + types);
        }
        if (!types().isEmpty()) {
            throw new RuntimeException("Types not empty: " + types());
        }
    }
}
