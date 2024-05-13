package com.dylibso.chicory.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Allows to react to Web Assembly module test failures with additional actions.
 */
@ExtendWith(ChicoryTestWatcher.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ChicoryTest {

    /**
     * Creates a {@code wasm-objdump} for the module used by failing test case.
     * If set to {@literal false} no module is dumped.
     *
     * @return
     */
    boolean dumpOnFail() default true;
}
