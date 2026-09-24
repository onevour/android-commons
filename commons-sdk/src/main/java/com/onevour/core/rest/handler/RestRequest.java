package com.onevour.core.rest.handler;

import com.onevour.core.rest.components.HttpHeaders;
import com.onevour.core.rest.components.HttpMultipart;
import com.onevour.core.rest.components.HttpRequest;
import com.onevour.core.rest.configurations.HttpTimeout;
import com.onevour.core.rest.listener.HttpListener;
import com.onevour.core.utilities.json.gson.GsonHelper;

import java.util.List;
import java.util.Map;

/**
 * Created by Zuliadin on 09/01/2017.
 */

@SuppressWarnings({"unchecked", "rawtypes"})
public class RestRequest {

    private static final String TAG = RestRequest.class.getSimpleName();

    private static RestExecutor queue() {
        return RestExecutor.newInstance();
    }

    public static <T> void get(String url, HttpTimeout timeout, HttpHeaders header, HttpListener<T> listener) {
        queue().add(new HttpRequest(url, "GET", timeout, header, null, listener));
    }

    public static <T, E> void post(String url, HttpTimeout timeout, HttpHeaders header, T json, HttpListener<E> listener) {
        if (json instanceof String) {
            queue().add(new HttpRequest(url, "POST", timeout, header, (String) json, listener));
        } else {
            queue().add(new HttpRequest(url, "POST", timeout, header, GsonHelper.newInstance().getGson().toJson(json), listener));
        }
    }

    public static <T, E> void put(String url, HttpTimeout timeout, HttpHeaders header, T json, HttpListener<E> listener) {
        if (json instanceof String) {
            queue().add(new HttpRequest(url, "PUT", timeout, header, (String) json, listener));
        } else {
            String body = GsonHelper.newInstance().getGson().toJson(json);
            queue().add(new HttpRequest(url, "PUT", timeout, header, body, listener));
        }
    }

    public static <T, E> void patch(String url, HttpTimeout timeout, HttpHeaders header, T json, HttpListener<E> listener) {
        if (json instanceof String) {
            queue().add(new HttpRequest(url, "PATCH", timeout, header, (String) json, listener));
        } else {
            queue().add(new HttpRequest(url, "PATCH", timeout, header, GsonHelper.newInstance().getGson().toJson(json), listener));
        }
    }

    public static <T, E> void delete(String url, HttpTimeout timeout, HttpHeaders header, T json, HttpListener<E> listener) {
        if (json instanceof String) {
            queue().add(new HttpRequest(url, "DELETE", timeout, header, (String) json, listener));
        } else {
            queue().add(new HttpRequest(url, "DELETE", timeout, header, GsonHelper.newInstance().getGson().toJson(json), listener));
        }
    }

    public static <T> void post(HttpMultipart request, HttpListener<T> listener) {
        queue().add(request, listener);
    }

}
