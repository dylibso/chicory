package com.example.generated;

import com.dylibso.chicory.component.ComponentModel;
import java.util.List;

/**
 * Generated SDK wrapper for the example component.
 * Provides typed methods for calling WASM exports.
 *
 * Supports both:
 * - POJO-based calls: describePerson(Person)
 * - Flexible calls for progressive testing: describePerson(String, int, boolean)
 */
public class ExampleComponent {
    private final ComponentModel component;

    public ExampleComponent(ComponentModel component) {
        this.component = component;
    }

    public int add(int a, int b) {
        Object result = component.callExport("add", a, b);
        return ((Number) result).intValue();
    }

    public long multiply(long a, long b) {
        Object result = component.callExport("multiply", a, b);
        return ((Number) result).longValue();
    }

    public boolean isPositive(int n) {
        Object result = component.callExport("is-positive", n);
        return (Boolean) result;
    }

    public String greet(String name) {
        Object result = component.callExport("greet", name);
        return (String) result;
    }

    public String processText(String text) {
        Object result = component.callExport("process-text", text);
        return (String) result;
    }

    // POJO-based call
    public String describePerson(Person person) {
        Object result = component.callExport("describe-person", person);
        return (String) result;
    }

    // Flexible call for progressive testing - accepts individual fields
    public String describePerson(String name, int age, boolean active) {
        Person person = new Person(name, age, active);
        Object result = component.callExport("describe-person", person);
        return (String) result;
    }

    public Person createPerson(String name, int age) {
        Object result = component.callExport("create-person", name, age);
        return (Person) result;
    }

    public Color pickColor(int index) {
        Object result = component.callExport("pick-color", index);
        return (Color) result;
    }

    public OperationResult getResult() {
        Object result = component.callExport("get-result");
        return (OperationResult) result;
    }

    public String repeatString(String s, int count) {
        Object result = component.callExport("repeat-string", s, count);
        return (String) result;
    }

    public long sumNumbers(List<Integer> numbers) {
        Object result = component.callExport("sum-numbers", numbers);
        return ((Number) result).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<String> getNames(List<?> people) {
        Object result = component.callExport("get-names", people);
        // ListValue implements List interface
        if (result instanceof java.util.List) {
            return (List<String>) result;
        }
        return new java.util.ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public List<?> filterHighValuePeople(List<?> people, int minAge) {
        Object result = component.callExport("filter-high-value-people", people, minAge);
        // ListValue implements List interface
        if (result instanceof java.util.List) {
            return (List<?>) result;
        }
        return new java.util.ArrayList<>();
    }

    public UserStatus createUserStatus(Person person, OperationResult status) {
        Object result = component.callExport("create-user-status", person, status);
        return (UserStatus) result;
    }

    public String processUserStatus(UserStatus status) {
        Object result = component.callExport("process-user-status", status);
        return (String) result;
    }
}
