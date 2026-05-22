package com.dylibso.chicory.component;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class for representing list values from WIT lists.
 * Stores the actual list data and provides type information.
 */
public class ListValue {
    private final List<Object> elements;

    public ListValue(List<Object> elements) {
        this.elements = elements != null ? new ArrayList<>(elements) : new ArrayList<>();
    }

    public List<Object> elements() {
        return new ArrayList<>(elements);
    }

    public int size() {
        return elements.size();
    }

    public Object get(int index) {
        return elements.get(index);
    }

    @Override
    public String toString() {
        return "ListValue{" + elements + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListValue that = (ListValue) o;
        return elements.equals(that.elements);
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }
}
