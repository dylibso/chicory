package com.dylibso.chicory.aot;

import static com.dylibso.chicory.aot.AotUtil.jvmType;
import static com.dylibso.chicory.aot.AotUtil.slotCount;
import static com.dylibso.chicory.aot.AotUtil.stackSize;

import com.dylibso.chicory.aot.AotUtil.StackSize;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.Instruction;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for tracking context relevant to compiling a single function
 */
final class AotContext {

    private final String internalClassName;
    private final List<ValueType> globalTypes;
    private final List<FunctionType> functionTypes;
    private final FunctionType[] types;
    private final int funcId;
    private final FunctionType type;
    private final FunctionBody body;
    private final List<Integer> slots;
    private final int memorySlot;
    private final int instanceSlot;
    private final int tempSlot;
    private final Deque<Deque<StackSize>> stackSizesStack = new ArrayDeque<>();
    private final Map<Instruction, Integer> scopeStackSize = new HashMap<>();
    private final Deque<Deque<StackSize>> restoreStackSize = new ArrayDeque<>();

    public AotContext(
            String internalClassName,
            List<ValueType> globalTypes,
            List<FunctionType> functionTypes,
            FunctionType[] types,
            int funcId,
            FunctionType type,
            FunctionBody body) {
        this.internalClassName = internalClassName;
        this.globalTypes = globalTypes;
        this.functionTypes = functionTypes;
        this.types = types;
        this.funcId = funcId;
        this.type = type;
        this.body = body;

        // compute JVM slot indices for WASM locals
        List<Integer> slots = new ArrayList<>();
        int slot = 0;

        // WASM arguments
        for (ValueType param : type.params()) {
            slots.add(slot);
            slot += slotCount(param);
        }

        // extra arguments
        this.memorySlot = slot;
        slot++;
        this.instanceSlot = slot;
        slot++;

        // WASM locals
        for (ValueType local : body.localTypes()) {
            slots.add(slot);
            slot += slotCount(local);
        }

        this.slots = List.copyOf(slots);
        this.tempSlot = slot;
        this.stackSizesStack.push(new ArrayDeque<>());
    }

    public String internalClassName() {
        return internalClassName;
    }

    public List<ValueType> globalTypes() {
        return globalTypes;
    }

    public List<FunctionType> functionTypes() {
        return functionTypes;
    }

    public FunctionType[] types() {
        return types;
    }

    public int getId() {
        return funcId;
    }

    public FunctionType getType() {
        return type;
    }

    public FunctionBody getBody() {
        return body;
    }

    public int localSlotIndex(int localIndex) {
        return slots.get(localIndex);
    }

    public int memorySlot() {
        return memorySlot;
    }

    public int instanceSlot() {
        return instanceSlot;
    }

    public int tempSlot() {
        return tempSlot;
    }

    public Deque<StackSize> stackSizes() {
        return stackSizesStack.getFirst();
    }

    public void pushStackSize(StackSize size) {
        stackSizes().push(size);
    }

    public StackSize popStackSize() {
        return stackSizes().pop();
    }

    public Deque<Deque<StackSize>> stackSizesStack() {
        return stackSizesStack;
    }

    public void pushStackSizesStack() {
        stackSizesStack.push(new ArrayDeque<>(stackSizes()));
    }

    public void popStackSizesStack() {
        stackSizesStack.pop();
    }

    public void enterScope(Instruction scope, FunctionType scopeType) {
        scopeStackSize.put(scope, stackSizes().size());

        // stack sizes when exiting "polymorphic" blocks after unconditional control transfer
        Deque<StackSize> stack = new ArrayDeque<>(stackSizes());
        for (int i = 0; i < scopeType.params().size(); i++) {
            stack.pop();
        }
        for (ValueType type : scopeType.returns()) {
            stack.push(stackSize(jvmType(type)));
        }
        restoreStackSize.push(stack);
    }

    public void exitScope(Instruction scope) {
        scopeStackSize.remove(scope);
        restoreStackSize.pop();
    }

    public int scopeStackSize(Instruction scope) {
        return scopeStackSize.get(scope);
    }

    public void scopeRestoreStackSize() {
        stackSizesStack.pop();
        stackSizesStack.push(restoreStackSize.getFirst());
    }
}
