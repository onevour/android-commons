package com.onevour.core.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Called once every call named here has succeeded (each with its {@link OnSuccess}), e.g. two
 * requests sent together: {@code @OnAllSuccess({"store", "stock"})}. A failure of one starts over.
 * No parameters; not private, not static.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface OnAllSuccess {

    String[] value();
}
