package com.example.generated;

import com.dylibso.chicory.component.CanonicalAbi;
import com.dylibso.chicory.component.annotation.WitField;
import com.dylibso.chicory.component.annotation.WitRecord;
import com.dylibso.chicory.component.types.RecordType;
import com.dylibso.chicory.runtime.Memory;
import java.util.HashMap;
import java.util.Map;

/**
 * Generated POJO for WIT record: user-status { person: person, status: operation-result }
 */
@WitRecord("user-status")
public class UserStatus {
    @WitField(order = 0)
    private Person person;

    @WitField(order = 1)
    private OperationResult status;

    public UserStatus() {}

    public UserStatus(Person person, OperationResult status) {
        this.person = person;
        this.status = status;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public OperationResult getStatus() {
        return status;
    }

    public void setStatus(OperationResult status) {
        this.status = status;
    }

    public long[] encode(Memory memory) throws Exception {
        Map<String, Object> map = new HashMap<>();
        if (person != null) {
            map.put("person", person);
        }
        if (status != null) {
            map.put("status", status);
        }
        return CanonicalAbi.encode(map, createRecordType(), memory);
    }

    public static UserStatus decode(long[] encoded, Memory memory) throws Exception {
        Object obj = CanonicalAbi.decode(encoded, createRecordType(), memory);
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            UserStatus result = new UserStatus();
            Object personObj = map.get("person");
            if (personObj instanceof Person) {
                result.person = (Person) personObj;
            }
            Object statusObj = map.get("status");
            if (statusObj instanceof OperationResult) {
                result.status = (OperationResult) statusObj;
            }
            return result;
        }
        throw new IllegalArgumentException("Invalid decode result type");
    }

    private static RecordType createRecordType() {
        RecordType record = new RecordType("user-status");
        record.addField("person", new RecordType("person"));
        record.addField(
                "status", new com.dylibso.chicory.component.types.VariantType("operation-result"));
        return record;
    }

    @Override
    public String toString() {
        return "UserStatus{person=" + person + ", status=" + status + "}";
    }
}
