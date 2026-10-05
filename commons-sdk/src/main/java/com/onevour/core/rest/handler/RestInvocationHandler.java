package com.onevour.core.rest.handler;

import com.onevour.core.rest.RestLog;

import com.onevour.core.rest.builder.RestParser;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Locale;

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
        RestParser configuration = new RestParser(method);
        configuration.resolveArgument(args);
        RestLog.basic(String.valueOf(configuration.getMethodName()).toUpperCase(Locale.ROOT) + " " + configuration.getUrl()
                + "  [" + repositoryClass.getSimpleName() + "." + method.getName() + "]");

        if ("get".equalsIgnoreCase(configuration.getMethodName())) {
            RestRequest.get(configuration.getUrl(), configuration.getTimeout(), configuration.getHeaders(), configuration.getHttpListener());
        }
        if ("post".equalsIgnoreCase(configuration.getMethodName())) {
            RestRequest.post(configuration.getUrl(), configuration.getTimeout(), configuration.getHeaders(), configuration.getBody(), configuration.getHttpListener());
        }
        if ("put".equalsIgnoreCase(configuration.getMethodName())) {
            RestRequest.put(configuration.getUrl(), configuration.getTimeout(), configuration.getHeaders(), configuration.getBody(), configuration.getHttpListener());
        }
        if ("patch".equalsIgnoreCase(configuration.getMethodName())) {
            RestRequest.patch(configuration.getUrl(), configuration.getTimeout(), configuration.getHeaders(), configuration.getBody(), configuration.getHttpListener());
        }
        if ("delete".equalsIgnoreCase(configuration.getMethodName())) {
            RestRequest.delete(configuration.getUrl(), configuration.getTimeout(), configuration.getHeaders(), configuration.getBody(), configuration.getHttpListener());
        }
        return null;
    }


}