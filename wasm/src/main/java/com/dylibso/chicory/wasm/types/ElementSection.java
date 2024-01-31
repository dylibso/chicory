package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.Objects;

public class ElementSection extends Section {
    private final ArrayList<Element> elements;

    private ElementSection(ArrayList<Element> elements) {
        super(SectionId.ELEMENT);
        this.elements = elements;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ElementSection elementSection) {
        return new Builder(elementSection);
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

    public static final class Builder {
        private final ArrayList<Element> elements;

        private Builder() {
            this.elements = new ArrayList<>();
        }

        private Builder(ElementSection elementSection) {
            this.elements = new ArrayList<>();
            this.elements.addAll(elementSection.elements);
        }

        public Builder addElement(Element element) {
            Objects.requireNonNull(element, "element");
            elements.add(element);
            return this;
        }

        public ElementSection build() {
            return new ElementSection(elements);
        }
    }
}
