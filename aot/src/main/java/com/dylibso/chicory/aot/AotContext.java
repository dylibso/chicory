package com.dylibso.chicory.aot;

import com.dylibso.chicory.wasm.types.FunctionBody;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValueType;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for tracking context relevant to compiling a single function
 */
public class AotContext {

    protected final int funcId;
    protected final FunctionType type;
    protected final FunctionBody body;
    protected final List<Integer> slots;

    public AotContext(int funcId, FunctionType type, FunctionBody body) {
        this.funcId = funcId;
        this.type = type;
        this.body = body;
        this.slots = computeSlots(type, body);
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

    public int slotIndex(int localIndex) {
        return slots.get(localIndex);
    }

    private static List<Integer> computeSlots(FunctionType type, FunctionBody body) {
        int slot = 0;
        List<Integer> slots = new ArrayList<>();
        slots.add(slot);
        slot++;
        for (ValueType param : type.params()) {
            slots.add(slot);
            slot += slotCount(param);
        }
        for (ValueType local : body.localTypes()) {
            slots.add(slot);
            slot += slotCount(local);
        }
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
