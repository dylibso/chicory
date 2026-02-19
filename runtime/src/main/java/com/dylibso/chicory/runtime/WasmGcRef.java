package com.dylibso.chicory.runtime;

/**
 * Marker interface for WasmGC heap objects (structs and arrays).
 */
public interface WasmGcRef {
    int typeIdx();
}
