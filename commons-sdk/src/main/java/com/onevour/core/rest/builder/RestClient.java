package com.onevour.core.rest.builder;


import com.onevour.core.rest.repository.RestRepository;

import java.lang.reflect.Proxy;

public final class RestClient {


    public <T> T create(Class<T> repositoryClass) {
        validateRepository(repositoryClass);

        RestInvocationHandler handler = new RestInvocationHandler(repositoryClass);
        return repositoryClass.cast(Proxy.newProxyInstance(repositoryClass.getClassLoader(), new Class<?>[]{repositoryClass}, handler));
    }

    private void validateRepository(Class<?> repositoryClass) {

        if (repositoryClass == null) {
            throw new IllegalArgumentException("Repository class must not be null");
        }

        if (!repositoryClass.isInterface()) {
            throw new IllegalArgumentException("Repository must be an interface: " + repositoryClass.getName());
        }

        if (!repositoryClass.isAnnotationPresent(RestRepository.class)) {
            throw new IllegalArgumentException("Repository must be annotated with @RestRepository: " + repositoryClass.getName());
        }
    }


}