package com.dylibso.chicory.runtime.internal;

import com.dylibso.chicory.runtime.GlobalInstance;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.TableInstance;
import com.dylibso.chicory.runtime.WasmArray;
import com.dylibso.chicory.runtime.WasmGcRef;
import com.dylibso.chicory.runtime.WasmStruct;
import com.dylibso.chicory.wasm.types.TypeSection;
import com.dylibso.chicory.wasm.types.Value;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * Store for GC-managed references keyed by auto-assigned integers.
 * Values are held with strong references and unreachable entries are
 * periodically removed via mark-sweep collection.
 */
public class GcRefStore {

    /**
     * GC ref IDs start at this offset to avoid collisions with externref
     * values that get internalized via any.convert_extern. Since internalized
     * externrefs and GC refs both live in the ANY hierarchy, they share the
     * same integer representation space.
     */
    public static final int ID_OFFSET = 0x10000;

    private static final int SWEEP_INTERVAL = 1024;

    private final Map<Integer, WasmGcRef> map = new HashMap<>();
    private int nextId = ID_OFFSET;

    private final Instance instance;
    private int allocCount;

    public GcRefStore(Instance instance) {
        this.instance = instance;
    }

    /**
     * Inserts a value with an automatically assigned key.
     * Triggers mark-sweep collection every {@value SWEEP_INTERVAL} allocations.
     */
    public int put(WasmGcRef value) {
        int id = nextId++;
        map.put(id, value);
        if (++allocCount >= SWEEP_INTERVAL) {
            allocCount = 0;
            sweep();
        }
        return id;
    }

    /** Retrieves a value by key, or null if missing. */
    public WasmGcRef get(int key) {
        return map.get(key);
    }

    /** Checks whether a raw reference value is a GC ref ID. */
    public static boolean isGcRefId(long val) {
        return val >= ID_OFFSET && val != Value.REF_NULL_VALUE && !Value.isI31(val);
    }

    /**
     * Performs mark-sweep collection of unreachable entries.
     * Scans all roots (globals, tables), traces through struct fields
     * and array elements, then removes unreachable entries.
     */
    public void sweep() {
        if (map.isEmpty()) {
            return;
        }

        Set<Integer> reachable = new HashSet<>();
        Queue<Integer> worklist = new ArrayDeque<>();

        IntConsumer collector =
                id -> {
                    if (reachable.add(id) && map.containsKey(id)) {
                        worklist.add(id);
                    }
                };

        // Mark phase: collect roots from globals and tables
        collectRoots(collector);

        // Trace phase: BFS through object graph
        TypeSection typeSection = instance.module().typeSection();
        while (!worklist.isEmpty()) {
            int id = worklist.poll();
            WasmGcRef value = map.get(id);
            if (value != null) {
                trace(value, typeSection, collector);
            }
        }

        // Sweep phase: remove unreachable entries
        map.keySet().retainAll(reachable);
    }

    private void collectRoots(IntConsumer collector) {
        // Globals (both local and imported)
        int totalGlobals =
                instance.module().globalSection().globalCount() + instance.imports().globalCount();
        for (int i = 0; i < totalGlobals; i++) {
            GlobalInstance g = instance.global(i);
            if (g.getType().isReference()) {
                long val = g.getValue();
                if (isGcRefId(val)) {
                    collector.accept((int) val);
                }
            }
        }

        // Tables (both local and imported)
        int totalTables =
                instance.module().tableSection().tableCount() + instance.imports().tableCount();
        for (int i = 0; i < totalTables; i++) {
            TableInstance t = instance.table(i);
            for (int j = 0; j < t.size(); j++) {
                int ref = t.ref(j);
                if (isGcRefId(ref)) {
                    collector.accept(ref);
                }
            }
        }
    }

    private static void trace(WasmGcRef ref, TypeSection typeSection, IntConsumer collector) {
        if (ref instanceof WasmStruct) {
            WasmStruct struct = (WasmStruct) ref;
            var subType = typeSection.getSubType(struct.typeIdx());
            var structType = subType.compType().structType();
            if (structType != null) {
                var fieldTypes = structType.fieldTypes();
                for (int i = 0; i < fieldTypes.length; i++) {
                    var storage = fieldTypes[i].storageType();
                    var valType = storage.valType();
                    if (valType != null && valType.isReference()) {
                        long val = struct.field(i);
                        if (isGcRefId(val)) {
                            collector.accept((int) val);
                        }
                    }
                }
            }
        } else if (ref instanceof WasmArray) {
            WasmArray array = (WasmArray) ref;
            var subType = typeSection.getSubType(array.typeIdx());
            var arrayType = subType.compType().arrayType();
            if (arrayType != null) {
                var storage = arrayType.fieldType().storageType();
                var valType = storage.valType();
                if (valType != null && valType.isReference()) {
                    for (int i = 0; i < array.length(); i++) {
                        long val = array.get(i);
                        if (isGcRefId(val)) {
                            collector.accept((int) val);
                        }
                    }
                }
            }
        }
    }
}
