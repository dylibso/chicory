package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.Limits;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;

import java.util.HashMap;
import java.util.Map;

import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;

public class TableInstance {

    private Table table;
    private Map<Integer, Instance> instances;

    public TableInstance(Table table) {
        this.table = table;
        this.instances = new HashMap<>();
    }

    public int size() {
        return table.size();
    }

    public ValueType elementType() {
        return table.elementType();
    }

    public Limits limits() {
        return table.limits();
    }

    public int grow(int size, int value, Instance instance) {
        return this.table.grow(size, value);
    }

    public Value ref(int index) {
        return table.ref(index);
    }

    public void setRef(int index, int value) {
        setRef(index, value, null);
    }

    public void setRef(int index, int value, Instance instance) {
        table.setRef(index, value);
        if (instance != null) {
            setInstance(index, instance);
        }
    }

    public void setInstance(int index, Instance instance) {
        instances.put(index, instance);
    }

    public Instance instance(int index) {
        return instances.get(index);
    }
    public void reset() {
        table.reset();
    }
}
