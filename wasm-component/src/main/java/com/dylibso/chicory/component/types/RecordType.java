package com.dylibso.chicory.component.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a record (struct-like) WIT type.
 */
public class RecordType implements WitType {
    private final String recordName;
    private final List<Field> fields;

    public RecordType(String recordName) {
        this.recordName = recordName;
        this.fields = new ArrayList<>();
    }

    @Override
    public String displayName() {
        return recordName;
    }

    public void addField(String fieldName, WitType fieldType) {
        fields.add(new Field(fieldName, fieldType));
    }

    public List<Field> fields() {
        return Collections.unmodifiableList(fields);
    }

    public Field fieldByName(String name) {
        return fields.stream()
                .filter(f -> f.name.equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Field not found: " + name));
    }

    public static class Field {
        public final String name;
        public final WitType type;

        public Field(String name, WitType type) {
            this.name = name;
            this.type = type;
        }
    }
}
