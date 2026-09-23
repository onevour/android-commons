package com.onevour.core.rest.components;

import java.util.HashMap;
import java.util.Map;

public class HttpHeaders {

    private Map<String, String> headers = new HashMap<>();

    public HttpHeaders() {
        headers.put("User-Agent", "EvoRest/1.0");
    }

    public void put(String key, String value) {
        headers.put(key, value);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
