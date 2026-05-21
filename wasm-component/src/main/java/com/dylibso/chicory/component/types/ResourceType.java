package com.dylibso.chicory.component.types;

/**
 * Represents a resource (opaque handle) WIT type.
 */
public class ResourceType implements WitType {
    private final String resourceName;

    public ResourceType(String resourceName) {
        this.resourceName = resourceName;
    }

    @Override
    public String displayName() {
        return resourceName;
    }
}
