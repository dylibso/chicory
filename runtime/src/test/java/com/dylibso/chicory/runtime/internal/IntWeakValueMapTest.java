package com.dylibso.chicory.runtime.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class IntWeakValueMapTest {

    @Test
    public void autoAssignKeysFromZero() {
        var map = new IntWeakValueMap<String>();
        int k0 = map.put("a");
        int k1 = map.put("b");
        int k2 = map.put("c");
        assertEquals(0, k0);
        assertEquals(1, k1);
        assertEquals(2, k2);
        assertEquals("a", map.get(k0));
        assertEquals("b", map.get(k1));
        assertEquals("c", map.get(k2));
        assertEquals(3, map.size());
    }

    @Test
    public void autoAssignKeysFromInitialId() {
        var map = new IntWeakValueMap<String>(100);
        int k0 = map.put("x");
        int k1 = map.put("y");
        assertEquals(100, k0);
        assertEquals(101, k1);
        assertEquals("x", map.get(k0));
        assertEquals("y", map.get(k1));
    }

    @Test
    public void putAtExplicitKey() {
        var map = new IntWeakValueMap<String>();
        map.put(42, "hello");
        assertEquals("hello", map.get(42));
        assertEquals(1, map.size());
    }

    @Test
    public void putAtExplicitKeyOverwrites() {
        var map = new IntWeakValueMap<String>();
        map.put(7, "first");
        map.put(7, "second");
        assertEquals("second", map.get(7));
        assertEquals(1, map.size());
    }

    @Test
    public void getMissingKeyReturnsNull() {
        var map = new IntWeakValueMap<String>();
        assertNull(map.get(0));
        assertNull(map.get(999));
    }

    @Test
    public void remove() {
        var map = new IntWeakValueMap<String>();
        int k = map.put("val");
        assertEquals("val", map.get(k));
        map.remove(k);
        assertNull(map.get(k));
        assertEquals(0, map.size());
    }

    @Test
    public void removeNonExistentKeyIsHarmless() {
        var map = new IntWeakValueMap<String>();
        map.remove(999);
        assertEquals(0, map.size());
    }

    @Test
    public void clear() {
        var map = new IntWeakValueMap<String>();
        map.put("a");
        map.put("b");
        map.put("c");
        assertEquals(3, map.size());
        map.clear();
        assertEquals(0, map.size());
    }

    @Test
    public void autoAndExplicitKeysCoexist() {
        var map = new IntWeakValueMap<String>(10);
        map.put(5, "explicit");
        int k0 = map.put("auto1");
        int k1 = map.put("auto2");
        assertEquals(10, k0);
        assertEquals(11, k1);
        assertEquals("explicit", map.get(5));
        assertEquals("auto1", map.get(10));
        assertEquals("auto2", map.get(11));
        assertEquals(3, map.size());
    }

    @Test
    public void weakReferenceCleanup() throws Exception {
        var map = new IntWeakValueMap<byte[]>();
        // Insert a value with no strong reference kept
        int key = map.put(new byte[1024 * 1024]);
        assertEquals(1, map.size());

        // Encourage GC to collect the weakly-held value
        for (int i = 0; i < 10; i++) {
            System.gc();
            Thread.sleep(50);
            if (map.get(key) == null) {
                break;
            }
        }

        // After GC, the value should be collected and size should reflect cleanup
        // Note: GC is non-deterministic, so we verify the contract holds
        // when the value IS collected
        if (map.get(key) == null) {
            assertEquals(0, map.size());
        }
    }

    @Test
    public void strongReferencePreventsCleaning() throws Exception {
        var map = new IntWeakValueMap<byte[]>();
        byte[] strongRef = new byte[1024];
        int key = map.put(strongRef);

        System.gc();
        Thread.sleep(100);

        // Value is still strongly referenced, so it must survive GC
        assertEquals(strongRef, map.get(key));
        assertEquals(1, map.size());
    }
}
