/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.onevour.core.rest.handler;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.JsonSyntaxException;

import com.onevour.core.rest.components.HttpMultipart;
import com.onevour.core.rest.components.HttpRequest;
import com.onevour.core.rest.listener.HttpListener;
import com.onevour.core.rest.models.HttpErrorResponse;
import com.onevour.core.rest.models.HttpResponse;
import com.onevour.core.utilities.json.gson.GsonHelper;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Zuliadin
 */
public class RestExecutor {

    private static final String TAG = RestExecutor.class.getSimpleName();

    private final int MAX_POOL = 16;

    private static RestExecutor restExecutor;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private ExecutorService executor = Executors.newFixedThreadPool(MAX_POOL);

    private RestExecutor() {
        if (null == executor) executor = Executors.newFixedThreadPool(MAX_POOL);
    }

    public static RestExecutor newInstance() {
        if (null == restExecutor) restExecutor = new RestExecutor();
        return restExecutor;
    }

    @SuppressWarnings({"rawtypes"})
    protected void add(HttpRequest httpRequest) {
        if (null == executor) executor = Executors.newFixedThreadPool(MAX_POOL);
        executor.execute(httpRequest::request);
    }

    @SuppressWarnings({"unchecked"})
    protected  <T> void add(final HttpMultipart multipart, final HttpListener<T> listener) {
        if (null == executor) executor = Executors.newFixedThreadPool(MAX_POOL);
        executor.execute(() -> {
            if (null == listener) return;
            StringBuffer responseString = new StringBuffer("");

            try {
                multipart.request();
                List<String> response = multipart.finish();

                HttpResponse<T> responseHttp = new HttpResponse<T>(multipart.getHeaderFields());
                responseHttp.setCode(multipart.getResponseCode());

                for (String s : response) {
                    responseString.append(s);
                }
                final Type responseType = getResponseType(listener);
                Log.d(TAG, responseString.toString());
                if (null == responseType) {
                    T responseBody = (T) responseString.toString();
                    responseHttp.setBody(responseBody);
                    handler.post(() -> listener.onSuccess(responseHttp));
                } else {
                    String responseResult = responseString.toString();
                    try {
                        final T jsonResponse = GsonHelper.newInstance().getGson().fromJson(responseResult, responseType);
                        responseHttp.setBody(jsonResponse);
                        handler.post(() -> listener.onSuccess(responseHttp));
                    } catch (JsonSyntaxException e) {
                        HttpErrorResponse httpErrorResponse = new HttpErrorResponse(multipart.getResponseCode(), "Cannot convert response \n:".concat(responseResult));
                        handler.post(() -> listener.onError(httpErrorResponse));
                    }
                }
            } catch (final IOException e) {

                for (StackTraceElement s : e.getStackTrace()) {
                    Log.e(TAG, String.valueOf(s));
                }
                HttpErrorResponse httpErrorResponse = new HttpErrorResponse(multipart.getResponseCode(), e);
                handler.post(() -> listener.onError(httpErrorResponse));
            } catch (JsonSyntaxException e) {
                HttpErrorResponse httpErrorResponse = new HttpErrorResponse(multipart.getResponseCode());
                handler.post(() -> listener.onError(httpErrorResponse));
            } finally {
                Log.d(TAG, "process upload finish");
            }
        });
    }

    protected void stop() {
        executor.shutdown();
    }

    /**
     * get type from interface
     */
    private <T> Type getResponseType(HttpListener<T> listener) {
        if (null == listener) return null;
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
