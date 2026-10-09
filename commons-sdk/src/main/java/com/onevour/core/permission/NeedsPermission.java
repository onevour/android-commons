package com.onevour.core.permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The method runs only once these permission groups are allowed: call the generated
 * {@code <Class>Permissions.<method>WithPermissionCheck(this, args...)} instead of the method.
 * Allowed already: the method runs at once. Not yet: the system dialog asks, then the method runs
 * when every permission is allowed, or the {@link OnPermissionDenied} method of the same groups runs.
 * <p>
 * Groups: location, camera, storage, bluetooth, phone, notifications (see PermissionHelper). The
 * method must not be private or static; the class needs annotationProcessor commons-sdk-processor,
 * and its screen forwards onRequestPermissionsResult to PermissionHelper.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface NeedsPermission {

    /** Permission groups, e.g. {"location", "camera"}. */
    String[] value();
}
