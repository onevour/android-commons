package com.onevour.core.rest.listener;

import com.onevour.core.rest.models.HttpErrorResponse;
import com.onevour.core.rest.models.HttpResponse;

import java.lang.reflect.Type;

/**
 * The answer of a repository call, on the main thread.
 * <p>
 * The body's type comes from the repository method when its code is generated (commons-sdk-processor),
 * from {@link Typed} (e.g. {@link #of(Class, OnSuccess, OnError)}), or else from this listener's own
 * class: an anonymous {@code new HttpListener<UserResponse>() {...}} as before.
 */
public interface HttpListener<T> {

    void onSuccess(HttpResponse<T> response);

    void onError(HttpErrorResponse error);

    interface OnSuccess<T> {
        void onSuccess(HttpResponse<T> response);
    }

    interface OnError {
        void onError(HttpErrorResponse error);
    }

    /** A listener that says the type of its body itself: a lambda carries none. */
    interface Typed {
        Type responseType();
    }

    /**
     * Lambdas: {@code HttpListener.of(response -> show(response.getBody()), error -> showError(error))}.
     * The body type comes from the generated repository code; without commons-sdk-processor use
     * {@link #of(Class, OnSuccess, OnError)}, or the body stays the raw text.
     */
    static <T> HttpListener<T> of(OnSuccess<T> onSuccess, OnError onError) {
        return new TypedHttpListener<>(null, onSuccess, onError);
    }

    /** Lambdas with the body type, e.g. {@code HttpListener.of(UserResponse.class, ...)}: works with or without the processor. */
    static <T> HttpListener<T> of(Class<T> type, OnSuccess<T> onSuccess, OnError onError) {
        return new TypedHttpListener<>(type, onSuccess, onError);
    }

    /** Lambdas with a generic body type, e.g. {@code new TypeToken<List<UserResponse>>(){}.getType()}. */
    static <T> HttpListener<T> of(Type type, OnSuccess<T> onSuccess, OnError onError) {
        return new TypedHttpListener<>(type, onSuccess, onError);
    }
}
