package com.dylibso.chicory.wasm.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Represents the Element Section in a WebAssembly module.
 * This section defines initialization data for table elements.
 */
public final class ElementSection extends Section {
    private final List<Element> elements;

    private ElementSection(List<Element> elements) {
        super(SectionId.ELEMENT);
        this.elements = List.copyOf(elements);
    }

    /**
     * Returns the element segments defined in this section.
     *
     * @return an array of {@link Element} instances.
     */
    public Element[] elements() {
        return elements.toArray(new Element[0]);
    }

    /**
     * Returns the number of element segments defined in this section.
     *
     * @return the count of element segments.
     */
    public int elementCount() {
        return elements.size();
    }

    /**
     * Returns the element segment at the specified index.
     *
     * @param idx the index of the element segment to retrieve.
     * @return the {@link Element} segment at the given index.
     */
    public Element getElement(int idx) {
        return elements.get(idx);
    }

    /**
     * Returns a stream over the element segments in this section.
     *
     * @return a {@link Stream} of {@link Element} instances.
     */
    public Stream<Element> stream() {
        return elements.stream();
    }

    /**
     * Creates a new builder for constructing an {@link ElementSection}.
     *
     * @return a new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link ElementSection} instances.
     */
    public static final class Builder {
        private final List<Element> elements = new ArrayList<>();

        private Builder() {}

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

        /**
         * Constructs the {@link ElementSection} instance from the added element segments.
         *
         * @return the built {@link ElementSection}.
         */
        public ElementSection build() {
            return new ElementSection(elements);
        }
    }

    /**
     * Compares this element section to another object for equality.
     *
     * @param o the object to compare against.
     * @return {@code true} if the object is an {@code ElementSection} with the same element segments, {@code false} otherwise.
     */
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

    /**
     * Computes the hash code for this element section.
     *
     * @return the hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(elements);
    }
}
