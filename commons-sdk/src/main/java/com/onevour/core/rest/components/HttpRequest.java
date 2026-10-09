/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onevour.core.rest.components;

import com.onevour.core.rest.RestExchange;
import com.onevour.core.rest.RestInspector;
import com.onevour.core.rest.RestLog;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.onevour.core.rest.configurations.HttpTimeout;
import com.onevour.core.rest.listener.HttpListener;
import com.onevour.core.rest.models.HttpErrorResponse;
import com.onevour.core.rest.models.HttpResponse;
import com.onevour.core.utilities.json.gson.GsonHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

/**
 * @author zuliadin
 */
public class HttpRequest<T> {

    private final String TAG = HttpRequest.class.getSimpleName();

    // private final int MIN_TIMEOUT = 1000;

    private int timeoutConnection = 0;
    private int timeoutRead = 0;

    private String endpoint;

    private String method;

    private HttpHeaders header;

    private String body;

    private HttpListener<T> listener;

    /** The body's type when the caller knows it (generated repository code); null: from the listener. */
    private Type responseType;

    /** Repository.method, for RestInspector. */
    private String source;

    /** This request for RestInspector; null while nobody observes. */
    private RestExchange exchange;

    // DYNAMIC
    public HttpRequest(String url, String method, HttpTimeout timeout, HttpHeaders header, String body, HttpListener<T> listener) {
        initialize(url, method, timeout, header, body, listener);
    }

    /** With the body's type known (the generated repository code knows it from the method). */
    public HttpRequest(String url, String method, HttpTimeout timeout, HttpHeaders header, String body, Type responseType, HttpListener<T> listener) {
        initialize(url, method, timeout, header, body, listener);
        this.responseType = responseType;
    }

    private void initialize(String url, String method, HttpTimeout timeout, HttpHeaders header, String body, HttpListener<T> listener) {
        this.endpoint = url;
        this.timeoutConnection = Math.max(timeout.getConnect() * 1000, 1500);
        this.timeoutRead = Math.max(timeout.getRead() * 1000, 4500);
        this.method = method;
        this.header = header;
        this.body = body;
        this.listener = listener;
    }

    /** Repository.method that sends it, shown by RestInspector. */
    public HttpRequest<T> source(String source) {
        this.source = source;
        return this;
    }

    public void request() {
        if (null == endpoint || "".equalsIgnoreCase(endpoint)) return;
        if (RestInspector.isActive()) exchange = RestInspector.begin(method, endpoint, source, sentHeaders(), body);
        if (endpoint.startsWith("https")) {
            requestHTTPS();
        } else {
            requestHTTP();
        }
    }

    private String getMediaType(String contentType) {
        if (contentType == null) {
            return null;
        }

        return contentType
                .split(";", 2)[0]
                .trim()
                .toLowerCase();
    }

    private void requestHTTP() {
        final long startedAt = SystemClock.elapsedRealtime();
        final StringBuffer response = new StringBuffer();
        HttpURLConnection conn = null;
        int responseCode = 0;
        try {
            URL url = new URL(this.endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(timeoutRead);
            conn.setConnectTimeout(timeoutConnection);
            conn.setRequestMethod(method());
            conn.setDoOutput(output());
            enableAutoClose(conn);
            enableHeader(conn);
            enableBody(conn);
            responseCode = conn.getResponseCode();
            RestLog.basic(method() + " " + endpoint + " → " + responseCode + " (" + (SystemClock.elapsedRealtime() - startedAt) + " ms)");

            HttpResponse httpResponse = new HttpResponse(conn.getHeaderFields());
            httpResponse.setCode(responseCode);
            if (responseCode == 204) {
                RestInspector.complete(exchange, responseCode, conn.getHeaderFields(), null, null);
                successHandler(null, httpResponse, null);
                return;
            }
            if (responseCode >= 200 && responseCode < 300) {
                buildResponse(conn, response);
                RestLog.body("  " + response);
                RestInspector.complete(exchange, responseCode, conn.getHeaderFields(), response.toString(), null);
                successHandler(getResponseType(), httpResponse, response);
            } else {
                RestInspector.complete(exchange, responseCode, conn.getHeaderFields(), errorBody(conn), null);
                errorHandler(httpResponse);
            }
//        } catch (final JsonParseException ex) {
//            errorHandler(ex);
//        } catch (final MalformedURLException ex) {
//            errorHandler(ex);
//        } catch (final SocketTimeoutException ex) {
//            errorHandler(ex);
//        } catch (final IOException ex) {
//            errorHandler(ex);
        } catch (final Exception ex) {
            RestInspector.complete(exchange, responseCode, null, null, ex);
            errorHandler(responseCode, ex);
        } finally {
            if (null != conn) conn.disconnect();
        }
    }

    private void requestHTTPS() {
        final long startedAt = SystemClock.elapsedRealtime();
        final StringBuffer response = new StringBuffer();
        HttpsURLConnection conn = null;
        int responseCode = 0;
        try {
            URL url = new URL(this.endpoint);
            conn = (HttpsURLConnection) url.openConnection();
            conn.setConnectTimeout(timeoutConnection);
            conn.setReadTimeout(timeoutRead);
            conn.setRequestMethod(method());
            conn.setDoOutput(output());
            enableAutoClose(conn);
            enableHeader(conn);
            enableSSLOnApiBeforeLollipop(conn);
            enableBody(conn);
            responseCode = conn.getResponseCode();
            RestLog.basic(method() + " " + endpoint + " → " + responseCode + " (" + (SystemClock.elapsedRealtime() - startedAt) + " ms)");
            HttpResponse httpResponse = new HttpResponse(conn.getHeaderFields());
            httpResponse.setCode(responseCode);
            if (responseCode == 204) {
                RestInspector.complete(exchange, responseCode, conn.getHeaderFields(), null, null);
                successHandler(null, httpResponse, null);
                return;
            }
            if (responseCode >= 200 && responseCode < 300) {

                buildResponse(conn, response);
                RestLog.body("  " + response);
                RestInspector.complete(exchange, responseCode, conn.getHeaderFields(), response.toString(), null);
                successHandler(getResponseType(), httpResponse, response);
            } else {
                RestInspector.complete(exchange, responseCode, conn.getHeaderFields(), errorBody(conn), null);
                errorHandler(httpResponse);
            }
//        } catch (final JsonParseException ex) {
//            errorHandler(ex);
//        } catch (final MalformedURLException ex) {
//            errorHandler(ex);
//        } catch (final SocketTimeoutException ex) {
//            errorHandler(ex);
//        } catch (final IOException ex) {
//            errorHandler(ex);
        } catch (final Exception ex) {
            RestInspector.complete(exchange, responseCode, null, null, ex);
            errorHandler(responseCode, ex);
        } finally {
            if (null != conn) conn.disconnect();
        }
    }

    private void enableAutoClose(HttpURLConnection conn) {
        // conn.setRequestProperty("Content-Type", "application/json");
        // conn.setRequestProperty("connection", "close");
    }

    private void enableBody(HttpURLConnection conn) throws IOException {
        if (null == body) return;
        OutputStream os = conn.getOutputStream();
        os.write(body.getBytes());
        os.flush();
        os.close();
    }

    private void buildResponse(HttpURLConnection conn, StringBuffer response) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
    }

    /** An error status's body, read only for RestInspector (the listener never got it). */
    private String errorBody(HttpURLConnection conn) {
        if (Objects.isNull(exchange)) return null;
        try {
            if (Objects.isNull(conn.getErrorStream())) return null;
            StringBuilder text = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                String line;
                while (Objects.nonNull(line = in.readLine())) text.append(line).append('\n');
            }
            return text.toString().trim();
        } catch (IOException e) {
            return null;
        }
    }

    /** The headers enableHeader sends, User-Agent included. */
    private Map<String, List<String>> sentHeaders() {
        Map<String, List<String>> sent = new LinkedHashMap<>();
        if (Objects.nonNull(header)) {
            for (Map.Entry<String, List<String>> entry : header.getHeaders().entrySet()) {
                sent.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
        if (Objects.isNull(header) || Objects.isNull(header.get("User-Agent"))) {
            sent.put("User-Agent", Collections.singletonList(HttpHeaders.DEFAULT_USER_AGENT));
        }
        return sent;
    }

    private boolean output() {
        return null != body;
    }

    private String method() {
        return method;
    }

    private void enableHeader(HttpURLConnection conn) {
        if (header != null) {
            for (Map.Entry<String, List<String>> entry : header.getHeaders().entrySet()) {
                List<String> values = entry.getValue();
                for (String value : values) {
                    RestLog.header(entry.getKey(), value);
                    conn.setRequestProperty(entry.getKey(), value);
                }

            }
        }
        // the app's own User-Agent wins; the default only when it declares none
        if (header == null || header.get("User-Agent") == null) {
            RestLog.header("user-agent", HttpHeaders.DEFAULT_USER_AGENT);
            conn.setRequestProperty("User-Agent", HttpHeaders.DEFAULT_USER_AGENT);
        }
    }

    private void enableSSLOnApiBeforeLollipop(HttpsURLConnection conn) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < Build.VERSION_CODES.LOLLIPOP) {
            if (endpoint.startsWith("https")) {
                try {
                    TLSSocketFactory sc = new TLSSocketFactory();
                    conn.setSSLSocketFactory(sc);
                } catch (Exception e) {
                    RestLog.error("TLS for " + endpoint, e);
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void successHandler(Type responseType, HttpResponse httpResponse, StringBuffer response) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (Objects.isNull(listener)) {
                    RestLog.basic("listener not implement");
                    return;
                }
                if (Objects.isNull(responseType) && Objects.isNull(response)) {
                    listener.onSuccess(httpResponse);
                    return;
                }
                HttpHeaders headers = httpResponse.getHeaders();
                String contentType = headers.get("Content-Type");
                String getMediaType = getMediaType(contentType);
                // unknown content type: the raw text, once (the listener was called twice before)
                if (Objects.isNull(contentType)) {
                    T body = (T) response.toString();
                    httpResponse.setBody(body);
                    listener.onSuccess(httpResponse);
                    return;
                }
                if ("application/json".equalsIgnoreCase(getMediaType)) {
                    if (Objects.isNull(responseType)) {
                        T body = (T) response.toString();
                        httpResponse.setBody(body);
                        listener.onSuccess(httpResponse);
                        return;
                    }
                    final T jsonResponse = GsonHelper.newInstance().getGson().fromJson(response.toString().trim(), responseType);
                    httpResponse.setBody(jsonResponse);
                    listener.onSuccess(httpResponse);
                    return;
                }
                // default
                T body = (T) response.toString();
                httpResponse.setBody(body);
                listener.onSuccess(httpResponse);


            } catch (Exception e) {
                HttpErrorResponse errorResponse = new HttpErrorResponse(httpResponse);
                listener.onError(errorResponse);
            }
        });
    }

    private void errorHandler(int responseCode, Exception error) {
        if (Objects.isNull(listener)) {
            RestLog.basic("listener not implement");
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            listener.onError(new HttpErrorResponse(responseCode, error));
        });
        RestLog.error(method() + " " + endpoint + " failed", error);
    }

    private void errorHandler(HttpResponse httpResponse) {
        if (Objects.isNull(listener)) {
            RestLog.basic("listener not implement");
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> listener.onError(new HttpErrorResponse(httpResponse)));
        RestLog.error("error hit api " + endpoint + " | " + httpResponse.getCode() + " | " + method(), null);
        RestLog.body("  " + httpResponse);
    }

    /**
     * The body's type: given by the caller (generated code), said by a {@link HttpListener.Typed}
     * listener (lambdas), or read from the listener's class (an anonymous HttpListener<X>).
     */
    private Type getResponseType() {
        if (null != responseType) return responseType;
        if (null == listener) return null;
        if (listener instanceof HttpListener.Typed) {
            Type typed = ((HttpListener.Typed) listener).responseType();
            if (null != typed) return typed;
        }
        Type[] types = listener.getClass().getGenericInterfaces();
        for (Type type : types) {
            if (type instanceof ParameterizedType) {
                Type[] gTypes = ((ParameterizedType) type).getActualTypeArguments();
                for (Type gType : gTypes) {
                    return gType;
                }
            }
        }
        return null;
    }

}
