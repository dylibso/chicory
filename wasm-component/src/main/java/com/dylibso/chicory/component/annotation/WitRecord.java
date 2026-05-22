package com.dylibso.chicory.component.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a WIT record type.
 *
 * <p>Used to annotate classes generated from WIT record definitions. Records represent structured
 * data with named fields, similar to structs in other languages.
 *
 * <p>Example:
 *
 * <pre>
 * &#064;WitRecord
 * public class Person {
 *     private String name;
 *     private int age;
 *     private boolean active;
 *
 *     public Person(String name, int age, boolean active) {
 *         this.name = name;
 *         this.age = age;
 *         this.active = active;
 *     }
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WitRecord {
    /**
     * Optional name of the WIT record type. If not specified, class name is used.
     *
     * @return the WIT name, or empty string to use class name
     */
    String value() default "";

    /**
     * Optional version/revision number for schema tracking.
     *
     * @return version number, or 0 if unspecified
     */
    int version() default 0;
}
