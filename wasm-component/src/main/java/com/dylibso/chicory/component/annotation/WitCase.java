package com.dylibso.chicory.component.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a variant case class within a @WitVariant.
 *
 * <p>Used to annotate inner classes that represent individual cases within a variant. Each case
 * has a discriminant value used for encoding/decoding.
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
public @interface WitCase {
    /**
     * Discriminant value for this case. Used during encoding/decoding to identify which case is
     * active.
     *
     * @return discriminant value (typically 0-based index)
     */
    int value();

    /**
     * Optional case name override. If not specified, class name is used.
     *
     * @return WIT case name, or empty string to use class name
     */
    String name() default "";
}
