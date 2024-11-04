package com.dylibso.chicory.experimental.aot;

import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.ArrayList;
import java.util.List;

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
            slot += AotUtil.slotCount(param);
        }

        // extra arguments
        this.memorySlot = slot;
        slot++;
        this.instanceSlot = slot;
        slot++;

        // WASM locals
        for (ValueType local : body.localTypes()) {
            slots.add(slot);
            slot += AotUtil.slotCount(local);
        }

        this.slots = List.copyOf(slots);
        this.tempSlot = slot;
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
}
