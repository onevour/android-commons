package org.cise.core.utilities.http;

import android.content.Context;

import org.cise.core.utilities.json.gson.GsonHelper;

/**
 * Created by Zuliadin on 09/01/2017.
 */

public class ApiRequest {

    private static final String TAG = "ApiRequest";

    private static HttpQueue queue(){
        return HttpQueue.newInstance();
    }

    @SuppressWarnings("unchecked")
    public static <T> void get(Context context,String url, HttpResponse.Listener<T> listener) {
        queue().add(new HttpRequest(context,url, listener));
    }

    public static <T> void post(Context context,String url, T json) {
        post(context, url, json, null);
    }

    @SuppressWarnings("unchecked")
    public static <T,E> void post(Context context,String url, T json, HttpResponse.Listener<E> listener) {
        if (json instanceof String) {
            queue().add(new HttpRequest(context, url, (String) json, listener));
        } else {
            String body = GsonHelper.newInstance().getGson().toJson(json);
            queue().add(new HttpRequest(context, url, body, listener));
        }
    }

    @SuppressWarnings("unchecked")
    public static <T,E> void post(Context context,String url, int timeout, T json, HttpResponse.Listener<E> listener) {
        if (json instanceof String) {
            queue().add(new HttpRequest(context,url, timeout, (String) json, listener));
        } else {
            queue().add(new HttpRequest(context, url, timeout, GsonHelper.newInstance().getGson().toJson(json), listener));
        }
    }

    public static <T> void post(HttpMultipart request, HttpResponse.Listener<T> listener) {
        queue().add(request, listener);
    }

}