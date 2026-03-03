package com.dylibso.chicory.runtime.internal;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.WasmArray;
import com.dylibso.chicory.runtime.WasmGcRef;
import com.dylibso.chicory.runtime.WasmStruct;
import com.dylibso.chicory.wasm.types.Value;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Store for GC-managed references keyed by auto-assigned integers.
 *
 * <p>Uses epoch-based deferred collection: refs are never swept during wasm
 * execution. Collection only happens at <em>safe points</em> — between
 * top-level calls — when the wasm operand stack and all call frames are
 * empty. At that point the only roots are globals and tables.
 */
public class GcRefStore {

    /**
     * GC ref IDs start at this offset to avoid collisions with externref
     * values that get internalized via any.convert_extern. Since internalized
     * externrefs and GC refs both live in the ANY hierarchy, they share the
     * same integer representation space.
     */
    public static final int ID_OFFSET = 0x10000;

    private static final int SWEEP_INTERVAL = 4096;

    private final Instance instance;
    private final Map<Integer, WasmGcRef> map = new HashMap<>();
    private int nextId = ID_OFFSET;
    private int allocsSinceLastSweep;
    private boolean sweepRequested;

    public GcRefStore(Instance instance) {
        this.instance = instance;
    }

    /** Inserts a value with an automatically assigned key. */
    public int put(WasmGcRef value) {
        int id = nextId++;
        map.put(id, value);
        allocsSinceLastSweep++;
        if (allocsSinceLastSweep >= SWEEP_INTERVAL) {
            sweepRequested = true;
        }
        return id;
    }

    /** Retrieves a value by key, or null if missing. */
    public WasmGcRef get(int key) {
        return map.get(key);
    }

    /** Called at safe points (between top-level calls). */
    public void safePoint() {
        if (sweepRequested) {
            sweep();
            sweepRequested = false;
            allocsSinceLastSweep = 0;
        }
    }

    /** Checks whether a raw reference value is a GC ref ID. */
    public static boolean isGcRefId(long val) {
        return val >= ID_OFFSET && val != Value.REF_NULL_VALUE && !Value.isI31(val);
    }

    private void sweep() {
        Set<Integer> reachable = new HashSet<>();

        // 1. Scan globals
        int globalCount = instance.globalCount();
        for (int i = 0; i < globalCount; i++) {
            var g = instance.global(i);
            if (g != null) {
                markIfGcRef(g.getValueLow(), reachable);
            }
        }

        // 2. Scan tables
        int tableCount = instance.tableCount();
        for (int i = 0; i < tableCount; i++) {
            var table = instance.table(i);
            if (table != null) {
                for (int j = 0; j < table.size(); j++) {
                    markIfGcRef(table.ref(j), reachable);
                }
            }
        }

        // 3. Remove unreachable entries
        map.keySet().removeIf(id -> !reachable.contains(id));
    }

    private void markIfGcRef(long val, Set<Integer> reachable) {
        if (!isGcRefId(val)) {
            return;
        }
        int id = (int) val;
        if (!reachable.add(id)) {
            return; // already visited — prevents infinite loops in cyclic structures
        }
        WasmGcRef ref = map.get(id);
        if (ref == null) {
            return;
        }
        // Recursively trace nested refs
        if (ref instanceof WasmStruct) {
            var s = (WasmStruct) ref;
            for (int i = 0; i < s.fieldCount(); i++) {
                markIfGcRef(s.field(i), reachable);
            }
        } else if (ref instanceof WasmArray) {
            var a = (WasmArray) ref;
            for (int i = 0; i < a.length(); i++) {
                markIfGcRef(a.get(i), reachable);
            }
        }
    }
}
