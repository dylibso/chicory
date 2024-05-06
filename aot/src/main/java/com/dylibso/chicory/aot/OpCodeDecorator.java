package com.dylibso.chicory.aot;

import com.dylibso.chicory.wasm.types.OpCode;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OpCodeDecorator {

    OpCode value();
}
