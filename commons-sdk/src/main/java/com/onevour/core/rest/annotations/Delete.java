package com.onevour.core.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Delete {

    String key() default "";

    String url() default "";

    int connect() default -1;

    int request() default -1;

    int read() default -1;

    String contentType() default "application/json";
}