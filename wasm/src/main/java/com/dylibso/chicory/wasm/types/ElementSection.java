package com.dylibso.chicory.wasm.types;

public class ElementSection extends Section {
    private Element[] elements;

    public ElementSection(Element[] elements) {
        super(SectionId.ELEMENT);
        this.elements = elements;
    }

    public Element[] elements() {
        return elements;
    }
}
