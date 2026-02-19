package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class ElementSection extends Section {
    private final List<Element> elements;

    private ElementSection(List<Element> elements) {
        super(SectionId.ELEMENT);
        this.elements = List.copyOf(elements);
    }

    public Element[] elements() {
        return elements.toArray(new Element[0]);
    }

    public int elementCount() {
        return elements.size();
    }

    public Element getElement(int idx) {
        return elements.get(idx);
    }

    public Stream<Element> stream() {
        return elements.stream();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<Element> elements = new ArrayList<>();

        /**
         * Add an element definition to this section.
         *
         * @param element the element to add to this section (must not be {@code null})
         * @return the Builder
         */
        public Builder addElement(Element element) {
            Objects.requireNonNull(element, "element");
            elements.add(element);
            return this;
        }

        public ElementSection build() {
            return new ElementSection(elements);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof ElementSection)) {
            return false;
        }
        ElementSection that = (ElementSection) o;
        return Objects.equals(elements, that.elements);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(elements);
    }
}
