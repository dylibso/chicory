package com.dylibso.chicory.aot.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotates a program element that is (only) used by generated code.
 * This can be used to prevent warnings about program elements that
 * static analysis tools flag as unused.
 */
@Target(ElementType.METHOD)
public @interface UsedByGeneratedCode {}
