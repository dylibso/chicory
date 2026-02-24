package com.dylibso.chicory.runtime.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.runtime.WasmGcRef;
import org.junit.jupiter.api.Test;

public class GcRefStoreTest {

    private static WasmGcRef ref(int typeIdx) {
        return () -> typeIdx;
    }

    @Test
    public void autoAssignKeysFromOffset() {
        var store = new GcRefStore();
        int k0 = store.put(ref(0));
        int k1 = store.put(ref(1));
        int k2 = store.put(ref(2));
        assertEquals(GcRefStore.ID_OFFSET, k0);
        assertEquals(GcRefStore.ID_OFFSET + 1, k1);
        assertEquals(GcRefStore.ID_OFFSET + 2, k2);
        assertEquals(0, store.get(k0).typeIdx());
        assertEquals(1, store.get(k1).typeIdx());
        assertEquals(2, store.get(k2).typeIdx());
    }

    @Test
    public void getMissingKeyReturnsNull() {
        var store = new GcRefStore();
        assertNull(store.get(0));
        assertNull(store.get(999));
    }

    @Test
    public void isGcRefIdClassifiesCorrectly() {
        assertTrue(GcRefStore.isGcRefId(GcRefStore.ID_OFFSET));
        assertTrue(GcRefStore.isGcRefId(GcRefStore.ID_OFFSET + 100));
        assertFalse(GcRefStore.isGcRefId(0));
        assertFalse(GcRefStore.isGcRefId(GcRefStore.ID_OFFSET - 1));
    }

    @Test
    public void sweepRemovesUnreachableEntries() {
        var store = new GcRefStore();
        int k0 = store.put(ref(0));
        int k1 = store.put(ref(1));
        int k2 = store.put(ref(2));

        // Only k0 is a root, no tracing
        store.configureSweep(collector -> collector.accept(k0), (val, collector) -> {});
        store.sweep();

        assertNotNull(store.get(k0));
        assertNull(store.get(k1));
        assertNull(store.get(k2));
    }

    @Test
    public void sweepTracesReferences() {
        var store = new GcRefStore();
        int k0 = store.put(ref(0));
        int k1 = store.put(ref(1));
        int k2 = store.put(ref(2));

        // k0 is a root, k0 references k2 via tracing, k1 is unreachable
        store.configureSweep(
                collector -> collector.accept(k0),
                (val, collector) -> {
                    if (val.typeIdx() == 0) {
                        collector.accept(k2);
                    }
                });
        store.sweep();

        assertNotNull(store.get(k0));
        assertNull(store.get(k1));
        assertNotNull(store.get(k2));
    }

    @Test
    public void sweepOnEmptyStoreIsNoop() {
        var store = new GcRefStore();
        store.configureSweep(collector -> {}, (val, collector) -> {});
        store.sweep(); // should not throw
    }
}
