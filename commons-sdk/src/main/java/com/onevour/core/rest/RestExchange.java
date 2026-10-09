package com.onevour.core.rest;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * One request and its answer as {@link RestInspector} shows them: what was sent and what came back,
 * or the exception when nothing did. Header values that are credentials are masked as in
 * {@link RestLog}. The response part is filled once, before {@link RestInspector.Observer#onResponse}.
 */
public final class RestExchange {

    private static final AtomicLong IDS = new AtomicLong();

    private final long id = IDS.incrementAndGet();

    private final String method;

    private final String url;

    @Nullable
    private final String source;

    private final Map<String, List<String>> requestHeaders;

    @Nullable
    private final String requestBody;

    private final long sentAt;

    /** SystemClock.elapsedRealtime() when it was sent, for the duration. */
    long startedAt;

    private boolean complete;

    private int code;

    private Map<String, List<String>> responseHeaders = Collections.emptyMap();

    @Nullable
    private String responseBody;

    @Nullable
    private Throwable error;

    private long durationMs;

    RestExchange(String method, String url, @Nullable String source, Map<String, List<String>> requestHeaders, @Nullable String requestBody, long sentAt) {
        this.method = method;
        this.url = url;
        this.source = source;
        this.requestHeaders = Collections.unmodifiableMap(requestHeaders);
        this.requestBody = requestBody;
        this.sentAt = sentAt;
    }

    /** False when it was already complete. */
    boolean complete(int code, Map<String, List<String>> headers, @Nullable String body, @Nullable Throwable error, long durationMs) {
        if (complete) return false;
        this.complete = true;
        this.code = code;
        this.responseHeaders = Collections.unmodifiableMap(headers);
        this.responseBody = body;
        this.error = error;
        this.durationMs = durationMs;
        return true;
    }

    /** Unique in the process: matches onResponse to its onRequest. */
    public long getId() {
        return id;
    }

    public String getMethod() {
        return method;
    }

    /** The url as sent: queries appended, path values in. */
    public String getUrl() {
        return url;
    }

    /** Repository.method of a @RestRepository call; null for RestRequest used directly. */
    @Nullable
    public String getSource() {
        return source;
    }

    /** Every header sent, User-Agent included, credentials masked. */
    public Map<String, List<String>> getRequestHeaders() {
        return requestHeaders;
    }

    @Nullable
    public String getRequestBody() {
        return requestBody;
    }

    /** System.currentTimeMillis() when it was sent. */
    public long getSentAt() {
        return sentAt;
    }

    public boolean isComplete() {
        return complete;
    }

    /** The HTTP status; 0 when no answer came (timeout, no network). */
    public int getCode() {
        return code;
    }

    public boolean isSuccessful() {
        return complete && error == null && code >= 200 && code < 300;
    }

    /** The answer's headers, credentials masked. */
    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

    /** The answer's text, an error status's too. */
    @Nullable
    public String getResponseBody() {
        return responseBody;
    }

    /** Why no answer could be read: timeout, no network, unreadable body. */
    @Nullable
    public Throwable getError() {
        return error;
    }

    public long getDurationMs() {
        return durationMs;
    }
}
