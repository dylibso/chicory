package com.example.generated;

import com.dylibso.chicory.component.CanonicalAbi;
import com.dylibso.chicory.component.annotation.WitField;
import com.dylibso.chicory.component.annotation.WitRecord;
import com.dylibso.chicory.component.types.RecordType;
import com.dylibso.chicory.runtime.Memory;
import java.util.HashMap;
import java.util.Map;

@WitRecord("user-status")
public class UserStatus {
    @WitField(order = 0)
    private Person person;

    public UserStatus() {}

    public UserStatus(Person person) {
        this.person = person;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public long[] encode(Memory memory) throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("person", person);
        return CanonicalAbi.encode(map, createRecordType(), memory);
    }

    public static UserStatus decode(long[] encoded, Memory memory) throws Exception {
        Object obj = CanonicalAbi.decode(encoded, createRecordType(), memory);
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            UserStatus result = new UserStatus();
            result.person = (Person) map.get("person");
            return result;
        }
        throw new IllegalArgumentException("Invalid decode result type");
    }

    private static RecordType createRecordType() {
        RecordType record = new RecordType("user-status");
        record.addField("person", new RecordType("person"));
        return record;
    }

    static {
        com.dylibso.chicory.component.PojoRegistry.register("user-status", UserStatus.class);
    }
}
