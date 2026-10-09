package com.onevour.core.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The @RestRepository interfaces this class calls: the processor generates {@code <Class>Api}, with
 * every repository method minus its listener; the answer goes to the {@link OnSuccess} /
 * {@link OnError} methods named after the repository method, e.g. {@code api.store(id)} to
 * {@code @OnSuccess("store")}. Their types are checked against the repository's at build time.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface RestCallbacks {

    Class<?>[] value();
}
