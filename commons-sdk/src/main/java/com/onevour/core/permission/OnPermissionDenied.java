package com.onevour.core.permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runs when the {@link NeedsPermission} method of the same groups could not run: a permission was
 * refused, or Android will not ask any more and Settings was opened. No parameters; not private or
 * static. Optional: without it, a refusal does nothing.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface OnPermissionDenied {

    /** The same groups as the NeedsPermission method, in any order. */
    String[] value();
}
