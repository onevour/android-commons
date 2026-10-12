package com.onevour.core.form.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Required (EvoForm): not null, not blank, not false, not an empty list. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Required {
    /** A string resource of the message; 0: the library's. */
    int message() default 0;
}
