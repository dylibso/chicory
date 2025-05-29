package com.dylibso.chicory.host.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WasmModuleInterface {
    /*
     * for wasmFile we support two kind of values:
     * - "file:/" format with an absolute path
     * - local files included in the current compilation unit resources
     */
    String value();
}
