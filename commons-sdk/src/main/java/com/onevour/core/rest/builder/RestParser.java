package com.onevour.core.rest.builder;

import android.util.Log;

import com.onevour.core.rest.annotations.Body;
import com.onevour.core.rest.annotations.Delete;
import com.onevour.core.rest.annotations.Get;
import com.onevour.core.rest.annotations.Patch;
import com.onevour.core.rest.annotations.Path;
import com.onevour.core.rest.annotations.Post;
import com.onevour.core.rest.annotations.Put;
import com.onevour.core.rest.components.HttpHeaders;
import com.onevour.core.rest.listener.HttpListener;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RestParser {

    private static final String TAG = RestParser.class.getSimpleName();

    String url;

    int connect = -1;

    int request = -1;

    int read = -1;

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
            initializeConfiguration(config.key(), config.url(), config.connect(), config.request(), config.read(), config.contentType());
            return;
        }

        if (method.isAnnotationPresent(Get.class)) {
            methodName = "get";
            Get config = method.getAnnotation(Get.class);
            initializeConfiguration(config.key(), config.url(), config.connect(), config.request(), config.read(), config.contentType());
            return;
        }

        if (method.isAnnotationPresent(Put.class)) {
            methodName = "put";
            Put config = method.getAnnotation(Put.class);
            initializeConfiguration(config.key(), config.url(), config.connect(), config.request(), config.read(), config.contentType());
            return;
        }

        if (method.isAnnotationPresent(Patch.class)) {
            methodName = "patch";
            Patch config = method.getAnnotation(Patch.class);
            initializeConfiguration(config.key(), config.url(), config.connect(), config.request(), config.read(), config.contentType());
            return;
        }

        if (method.isAnnotationPresent(Delete.class)) {
            methodName = "delete";
            Delete config = method.getAnnotation(Delete.class);
            initializeConfiguration(config.key(), config.url(), config.connect(), config.request(), config.read(), config.contentType());
            return;
        }

        throw new IllegalArgumentException("Http method not found");
    }

    private void initializeConfiguration(String key, String url, int connectTimeout, int requestTimeout, int readTimeout, String contentType) {
        this.url = key + url;
        this.connect = connectTimeout;
        this.request = requestTimeout;
        this.read = readTimeout;
        this.contentType = contentType;
    }


    @SuppressWarnings("rawtypes")
    public void resolveArgument(Object[] args) {
        Annotation[][] annotations = method.getParameterAnnotations();
        Map<String, Object> paths = new HashMap<>();

        for (int i = 0; i < annotations.length; i++) {

            Object value = args != null ? args[i] : null;

            for (Annotation annotation : annotations[i]) {

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
            }
        }

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

    public int getConnect() {
        return connect;
    }

    public int getRequest() {
        return request;
    }

    public int getRead() {
        return read;
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
}
