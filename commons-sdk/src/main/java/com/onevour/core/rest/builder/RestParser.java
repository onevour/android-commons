package com.onevour.core.rest.builder;

import android.util.Log;

import com.onevour.core.rest.annotations.Body;
import com.onevour.core.rest.annotations.Delete;
import com.onevour.core.rest.annotations.Get;
import com.onevour.core.rest.annotations.Header;
import com.onevour.core.rest.annotations.Patch;
import com.onevour.core.rest.annotations.Path;
import com.onevour.core.rest.annotations.Post;
import com.onevour.core.rest.annotations.Put;
import com.onevour.core.rest.annotations.Query;
import com.onevour.core.rest.components.HttpHeaders;
import com.onevour.core.rest.configurations.HttpTimeout;
import com.onevour.core.rest.listener.HttpListener;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RestParser {

    private static final String TAG = RestParser.class.getSimpleName();

    String url;

    HttpTimeout timeout = new HttpTimeout();

    String contentType = "application/json";

    HttpHeaders headers = new HttpHeaders();

    Method method;

    String methodName;

    Object body;

    HttpListener httpListener;

    public RestParser(Method method) {
        this.method = method;
        initializeParameterMethod(method);
    }

    private void initializeParameterMethod(Method method) {
        if (method.isAnnotationPresent(Post.class)) {
            methodName = "post";
            Post config = method.getAnnotation(Post.class);
            initializeConfiguration(config.key(), config.url(), config.connect(), config.read(), config.contentType());
            return;
        }

        if (method.isAnnotationPresent(Get.class)) {
            methodName = "get";
            Get config = method.getAnnotation(Get.class);
            initializeConfiguration(config.key(), config.url(), config.connect(), config.read(), config.contentType());
            return;
        }

        if (method.isAnnotationPresent(Put.class)) {
            methodName = "put";
            Put config = method.getAnnotation(Put.class);
            initializeConfiguration(config.key(), config.url(), config.connect(), config.read(), config.contentType());
            return;
        }

        if (method.isAnnotationPresent(Patch.class)) {
            methodName = "patch";
            Patch config = method.getAnnotation(Patch.class);
            initializeConfiguration(config.key(), config.url(), config.connect(), config.read(), config.contentType());
            return;
        }

        if (method.isAnnotationPresent(Delete.class)) {
            methodName = "delete";
            Delete config = method.getAnnotation(Delete.class);
            initializeConfiguration(config.key(), config.url(), config.connect(), config.read(), config.contentType());
            return;
        }

        throw new IllegalArgumentException("Http method not found");
    }

    private void initializeConfiguration(String key, String url, int connectTimeout, int readTimeout, String contentType) {
        this.url = key + url;
        timeout.setValue(connectTimeout, readTimeout);
        this.contentType = contentType;
    }


    @SuppressWarnings("rawtypes")
    public void resolveArgument(Object[] args) throws UnsupportedEncodingException {
        Annotation[][] annotations = method.getParameterAnnotations();
        Map<String, Object> queries = new LinkedHashMap<>();
        Map<String, Object> paths = new HashMap<>();

        for (int i = 0; i < annotations.length; i++) {

            Object value = args != null ? args[i] : null;

            for (Annotation annotation : annotations[i]) {
                if (annotation instanceof Header) {
                    Header path = (Header) annotation;
                    headers.add(path.value(), String.valueOf(value));
                }
                if (annotation instanceof Path) {
                    Path path = (Path) annotation;
                    String name = path.value();
                    paths.put(name, value);
                    Log.d(TAG, "Path name  = " + name);
                    Log.d(TAG, "Path value = " + value);
                }
                if (annotation instanceof Body) {
                    this.body = value;
                }
                if (annotation instanceof Query) {
                    Query path = (Query) annotation;
                    // this.body = value;
                    queries.put(path.value(), value);
                }
            }
        }

        // update url
        updateUrlFromQuery(queries);

        // header
        HttpHeaders httpHeaders = determineHeaders(method, args);
        if (Objects.nonNull(httpHeaders)) {
            for (Map.Entry<String, List<String>> entry : httpHeaders.getHeaders().entrySet()) {
                String key = entry.getKey();
                List<String> values = entry.getValue();
                if (Objects.isNull(values) || values.isEmpty()) continue;
                for (String value : values) {
                    headers.add(key, value);
                }

            }
        }
        // listener
        HttpListener httpListener = determineHttpListener(method, args);
        if (Objects.nonNull(httpListener)) {
            this.httpListener = httpListener;
        }

        // rebuild url
        for (Map.Entry<String, Object> entry : paths.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            url = url.replace("{" + key + "}", String.valueOf(value));
        }

    }

    private void updateUrlFromQuery(Map<String, Object> queries) throws UnsupportedEncodingException {
        StringBuilder query = new StringBuilder();

        for (Map.Entry<String, Object> entry : queries.entrySet()) {
            if (query.length() > 0) {
                query.append("&");
            }

            query.append(URLEncoder.encode(entry.getKey(), "UTF-8"));

            query.append("=");

            query.append(URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"));
        }

        String queryString = query.toString();
        this.url = url + "?" + queryString;
    }

    private HttpHeaders determineHeaders(Method method, Object[] args) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (HttpHeaders.class.isAssignableFrom(parameterTypes[i])) {
                return (HttpHeaders) args[i];
            }
        }
        return null;
    }


    private HttpListener determineHttpListener(Method method, Object[] args) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (HttpListener.class.isAssignableFrom(parameterTypes[i])) {
                return (HttpListener) args[i];
            }
        }
        return null;
    }

    public String getUrl() {
        return url;
    }

    public String getContentType() {
        return contentType;
    }

    public HttpHeaders getHeaders() {
        headers.putIfAbsent("Content-Type", contentType);
        return headers;
    }

    public Object getBody() {
        return body;
    }

    public HttpListener getHttpListener() {
        return httpListener;
    }

    public String getMethodName() {
        return methodName;
    }

    public HttpTimeout getTimeout() {
        return timeout;
    }
}
