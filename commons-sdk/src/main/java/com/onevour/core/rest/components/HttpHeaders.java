package com.onevour.core.rest.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HttpHeaders {

    private Map<String, List<String>> headers = new HashMap<>();

    public HttpHeaders() {
        putIfAbsent("User-Agent", "EvoRest/1.0");
    }

    public HttpHeaders(Map<String, List<String>> headerFields) {
        if (headerFields == null) {
            return;
        }

        for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
            String name = entry.getKey();

            if (name == null) {
                continue;
            }

            List<String> values = entry.getValue();

            if (values == null) {
                continue;
            }

            for (String value : values) {
                add(name, value);
            }
        }
    }

    public void add(String name, String value) {
        if (Objects.isNull(name) || Objects.isNull(value)) {
            return;
        }
        List<String> values = headers.get(name.toLowerCase());
        if (values == null) {
            values = new ArrayList<>();
            headers.put(name.toLowerCase(), values);
        }

        values.add(value);
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public String get(String name) {
        if (Objects.isNull(name)) {
            return null;
        }
        List<String> values = headers.get(name.toLowerCase());
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    public void putIfAbsent(String name, String value) {
        String exist = get(name);
        if (exist == null) {
            add(name, value);
        }
    }

    public List<String> getAll(String name) {
        List<String> values = headers.get(name);

        if (values == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(values);
    }
}
