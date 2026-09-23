package com.onevour.core.rest.models;

import com.google.gson.annotations.Expose;

public class Response<T> {

    @Expose
    int code;

    @Expose
    String message;

    @Expose
    T result;

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

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public boolean success() {
        return code >= 200 && code < 300;
    }

    public boolean error() {
        return code >= 300;
    }
}
