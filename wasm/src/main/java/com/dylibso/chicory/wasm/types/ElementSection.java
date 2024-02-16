package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.Objects;

public final class ElementSection extends Section {
    private final ArrayList<Element> elements;

    /**
     * Construct a new, empty section instance.
     */
    public ElementSection() {
        this(new ArrayList<>());
    }

    /**
     * Construct a new, empty section instance.
     *
     * @param estimatedSize the estimated number of elements to reserve space for
     */
    public ElementSection(int estimatedSize) {
        this(new ArrayList<>(estimatedSize));
    }

    private ElementSection(ArrayList<Element> elements) {
        super(SectionId.ELEMENT);
        this.elements = elements;
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

    /**
     * Add an element definition to this section.
     *
     * @param element the element to add to this section (must not be {@code null})
     * @return the index of the newly-added element
     */
    public int addElement(Element element) {
        Objects.requireNonNull(element, "element");
        int idx = elements.size();
        elements.add(element);
        return idx;
    }
}
