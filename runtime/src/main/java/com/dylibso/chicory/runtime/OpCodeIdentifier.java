package com.dylibso.chicory.runtime;

import com.dylibso.chicory.wasm.types.OpCode;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OpCodeIdentifier {

    OpCode value();

}
