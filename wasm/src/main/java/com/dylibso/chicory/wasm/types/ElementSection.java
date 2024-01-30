package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;

public class ElementSection extends Section {
    private final ArrayList<Element> elements;

    public ElementSection(Element[] elements) {
        super(SectionId.ELEMENT);
        this.elements = new ArrayList<>(List.of(elements));
    }

    public Element[] elements() {
        return elements.toArray(Element[]::new);
    }

    public int elementCount() {
        return elements.size();
    }

    public Element getElement(int idx) {
        return elements.get(idx);
    }
}
