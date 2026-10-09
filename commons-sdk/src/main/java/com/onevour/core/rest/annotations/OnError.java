package com.onevour.core.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Receives the failure of the repository call named value() (see {@link OnSuccess}): an
 * HttpErrorResponse, or nothing. Optional: without it a failure is only logged.
 * Not called once the screen is gone (an Activity finishing or destroyed, a Fragment no longer added).
 * Not private, not static; needs annotationProcessor commons-sdk-processor.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface OnError {

    /** The call's name in this class; with @RestCallbacks, the repository method's name. */
    String value();
}
