package com.example.generated;

import com.dylibso.chicory.component.annotation.WitComponent;
import com.dylibso.chicory.component.ComponentModel;
import com.dylibso.chicory.runtime.Memory;
import java.util.ArrayList;
import java.util.List;

@WitComponent("example")
public class ExampleComponent {
    private final ComponentModel componentModel;

    public ExampleComponent(ComponentModel componentModel) {
        this.componentModel = componentModel;
    }

    public int add(int a, int b) throws Exception {
        Object result = componentModel.callExport("add", a, b);
        return ((Number) result).intValue();
    }

    public long multiply(long a, long b) throws Exception {
        Object result = componentModel.callExport("multiply", a, b);
        return ((Number) result).longValue();
    }

    public boolean isPositive(int x) throws Exception {
        Object result = componentModel.callExport("is-positive", x);
        return (Boolean) result;
    }

    public String greet(String name) throws Exception {
        Object result = componentModel.callExport("greet", name);
        return (String) result;
    }

    public String processText(String text) throws Exception {
        Object result = componentModel.callExport("process-text", text);
        return (String) result;
    }

    public void testHostCallLog(String msg) throws Exception {
        Object result = componentModel.callExport("test-host-call-log", msg);
    }

    public String testHostCallGetInput() throws Exception {
        Object result = componentModel.callExport("test-host-call-get-input");
        return (String) result;
    }

    public String describePerson(Person p) throws Exception {
        long[] __p_encoded = p.encode(componentModel.getInstance().memory());
        Object result = componentModel.callExport("describe-person", __p_encoded);
        return (String) result;
    }

    public Person createPerson(String name, int age) throws Exception {
        Object result = componentModel.callExport("create-person", name, age);
        return Person.decode((long[]) result, componentModel.getInstance().memory());
    }

    public OperationResult getResult() throws Exception {
        Object result = componentModel.callExport("get-result");
        return OperationResult.decode((long[]) result, componentModel.getInstance().memory());
    }

    public Color pickColor(int index) throws Exception {
        Object result = componentModel.callExport("pick-color", index);
        return Color.decode((long[]) result, componentModel.getInstance().memory());
    }

    public List<String> repeatString(String text, int count) throws Exception {
        Object result = componentModel.callExport("repeat-string", text, count);
        return (List<String>) result;
    }

    public int sumNumbers(List<Integer> numbers) throws Exception {
        Object result = componentModel.callExport("sum-numbers", numbers);
        return ((Number) result).intValue();
    }

    public List<String> getNames(List<Person> people) throws Exception {
        Object result = componentModel.callExport("get-names", people);
        return (List<String>) result;
    }

    public List<Person> filterHighValuePeople(List<Person> people, int minAge) throws Exception {
        Object result = componentModel.callExport("filter-high-value-people", people, minAge);
        return (List<Person>) result;
    }

    public String processUserStatus(UserStatus status) throws Exception {
        long[] __status_encoded = status.encode(componentModel.getInstance().memory());
        Object result = componentModel.callExport("process-user-status", __status_encoded);
        return (String) result;
    }

    public UserStatus createUserStatus(String name, int age, String message) throws Exception {
        Object result = componentModel.callExport("create-user-status", name, age, message);
        return UserStatus.decode((long[]) result, componentModel.getInstance().memory());
    }

    public int validateResults(List<OperationResult> results) throws Exception {
        Object result = componentModel.callExport("validate-results", results);
        return ((Number) result).intValue();
    }

}
