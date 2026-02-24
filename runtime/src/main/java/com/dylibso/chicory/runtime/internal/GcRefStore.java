package com.dylibso.chicory.runtime.internal;

import com.dylibso.chicory.runtime.WasmGcRef;
import com.dylibso.chicory.wasm.types.Value;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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

    private Consumer<IntConsumer> rootCollector;
    private BiConsumer<WasmGcRef, IntConsumer> tracer;
    private int allocCount;

    public GcRefStore() {}

    /**
     * Configures the mark-sweep callbacks.
     *
     * @param rootCollector receives an {@link IntConsumer} to report all root keys
     *                      (e.g. from globals, tables)
     * @param tracer        given a value and an {@link IntConsumer}, reports the keys
     *                      of all entries directly referenced by that value
     */
    public void configureSweep(
            Consumer<IntConsumer> rootCollector, BiConsumer<WasmGcRef, IntConsumer> tracer) {
        this.rootCollector = rootCollector;
        this.tracer = tracer;
    }

    /**
     * Inserts a value with an automatically assigned key.
     * Triggers mark-sweep collection every {@value SWEEP_INTERVAL} allocations.
     */
    public int put(WasmGcRef value) {
        int id = nextId++;
        map.put(id, value);
        if (rootCollector != null && ++allocCount >= SWEEP_INTERVAL) {
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
     * Scans all roots via the configured root collector, traces through
     * the object graph, then removes unreachable entries.
     */
    public void sweep() {
        if (map.isEmpty() || rootCollector == null) {
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

        // Mark phase: collect roots
        rootCollector.accept(collector);

        // Trace phase: BFS through object graph
        while (!worklist.isEmpty()) {
            int id = worklist.poll();
            WasmGcRef value = map.get(id);
            if (value != null) {
                tracer.accept(value, collector);
            }
        }

        // Sweep phase: remove unreachable entries
        map.keySet().retainAll(reachable);
    }
}
