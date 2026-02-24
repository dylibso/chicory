package com.dylibso.chicory.runtime.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.WasmGcRef;
import com.dylibso.chicory.wasm.Parser;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;

public class GcRefStoreTest {

    // Minimal valid wasm module: magic number + version 1
    private static final byte[] EMPTY_WASM = {0x00, 0x61, 0x73, 0x6D, 0x01, 0x00, 0x00, 0x00};

    private static WasmGcRef ref(int typeIdx) {
        return () -> typeIdx;
    }

    private static GcRefStore newStore() {
        var module = Parser.parse(new ByteArrayInputStream(EMPTY_WASM));
        var instance = Instance.builder(module).build();
        return new GcRefStore(instance);
    }

    @Test
    public void autoAssignKeysFromOffset() {
        var store = newStore();
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
        var store = newStore();
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
}
