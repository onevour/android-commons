package com.onevour.core.form.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The view of the field (EvoForm), when its id name differs from the field's name (normalPrice also finds normal_price). */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Bind {
    /** The view id's name, e.g. "cio_normal_price". */
    String value();
}
