package com.dylibso.chicory.component.types;

/**
 * Represents a list (dynamic array) WIT type.
 */
public class ListType implements WitType {
    private final WitType elementType;

    public ListType(WitType elementType) {
        this.elementType = elementType;
    }

    @Override
    public String displayName() {
        return "list<" + elementType.displayName() + ">";
    }

    public WitType elementType() {
        return elementType;
    }
}
