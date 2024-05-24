package com.dylibso.chicory.aot;

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

    protected final int funcId;
    protected final FunctionType type;
    protected final FunctionBody body;
    protected final List<Integer> slots;
    protected final int memorySlot;
    protected final int longSlot;
    protected final int doubleSlot;
    protected final Deque<StackSize> stackSizes = new ArrayDeque<>();

    public AotContext(int funcId, FunctionType type, FunctionBody body) {
        this.funcId = funcId;
        this.type = type;
        this.body = body;
        this.slots = computeSlots(type, body);
        this.memorySlot = slots.get(slots.size() - 1);
        this.longSlot = memorySlot + 1;
        this.doubleSlot = longSlot + 2;
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

    public int longSlot() {
        return longSlot;
    }

    public int doubleSlot() {
        return doubleSlot;
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

    private static List<Integer> computeSlots(FunctionType type, FunctionBody body) {
        List<Integer> slots = new ArrayList<>();

        // skip "this"
        int slot = 1;

        // arguments and local variables
        for (ValueType param : type.params()) {
            slots.add(slot);
            slot += slotCount(param);
        }
        for (ValueType local : body.localTypes()) {
            slots.add(slot);
            slot += slotCount(local);
        }

        // first available slot
        slots.add(slot);

        return List.copyOf(slots);
    }

    private static int slotCount(ValueType type) {
        switch (type) {
            case I32:
            case F32:
                return 1;
            case I64:
            case F64:
                return 2;
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }
}
