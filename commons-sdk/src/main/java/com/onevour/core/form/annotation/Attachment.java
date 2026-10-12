package com.onevour.core.form.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Ids of the form's draft attachments (photos), a Long or a list of Long (EvoForm): committed by saved(), discarded with the draft. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Attachment {
}
