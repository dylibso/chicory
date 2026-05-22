package com.dylibso.chicory.component.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field within a WIT record with metadata for serialization.
 *
 * <p>Used to annotate fields in classes marked with @WitRecord. Provides field ordering and type
 * information necessary for correct encoding/decoding.
 *
 * <p>Example:
 *
 * <pre>
 * &#064;WitRecord
 * public class Person {
 *     &#064;WitField(order = 0)
 *     private String name;
 *
 *     &#064;WitField(order = 1)
 *     private int age;
 *
 *     &#064;WitField(order = 2)
 *     private boolean active;
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface WitField {
    /**
     * Order of this field in the record. Must match the order in the WIT definition.
     *
     * @return field order (0-based)
     */
    int order();

    /**
     * Optional field name override. If not specified, Java field name is used.
     *
     * @return WIT field name, or empty string to use Java field name
     */
    String value() default "";

    /**
     * Whether this field is optional (nullable).
     *
     * @return true if field is optional
     */
    boolean optional() default false;
}
