package com.dylibso.chicory.runtime.internal;

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
 * A strong-value map keyed by auto-assigned integers.
 * Values are held with strong references to prevent premature GC collection.
 * Unreachable entries can be removed via {@link #sweep}.
 */
public class IntWeakValueMap<V> {
    private final Map<Integer, V> map = new HashMap<>();
    private int nextId;

    public IntWeakValueMap() {}

    /** Creates a map with auto-assigned keys starting at the given ID. */
    public IntWeakValueMap(int initialId) {
        this.nextId = initialId;
    }

    /** Inserts a value with an automatically assigned key. */
    public int put(V value) {
        int id = nextId++;
        map.put(id, value);
        return id;
    }

    /** Inserts or replaces a value at a given key. */
    public void put(int key, V value) {
        map.put(key, value);
    }

    /** Retrieves a value by key, or null if missing. */
    public V get(int key) {
        return map.get(key);
    }

    /** Checks if the map contains the given key. */
    public boolean containsKey(int key) {
        return map.containsKey(key);
    }

    /** Explicitly removes an entry. */
    public void remove(int key) {
        map.remove(key);
    }

    /** Returns the number of entries. */
    public int size() {
        return map.size();
    }

    /** Clears all entries. */
    public void clear() {
        map.clear();
    }

    /**
     * Mark-sweep collection of unreachable entries.
     *
     * @param rootCollector receives an {@link IntConsumer} that the caller uses
     *                      to report all root keys (e.g. from globals, tables)
     * @param tracer        given a value and an {@link IntConsumer}, reports the keys
     *                      of all entries directly referenced by that value
     */
    public void sweep(Consumer<IntConsumer> rootCollector, BiConsumer<V, IntConsumer> tracer) {
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

        // Mark phase: collect roots
        rootCollector.accept(collector);

        // Trace phase: BFS through object graph
        while (!worklist.isEmpty()) {
            int id = worklist.poll();
            V value = map.get(id);
            if (value != null) {
                tracer.accept(value, collector);
            }
        }

        // Sweep phase: remove unreachable entries
        map.keySet().retainAll(reachable);
    }
}
