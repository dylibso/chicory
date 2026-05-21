package com.dylibso.chicory.component.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for looking up and managing WIT type definitions.
 */
public class TypeRegistry {
    private final Map<String, WitType> types = new HashMap<>();

    public TypeRegistry() {
        // Register primitive types
        for (PrimitiveType t : PrimitiveType.values()) {
            types.put(t.name(), t);
        }
    }

    public void register(String name, WitType type) {
        if (types.containsKey(name)) {
            throw new IllegalArgumentException("Type already registered: " + name);
        }
        types.put(name, type);
    }

    public Optional<WitType> lookup(String name) {
        return Optional.ofNullable(types.get(name));
    }

    public WitType lookupRequired(String name) {
        return lookup(name)
                .orElseThrow(() -> new IllegalArgumentException("Type not found: " + name));
    }

    public boolean isDefined(String name) {
        return types.containsKey(name);
    }
}
