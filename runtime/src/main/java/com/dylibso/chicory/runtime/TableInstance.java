package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.exceptions.ChicoryException;
import com.dylibso.chicory.wasm.types.Limits;
import com.dylibso.chicory.wasm.types.Table;
import com.dylibso.chicory.wasm.types.Value;
import com.dylibso.chicory.wasm.types.ValueType;

import static com.dylibso.chicory.wasm.types.Value.REF_NULL_VALUE;

public class TableInstance {

    private Table table;
    private Instance[] instances;

    public TableInstance(Table table) {
        this.table = table;
        this.instances = new Instance[table.size()];
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
        var sizeBefore = this.table.size();
        var res = this.table.grow(size, value);
        var sizeAfter = this.table.size();
        // TODO: implement grow for instances
        return res;
    }

    public Value ref(int index) {
        return table.ref(index);
    }

    public void setRef(int index, int value, Instance instance) {
        table.setRef(index, value);
        setInstance(index, instance);
    }

    public void setInstance(int index, Instance instance) {
        instances[index] = instance;
    }

    public Instance instance(int index) {
        return instances[index];
    }
    public void reset() {
        for (int i = 0; i < table.size(); i++) {
            this.instances[i] = null;
        }
    }
}
