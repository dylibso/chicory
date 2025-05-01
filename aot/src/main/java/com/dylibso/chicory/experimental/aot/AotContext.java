package com.dylibso.chicory.experimental.aot;

import static com.dylibso.chicory.experimental.aot.AotUtil.hasTooManyParameters;
import static com.dylibso.chicory.experimental.aot.AotUtil.slotCount;

import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for tracking context relevant to compiling a single function
 */
final class AotContext {

    private final String internalClassName;
    private final int maxFunctionsPerClass;
    private final List<ValType> globalTypes;
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
            int maxFunctionsPerClass,
            List<ValType> globalTypes,
            List<FunctionType> functionTypes,
            FunctionType[] types,
            int funcId,
            FunctionType type,
            FunctionBody body) {
        this.internalClassName = internalClassName;
        this.maxFunctionsPerClass = maxFunctionsPerClass;
        this.globalTypes = globalTypes;
        this.functionTypes = functionTypes;
        this.types = types;
        this.funcId = funcId;
        this.type = type;
        this.body = body;

        // compute JVM slot indices for WASM locals
        List<Integer> slots = new ArrayList<>(type.params().size() + body.localTypes().size());
        int slot = 0;

        // WASM arguments
        if (hasTooManyParameters(type)) {
            slot += 1; // long[]
        } else {
            for (ValType param : type.params()) {
                slots.add(slot);
                slot += slotCount(param);
            }
        }

        // extra arguments
        this.memorySlot = slot;
        slot++;
        this.instanceSlot = slot;
        slot++;

        // the long[] gets unboxed
        if (hasTooManyParameters(type)) {
            for (ValType param : type.params()) {
                slots.add(slot);
                slot += slotCount(param);
            }
        }

        // WASM locals
        for (ValType local : body.localTypes()) {
            slots.add(slot);
            slot += slotCount(local);
        }

        this.slots = List.copyOf(slots);
        this.tempSlot = slot;
    }

    public String internalClassName() {
        return internalClassName;
    }

    public List<ValType> globalTypes() {
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

    public String classNameForFuncGroup(int funcId) {
        return "$FuncGroup_" + (funcId / maxFunctionsPerClass);
    }
}
