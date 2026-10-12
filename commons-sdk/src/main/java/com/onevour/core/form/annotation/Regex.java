package com.onevour.core.form.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A text matching {@link #value()} (EvoForm). */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Regex {
    String value();

    /** A string resource of the message. */
    int message();
}
