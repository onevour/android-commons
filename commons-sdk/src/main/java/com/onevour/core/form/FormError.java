package com.onevour.core.form;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.Arrays;
import java.util.Objects;

/**
 * A field's error as a {@link EvoForm} keeps it, without a Context: a string resource with its arguments, or a
 * text (e.g. from the server). The UI ({@link EvoFormView}) shows {@link #text(Context)}; a test compares the resource.
 */
public final class FormError {

    @StringRes
    public final int resource;

    @NonNull
    public final Object[] args;

    @Nullable
    public final String text;

    private FormError(@StringRes int resource, @Nullable String text, Object... args) {
        this.resource = resource;
        this.text = text;
        this.args = Objects.isNull(args) ? new Object[0] : args;
    }

    public static FormError of(@StringRes int resource, Object... args) {
        return new FormError(resource, null, args);
    }

    public static FormError of(@NonNull String text) {
        return new FormError(0, text);
    }

    public String text(@NonNull Context context) {
        if (Objects.nonNull(text)) return text;
        return args.length == 0 ? context.getString(resource) : context.getString(resource, args);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FormError)) return false;
        FormError other = (FormError) o;
        return resource == other.resource && Objects.equals(text, other.text) && Arrays.equals(args, other.args);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, text, Arrays.hashCode(args));
    }

    @NonNull
    @Override
    public String toString() {
        return Objects.nonNull(text) ? text : "@" + resource + Arrays.toString(args);
    }
}
