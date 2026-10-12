package com.onevour.core.form.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A text of at least {@link #value()} characters (EvoForm). */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MinLength {
    int value();

    /** A string resource of the message; 0: the library's. */
    int message() default 0;
}
