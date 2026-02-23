package com.dylibso.chicory.runtime.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A strong-value map keyed by auto-assigned integers.
 * Values are held with strong references to prevent premature GC collection.
 * Unreachable entries can be removed via {@link #retainAll(Set)}.
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

    /** Removes all entries whose keys are NOT in the given set. */
    public void retainAll(Set<Integer> keysToKeep) {
        map.keySet().retainAll(keysToKeep);
    }

    /** Returns the key set for iteration during sweep. */
    public Set<Integer> keySet() {
        return map.keySet();
    }
}
