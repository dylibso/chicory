package com.dylibso.chicory.component.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a WIT variant type (tagged union/discriminated union).
 *
 * <p>Used to annotate sealed abstract classes generated from WIT variant definitions. Variants
 * represent tagged unions with multiple possible cases, where only one case is active at any time.
 *
 * <p>Example:
 *
 * <pre>
 * &#064;WitVariant
 * public sealed abstract class Result permits Result.Ok, Result.Err {
 *     &#064;WitCase(0)
 *     public static final class Ok extends Result {
 *         private final String value;
 *
 *         public Ok(String value) {
 *             this.value = value;
 *         }
 *     }
 *
 *     &#064;WitCase(1)
 *     public static final class Err extends Result {
 *         private final int code;
 *
 *         public Err(int code) {
 *             this.code = code;
 *         }
 *     }
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WitVariant {
    /**
     * Optional name of the WIT variant type. If not specified, class name is used.
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
