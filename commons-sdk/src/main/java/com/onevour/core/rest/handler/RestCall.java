package com.onevour.core.rest.handler;

import com.onevour.core.rest.RestLog;
import com.onevour.core.rest.components.HttpHeaders;
import com.onevour.core.rest.configurations.HttpTimeout;
import com.onevour.core.rest.listener.HttpListener;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One repository call, built the same way by the code generated for a @RestRepository and by the
 * Proxy fallback, so both send exactly the same request:
 * <ul>
 *     <li>the url is key + url; queries are appended URL-encoded (UTF-8) in their order, a later
 *     one of the same name wins; then each {name} is replaced by its @Path value, as text;</li>
 *     <li>@Header values in parameter order, then an HttpHeaders parameter's; Content-Type only
 *     when none was given;</li>
 *     <li>a String body is sent as it is, anything else as JSON (GsonHelper); GET sends none.</li>
 * </ul>
 */
public final class RestCall {

    private final String method;

    private final String url;

    private final HttpTimeout timeout = new HttpTimeout();

    private final String contentType;

    private final String source;

    private final HttpHeaders headers = new HttpHeaders();

    private final Map<String, Object> queries = new LinkedHashMap<>();

    private final Map<String, Object> paths = new LinkedHashMap<>();

    private Object body;

    /**
     * @param method      GET, POST, PUT, PATCH or DELETE
     * @param url         key + url of the annotation
     * @param connect     connect timeout in seconds (0: the default)
     * @param read        read timeout in seconds (0: the default)
     * @param contentType Content-Type unless a header gives one
     * @param source      Repository.method, for the log
     */
    public RestCall(String method, String url, int connect, int read, String contentType, String source) {
        this.method = method;
        this.url = url;
        this.timeout.setValue(connect, read);
        this.contentType = contentType;
        this.source = source;
    }

    public RestCall path(String name, Object value) {
        paths.put(name, value);
        RestLog.body("  path " + name + " = " + value);
        return this;
    }

    public RestCall query(String name, Object value) {
        queries.put(name, value);
        return this;
    }

    public RestCall header(String name, Object value) {
        headers.add(name, String.valueOf(value));
        return this;
    }

    /** An HttpHeaders parameter: added after the @Header ones. */
    public RestCall headers(HttpHeaders values) {
        if (Objects.isNull(values)) return this;
        for (Map.Entry<String, List<String>> entry : values.getHeaders().entrySet()) {
            List<String> list = entry.getValue();
            if (Objects.isNull(list) || list.isEmpty()) continue;
            for (String value : list) headers.add(entry.getKey(), value);
        }
        return this;
    }

    public RestCall body(Object value) {
        this.body = value;
        return this;
    }

    /** The url as it will be sent. */
    public String url() {
        String result = url;
        if (!queries.isEmpty()) {
            StringBuilder query = new StringBuilder();
            for (Map.Entry<String, Object> entry : queries.entrySet()) {
                if (query.length() > 0) query.append('&');
                query.append(encode(entry.getKey())).append('=').append(encode(String.valueOf(entry.getValue())));
            }
            result = result + "?" + query;
        }
        for (Map.Entry<String, Object> entry : paths.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }

    /**
     * Sends it; responseType is the body's type (null: from the listener, as the Proxy fallback does).
     */
    public <T> void send(Type responseType, HttpListener<T> listener) {
        String finalUrl = url();
        headers.putIfAbsent("Content-Type", contentType);
        RestLog.basic(method + " " + finalUrl + "  [" + source + "]");
        RestRequest.send(method, finalUrl, timeout, headers, body, responseType, listener, source);
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);                    // UTF-8 is always there
        }
    }
}
