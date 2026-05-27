package com.example.generated;

import com.dylibso.chicory.component.CanonicalAbi;
import com.dylibso.chicory.component.annotation.WitField;
import com.dylibso.chicory.component.annotation.WitRecord;
import com.dylibso.chicory.component.types.PrimitiveType;
import com.dylibso.chicory.component.types.RecordType;
import com.dylibso.chicory.runtime.Memory;
import java.util.HashMap;
import java.util.Map;

@WitRecord("person")
public class Person {
    @WitField(order = 0)
    private String name;

    @WitField(order = 1)
    private int age;

    @WitField(order = 2)
    private boolean active;

    public Person() {}

    public Person(String name, int age, boolean active) {
        this.name = name;
        this.age = age;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long[] encode(Memory memory) throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("age", age);
        map.put("active", active);
        return CanonicalAbi.encode(map, createRecordType(), memory);
    }

    public static Person decode(long[] encoded, Memory memory) throws Exception {
        Object obj = CanonicalAbi.decode(encoded, createRecordType(), memory);
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            Person result = new Person();
            result.name = (String) map.get("name");
            result.age = (int) map.get("age");
            result.active = (boolean) map.get("active");
            return result;
        }
        throw new IllegalArgumentException("Invalid decode result type");
    }

    private static RecordType createRecordType() {
        RecordType record = new RecordType("person");
        record.addField("name", PrimitiveType.STRING);
        record.addField("age", PrimitiveType.I32);
        record.addField("active", PrimitiveType.BOOL);
        return record;
    }

    static {
        com.dylibso.chicory.component.PojoRegistry.register("person", Person.class);
    }
}
