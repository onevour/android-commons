package com.onevour.core.rest;

import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * What the REST client writes to logcat, under one tag ({@value #TAG}). Off by default, so a
 * release build logs nothing; an app turns it on for its debug build:
 * <pre>
 * RestLog.setLevel(BuildConfig.DEBUG ? RestLog.Level.BASIC : RestLog.Level.NONE);
 * </pre>
 * Credentials never reach the log as they are: the values of {@code Authorization},
 * {@code Proxy-Authorization}, {@code Cookie}, {@code Set-Cookie} and {@code X-Api-Key} (and any
 * header added with {@link #addSensitiveHeader}) show only their last characters, at every level.
 */
public final class RestLog {

    public static final String TAG = "EvoRest";

    public enum Level {
        /** Nothing (the default). */
        NONE,
        /** One line per call and its outcome: method, url, status, time; errors in short. */
        BASIC,
        /** BASIC and the request headers, credentials masked. */
        HEADERS,
        /** HEADERS and the bodies, path values, error details and stack traces. */
        BODY
    }

    private static volatile Level level = Level.NONE;

    private static final Set<String> sensitive = new CopyOnWriteArraySet<>();

    static {
        sensitive.add("authorization");
        sensitive.add("proxy-authorization");
        sensitive.add("cookie");
        sensitive.add("set-cookie");
        sensitive.add("x-api-key");
    }

    private RestLog() {
    }

    public static void setLevel(Level value) {
        level = value == null ? Level.NONE : value;
    }

    public static Level getLevel() {
        return level;
    }

    /** One more header whose value is masked (e.g. an app's own token header). */
    public static void addSensitiveHeader(String name) {
        if (name != null) sensitive.add(name.toLowerCase(Locale.ROOT));
    }

    public static boolean isLoggable(Level at) {
        return level != Level.NONE && level.ordinal() >= at.ordinal();
    }

    public static void basic(String message) {
        if (isLoggable(Level.BASIC)) Log.d(TAG, message);
    }

    public static void header(String name, String value) {
        if (isLoggable(Level.HEADERS)) Log.d(TAG, "  " + name + ": " + redact(name, value));
    }

    public static void body(String message) {
        if (isLoggable(Level.BODY)) Log.d(TAG, message);
    }

    /** A failure: in short from BASIC, with its stack trace at BODY. */
    public static void error(String message, @Nullable Throwable error) {
        if (!isLoggable(Level.BASIC)) return;
        if (error != null && isLoggable(Level.BODY)) {
            Log.e(TAG, message, error);
        } else {
            Log.e(TAG, error == null ? message : message + ": " + error);
        }
    }

    /** The header's value as it may be logged: masked for a credential. */
    public static String redact(String name, String value) {
        if (name == null || value == null || !sensitive.contains(name.toLowerCase(Locale.ROOT))) return value;
        // keep the scheme ("Bearer ") so the log still says what kind of credential it was
        int space = value.indexOf(' ');
        if (space > 0 && space < value.length() - 1) return value.substring(0, space + 1) + mask(value.substring(space + 1));
        return mask(value);
    }

    /** A secret as "••••Fow4": its last four characters only, nothing for a short one. */
    public static String mask(String secret) {
        if (secret == null || secret.isEmpty()) return String.valueOf(secret);
        return secret.length() <= 8 ? "••••" : "••••" + secret.substring(secret.length() - 4);
    }
}
