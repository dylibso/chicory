package com.dylibso.chicory.wasm.types;

public class ElementSection extends Section {
    private Element[] elements;

    public ElementSection(long id, long size, Element[] elements) {
       super(id, size);
       this.elements = elements;
    }

    public Element[] getElements() {
        return elements;
    }
}
