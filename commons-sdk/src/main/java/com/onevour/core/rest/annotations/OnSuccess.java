package com.onevour.core.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Receives the answer of a repository call named value(): its parsed body ({@code UserResponse},
 * {@code List<UserResponse>}, {@code String}...) or the whole {@code HttpResponse<...>}, or nothing. The processor generates
 * {@code <Class>Callbacks.<value>(this)}, a listener to pass to the repository method; with
 * {@link RestCallbacks} on the class, {@code new <Class>Api(this).<method>(args)} calls it by itself
 * (value() is then the repository method's name).
 * Not called once the screen is gone (an Activity finishing or destroyed, a Fragment no longer added).
 * Not private, not static; needs annotationProcessor commons-sdk-processor.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface OnSuccess {

    /** The call's name in this class; with @RestCallbacks, the repository method's name. */
    String value();
}
