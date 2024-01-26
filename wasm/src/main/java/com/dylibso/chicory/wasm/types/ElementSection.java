package com.dylibso.chicory.wasm.types;

public class ElementSection extends Section {
    private Element[] elements;

    public ElementSection(long id, Element[] elements) {
        super(id);
        this.elements = elements;
    }

    public Element[] elements() {
        return elements;
    }
}
