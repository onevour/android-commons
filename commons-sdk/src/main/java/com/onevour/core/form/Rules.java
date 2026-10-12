package com.onevour.core.form;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.onevour.core.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * The checks of one field of an {@link EvoForm}, as react-hook-form's register options: the first one that fails
 * gives the field's error. Messages are a string or a string resource (shown in the app's language).
 *
 * <pre>
 * Rules.required().number().min(0)
 * Rules.required(R.string.err_name).maxLength(50)
 * Rules.of().custom((value, values) -> !value.equals(values.get("normal_price")), R.string.err_same_price)
 * </pre>
 */
public final class Rules {

    /** One check: the error message, or null when the value passes. */
    interface Check {
        @Nullable
        String test(Context context, @Nullable Object value, Map<String, Object> values);
    }

    private final List<Check> checks = new ArrayList<>();

    private Rules() {
    }

    /** No check yet. */
    public static Rules of() {
        return new Rules();
    }

    /** Required: not null, not blank text, not false, not an empty list. */
    public static Rules required() {
        return new Rules().requiredCheck(null, 0);
    }

    public static Rules required(@StringRes int message) {
        return new Rules().requiredCheck(null, message);
    }

    public static Rules required(String message) {
        return new Rules().requiredCheck(message, 0);
    }

    /** A whole or decimal number (an empty value passes: add required for that). */
    public Rules number() {
        return number(0);
    }

    /** {@code message}: a string resource, 0 for the library's. */
    public Rules number(@StringRes int message) {
        checks.add((context, value, values) -> isEmpty(value) || toNumber(value) != null ? null
                : message(context, message, R.string.commons_form_number));
        return this;
    }

    public Rules min(double min) {
        return min(min, 0);
    }

    public Rules min(double min, @StringRes int message) {
        checks.add((context, value, values) -> {
            Double number = toNumber(value);
            return isEmpty(value) || (number != null && number >= min) ? null
                    : message(context, message, R.string.commons_form_min, format(min));
        });
        return this;
    }

    public Rules max(double max) {
        return max(max, 0);
    }

    public Rules max(double max, @StringRes int message) {
        checks.add((context, value, values) -> {
            Double number = toNumber(value);
            return isEmpty(value) || (number != null && number <= max) ? null
                    : message(context, message, R.string.commons_form_max, format(max));
        });
        return this;
    }

    public Rules minLength(int length) {
        return minLength(length, 0);
    }

    public Rules minLength(int length, @StringRes int message) {
        checks.add((context, value, values) -> isEmpty(value) || String.valueOf(value).length() >= length ? null
                : message(context, message, R.string.commons_form_min_length, length));
        return this;
    }

    public Rules maxLength(int length) {
        return maxLength(length, 0);
    }

    public Rules maxLength(int length, @StringRes int message) {
        checks.add((context, value, values) -> isEmpty(value) || String.valueOf(value).length() <= length ? null
                : message(context, message, R.string.commons_form_max_length, length));
        return this;
    }

    public Rules pattern(String regex, @StringRes int message) {
        Pattern pattern = Pattern.compile(regex);
        checks.add((context, value, values) -> isEmpty(value) || pattern.matcher(String.valueOf(value)).matches() ? null
                : context.getString(message));
        return this;
    }

    public Rules email() {
        return email(0);
    }

    public Rules email(@StringRes int message) {
        return pattern("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message != 0 ? message : R.string.commons_form_email);
    }

    /** A check of the value alone. */
    public Rules custom(Predicate<Object> valid, @StringRes int message) {
        checks.add((context, value, values) -> valid.test(value) ? null : context.getString(message));
        return this;
    }

    /** A check of the value against the other values (e.g. end date after start date). */
    public Rules custom(BiPredicate<Object, Map<String, Object>> valid, @StringRes int message) {
        checks.add((context, value, values) -> valid.test(value, values) ? null : context.getString(message));
        return this;
    }

    public Rules custom(BiPredicate<Object, Map<String, Object>> valid, String message) {
        checks.add((context, value, values) -> valid.test(value, values) ? null : message);
        return this;
    }

    /** The first failing check's message, null when all pass. */
    @Nullable
    String validate(@NonNull Context context, @Nullable Object value, Map<String, Object> values) {
        for (Check check : checks) {
            String error = check.test(context, value, values);
            if (error != null) return error;
        }
        return null;
    }

    private Rules requiredCheck(@Nullable String message, @StringRes int resource) {
        checks.add((context, value, values) -> {
            if (!isEmpty(value) && !Boolean.FALSE.equals(value)) return null;
            if (message != null) return message;
            return context.getString(resource != 0 ? resource : R.string.commons_form_required);
        });
        return this;
    }

    /** The rule's own message when it has one, else the library's (with its argument). */
    private static String message(Context context, @StringRes int own, @StringRes int library, Object... args) {
        return own != 0 ? context.getString(own, args) : context.getString(library, args);
    }

    /** The checks of {@code other} added after these. */
    Rules add(Rules other) {
        checks.addAll(other.checks);
        return this;
    }

    /** Whether no check was added. */
    boolean isEmptyRules() {
        return checks.isEmpty();
    }

    static boolean isEmpty(@Nullable Object value) {
        if (Objects.isNull(value)) return true;
        if (value instanceof CharSequence) return value.toString().trim().isEmpty();
        if (value instanceof java.util.Collection) return ((java.util.Collection<?>) value).isEmpty();
        return false;
    }

    @Nullable
    static Double toNumber(@Nullable Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (Objects.isNull(value)) return null;
        String text = String.valueOf(value).trim().replace(",", ".");
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String format(double number) {
        return number == Math.rint(number) ? String.valueOf((long) number) : String.valueOf(number);
    }
}
