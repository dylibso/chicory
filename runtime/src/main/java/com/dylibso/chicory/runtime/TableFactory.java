package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.Table;

/**
 * Factory for creating {@link TableInstance} objects during module instantiation.
 */
@FunctionalInterface
public interface TableFactory {
    TableInstance create(Table table, int initValue);
}
