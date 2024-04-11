package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.MutabilityType;
import com.dylibso.chicory.wasm.types.Value;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class HostGlobal implements FromHost {
    private Value value;
    private Supplier<Value> getValue;
    private Consumer<Value> setValue;

    private final MutabilityType type;
    private final String moduleName;
    private final String fieldName;

    public HostGlobal(String moduleName, String fieldName, Value value) {
        this(moduleName, fieldName, value, MutabilityType.Const);
    }

    public HostGlobal(String moduleName, String fieldName, Value value, MutabilityType type) {
        this.value = value;
        this.type = type;
        this.moduleName = moduleName;
        this.fieldName = fieldName;
    }

    public HostGlobal(
            String moduleName,
            String fieldName,
            Supplier<Value> getValue,
            Consumer<Value> setValue,
            MutabilityType type) {
        this.value = null;
        this.setValue = setValue;
        this.getValue = getValue;
        this.type = type;
        this.moduleName = moduleName;
        this.fieldName = fieldName;
    }

    public Value value() {
        if (getValue == null) {
            return value;
        } else {
            return getValue.get();
        }
    }

    public void setValue(Value value) {
        if (setValue == null) {
            this.value = value;
        } else {
            setValue.accept(value);
        }
    }

    public MutabilityType mutabilityType() {
        return type;
    }

    public String moduleName() {
        return moduleName;
    }

    public String fieldName() {
        return fieldName;
    }

    @Override
    public FromHostType type() {
        return FromHostType.GLOBAL;
    }
}
