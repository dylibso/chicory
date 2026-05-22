package com.dylibso.chicory.component.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a generated WIT component wrapper.
 *
 * <p>Used to annotate classes generated as typed wrappers for WASM components. These wrappers
 * provide type-safe methods for calling component exports and binding component imports.
 *
 * <p>Example:
 *
 * <pre>
 * &#064;WitComponent
 * public class ExampleComponent {
 *     private final ComponentModel model;
 *
 *     public ExampleComponent(ComponentModel model) {
 *         this.model = model;
 *     }
 *
 *     public String describePerson(Person person) {
 *         Object result = model.callExport("describe-person", person);
 *         return (String) result;
 *     }
 *
 *     public Person createPerson(String name, int age) {
 *         Object result = model.callExport("create-person", name, age);
 *         return Person.decode((Map<String, Object>) result);
 *     }
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WitComponent {
    /**
     * Name of the component (e.g., world name from WIT).
     *
     * @return component name
     */
    String value() default "";

    /**
     * Optional package name this component belongs to.
     *
     * @return package name, or empty string if unspecified
     */
    String packageName() default "";
}
