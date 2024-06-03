package com.dylibso.chicory.aot;

import static com.dylibso.chicory.aot.AotUtil.slotCount;

import com.dylibso.chicory.aot.AotUtil.StackSize;
import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Class for tracking context relevant to compiling a single function
 */
public class AotContext {

    protected final String internalClassName;
    protected final List<ValueType> globalTypes;
    protected final List<FunctionType> functionTypes;
    protected final int funcId;
    protected final FunctionType type;
    protected final FunctionBody body;
    protected final List<Integer> slots;
    protected final int memorySlot;
    protected final int instanceSlot;
    protected final Deque<StackSize> stackSizes = new ArrayDeque<>();

    public AotContext(
            String internalClassName,
            List<ValueType> globalTypes,
            List<FunctionType> functionTypes,
            int funcId,
            FunctionType type,
            FunctionBody body) {
        this.internalClassName = internalClassName;
        this.globalTypes = globalTypes;
        this.functionTypes = functionTypes;
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

    public Deque<StackSize> stackSizes() {
        return stackSizes;
    }

    public void pushStackSize(StackSize size) {
        stackSizes.push(size);
    }

    public StackSize popStackSize() {
        return stackSizes.pop();
    }
}
