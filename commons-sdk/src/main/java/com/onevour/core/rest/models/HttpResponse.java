package com.onevour.core.rest.models;


import com.onevour.core.rest.components.HttpHeaders;

import java.util.List;
import java.util.Map;

public class HttpResponse<T> {

    HttpHeaders headers;

    int code;

    String message;

    T body;

    public HttpResponse(Map<String, List<String>> headerFields) {
        this.headers = new HttpHeaders(headerFields);
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(HttpHeaders headers) {
        this.headers = headers;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }
}
