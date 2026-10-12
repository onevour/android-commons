package com.onevour.core.form.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A whole or decimal number, comma or dot (EvoForm). */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Numeric {
    /** A string resource of the message; 0: the library's. */
    int message() default 0;
}
