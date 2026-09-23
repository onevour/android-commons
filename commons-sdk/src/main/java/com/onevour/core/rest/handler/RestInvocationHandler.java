package com.onevour.core.rest.handler;

import android.util.Log;

import com.onevour.core.rest.builder.RestParser;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class RestInvocationHandler implements InvocationHandler {

    private static final String TAG = RestInvocationHandler.class.getSimpleName();

    private final Class<?> repositoryClass;

    public RestInvocationHandler(Class<?> repositoryClass) {
        this.repositoryClass = repositoryClass;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getReturnType() != Void.TYPE) {
            throw new IllegalArgumentException(
                    "Repository method must return void"
            );
        }
        Log.d(TAG, "Repository : " + repositoryClass.getSimpleName());
        Log.d(TAG, "Method     : " + method.getName());

        RestParser configuration = new RestParser(method);
        configuration.resolveArgument(args);
        Log.d(TAG, "Base URL   : " + configuration.getUrl());

        if ("get".equalsIgnoreCase(configuration.getMethodName())) {
            RestRequest.get(configuration.getUrl(), configuration.getConnect(), configuration.getHeaders(), configuration.getHttpListener());
        }
        if ("post".equalsIgnoreCase(configuration.getMethodName())) {
            RestRequest.post(configuration.getUrl(), configuration.getConnect(), configuration.getHeaders(), configuration.getBody(), configuration.getHttpListener());
        }
        if ("put".equalsIgnoreCase(configuration.getMethodName())) {
            RestRequest.put(configuration.getUrl(), configuration.getConnect(), configuration.getHeaders(), configuration.getBody(), configuration.getHttpListener());
        }
        if ("patch".equalsIgnoreCase(configuration.getMethodName())) {
            RestRequest.patch(configuration.getUrl(), configuration.getConnect(), configuration.getHeaders(), configuration.getBody(), configuration.getHttpListener());
        }
        if ("delete".equalsIgnoreCase(configuration.getMethodName())) {
            RestRequest.delete(configuration.getUrl(), configuration.getConnect(), configuration.getHeaders(), configuration.getBody(), configuration.getHttpListener());
        }
        return null;
    }


}