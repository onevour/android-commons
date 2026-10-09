package com.onevour.core.rest.listener;

import com.onevour.core.rest.RestLog;
import com.onevour.core.rest.models.HttpErrorResponse;
import com.onevour.core.rest.models.HttpResponse;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * An {@link HttpListener} that carries its body type and forwards to two callbacks; what
 * {@link HttpListener#of} and the generated @OnSuccess / @OnError code build.
 */
public class TypedHttpListener<T> implements HttpListener<T>, HttpListener.Typed {

    private final Type type;

    private final OnSuccess<T> onSuccess;

    private final OnError onError;

    public TypedHttpListener(Type type, OnSuccess<T> onSuccess, OnError onError) {
        this.type = type;
        this.onSuccess = onSuccess;
        this.onError = onError;
    }

    @Override
    public Type responseType() {
        return type;
    }

    @Override
    public void onSuccess(HttpResponse<T> response) {
        if (Objects.nonNull(onSuccess)) onSuccess.onSuccess(response);
    }

    @Override
    public void onError(HttpErrorResponse error) {
        if (Objects.nonNull(onError)) {
            onError.onError(error);
            return;
        }
        RestLog.basic("error " + error.getCode() + " not handled: " + error.getMessage());
    }
}
