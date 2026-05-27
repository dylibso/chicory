package com.example.generated;

import com.dylibso.chicory.component.ComponentModel;
import com.dylibso.chicory.component.PojoRegistry;
import com.dylibso.chicory.component.VariantValue;
import com.dylibso.chicory.component.annotation.WitComponent;
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
        Object result = componentModel.callExport("describe-person", p);
        return (String) result;
    }

    public Person createPerson(String name, int age) throws Exception {
        Object result = componentModel.callExport("create-person", name, age);
        if (result instanceof VariantValue) {
            return (Person) PojoRegistry.variantValueToPojo("person", (VariantValue) result);
        }
        return (Person) result;
    }

    public OperationResult getResult() throws Exception {
        Object result = componentModel.callExport("get-result");
        // Debug: print what we got
        System.out.println("[DEBUG getResult] Result type: " + result.getClass().getSimpleName());
        if (result instanceof VariantValue) {
            VariantValue vv = (VariantValue) result;
            System.out.println(
                    "[DEBUG getResult] VariantValue - caseName: '"
                            + vv.caseName()
                            + "', data: "
                            + vv.data());
            Object converted = PojoRegistry.variantValueToPojo("operation-result", vv);
            System.out.println(
                    "[DEBUG getResult] After conversion: " + converted.getClass().getSimpleName());
            return (OperationResult) converted;
        }
        return (OperationResult) result;
    }

    public Color pickColor(int index) throws Exception {
        Object result = componentModel.callExport("pick-color", index);
        if (result instanceof VariantValue) {
            return (Color) PojoRegistry.variantValueToPojo("color", (VariantValue) result);
        }
        return (Color) result;
    }

    public List<String> repeatString(String text, int count) throws Exception {
        Object result = componentModel.callExport("repeat-string", text, count);
        if (result instanceof com.dylibso.chicory.component.ListValue) {
            java.util.List<Object> elements =
                    ((com.dylibso.chicory.component.ListValue) result).elements();
            return (List<String>) (java.util.List<?>) elements;
        }
        return (List<String>) result;
    }

    public int sumNumbers(List<Integer> numbers) throws Exception {
        Object result = componentModel.callExport("sum-numbers", numbers);
        return ((Number) result).intValue();
    }

    public List<String> getNames(List<Person> people) throws Exception {
        Object result = componentModel.callExport("get-names", people);
        if (result instanceof com.dylibso.chicory.component.ListValue) {
            java.util.List<Object> elements =
                    ((com.dylibso.chicory.component.ListValue) result).elements();
            return (List<String>) (java.util.List<?>) elements;
        }
        return (List<String>) result;
    }

    public List<Person> filterHighValuePeople(List<Person> people, int minAge) throws Exception {
        Object result = componentModel.callExport("filter-high-value-people", people, minAge);
        if (result instanceof com.dylibso.chicory.component.ListValue) {
            java.util.List<Object> elements =
                    ((com.dylibso.chicory.component.ListValue) result).elements();
            return elements.stream()
                    .map(
                            e ->
                                    (Person)
                                            PojoRegistry.mapToPojo(
                                                    "person", (java.util.Map<String, Object>) e))
                    .collect(java.util.stream.Collectors.toList());
        }
        return (List<Person>) result;
    }

    public String processUserStatus(UserStatus status) throws Exception {
        Object result = componentModel.callExport("process-user-status", status);
        return (String) result;
    }

    public UserStatus createUserStatus(String name, int age, String message) throws Exception {
        Object result = componentModel.callExport("create-user-status", name, age, message);
        if (result instanceof VariantValue) {
            return (UserStatus)
                    PojoRegistry.variantValueToPojo("user-status", (VariantValue) result);
        }
        return (UserStatus) result;
    }

    public int validateResults(List<OperationResult> results) throws Exception {
        Object result = componentModel.callExport("validate-results", results);
        return ((Number) result).intValue();
    }
}
