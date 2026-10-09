package com.onevour.core.rest;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shows every request of the REST client and its answer to one observer, on the main thread: a debug
 * screen, a network log of the app. Nothing is collected while no observer is set.
 * <pre>
 * RestInspector.set(new RestInspector.Observer() {
 *     public void onRequest(RestExchange exchange) { ... method, url, headers, body }
 *     public void onResponse(RestExchange exchange) { ... status, headers, body or error }
 * });
 * ...
 * RestInspector.set(null);
 * </pre>
 * Credentials in the headers are masked as in {@link RestLog}; the bodies are as they are, so keep
 * an observer out of a release build when they carry personal data.
 */
public final class RestInspector {

    public interface Observer {

        @MainThread
        void onRequest(RestExchange exchange);

        /** The same exchange, now complete. */
        @MainThread
        void onResponse(RestExchange exchange);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    @Nullable
    private static volatile Observer observer;

    private RestInspector() {
    }

    /** The observer, or null to stop. */
    public static void set(@Nullable Observer value) {
        observer = value;
    }

    public static boolean isActive() {
        return Objects.nonNull(observer);
    }

    /** The REST client, when a request goes out: null while nobody observes. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Nullable
    public static RestExchange begin(String method, String url, @Nullable String source, Map<String, List<String>> headers, @Nullable String body) {
        Observer current = observer;
        if (Objects.isNull(current)) return null;
        RestExchange exchange = new RestExchange(method, url, source, masked(headers), body, System.currentTimeMillis());
        exchange.startedAt = SystemClock.elapsedRealtime();
        MAIN.post(() -> current.onRequest(exchange));
        return exchange;
    }

    /** The REST client, once the answer (or the failure) is known; a second call is ignored. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void complete(@Nullable RestExchange exchange, int code, @Nullable Map<String, List<String>> headers, @Nullable String body, @Nullable Throwable error) {
        if (Objects.isNull(exchange)) return;
        long duration = SystemClock.elapsedRealtime() - exchange.startedAt;
        if (!exchange.complete(code, masked(headers), body, error, duration)) return;
        Observer current = observer;
        if (Objects.isNull(current)) return;
        MAIN.post(() -> current.onResponse(exchange));
    }

    private static Map<String, List<String>> masked(@Nullable Map<String, List<String>> headers) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (Objects.isNull(headers)) return result;
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String name = entry.getKey();
            if (Objects.isNull(name) || Objects.isNull(entry.getValue())) continue;     // the status line
            List<String> values = new ArrayList<>();
            for (String value : entry.getValue()) values.add(RestLog.redact(name, value));
            result.put(name, Collections.unmodifiableList(values));
        }
        return result;
    }
}
