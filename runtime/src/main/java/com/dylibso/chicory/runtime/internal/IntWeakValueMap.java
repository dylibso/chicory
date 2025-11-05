package com.dylibso.chicory.runtime.internal;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * A weak-value map keyed by integers.
 * Values are weakly referenced and removed when GC'd.
 * Keys are assigned automatically in increasing order.
 */
public class IntWeakValueMap<V> {
    private static class EntryRef<V> extends WeakReference<V> {
        final int key;

        EntryRef(int key, V value, ReferenceQueue<V> queue) {
            super(value, queue);
            this.key = key;
        }
    }

    private final Map<Integer, EntryRef<V>> map = new HashMap<>();
    private final ReferenceQueue<V> queue = new ReferenceQueue<>();
    private int nextId;

    /** Inserts a value with an automatically assigned key. */
    public int put(V value) {
        cleanup();
        int id = nextId++;
        map.put(id, new EntryRef<>(id, value, queue));
        return id;
    }

    /** Inserts or replaces a value at a given key. */
    public void put(int key, V value) {
        cleanup();
        map.put(key, new EntryRef<>(key, value, queue));
    }

    /** Retrieves a value by key, or null if GC’d or missing. */
    public V get(int key) {
        EntryRef<V> ref = map.get(key);
        return ref != null ? ref.get() : null;
    }

    /** Explicitly removes an entry. */
    public void remove(int key) {
        cleanup();
        map.remove(key);
    }

    /** Returns the number of live entries. */
    public int size() {
        cleanup();
        return map.size();
    }

    /** Clears all entries. */
    public void clear() {
        map.clear();
        while (queue.poll() != null) {
            // drain
        }
    }

    /** Removes entries whose values were GC’d. */
    @SuppressWarnings("unchecked")
    private void cleanup() {
        EntryRef<V> ref;
        while ((ref = (EntryRef<V>) queue.poll()) != null) {
            map.remove(ref.key, ref);
        }
    }
}
