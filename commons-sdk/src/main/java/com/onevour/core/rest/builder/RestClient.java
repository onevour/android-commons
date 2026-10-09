package com.onevour.core.rest.builder;

import com.onevour.core.rest.handler.RestInvocationHandler;
import com.onevour.core.rest.repository.RestRepository;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Makes a @RestRepository: the code commons-sdk-processor generated for it ({@code <Repository>_Rest},
 * no reflection) when the app has the processor, otherwise a Proxy that reads the annotations at run
 * time. Both build the same requests.
 */
public final class RestClient {

    /** Generated implementations are stateless: one per repository. */
    private static final Map<Class<?>, Object> GENERATED = new ConcurrentHashMap<>();

    private final boolean useGenerated;

    public RestClient() {
        this(true);
    }

    /** @param useGenerated false: always the Proxy (e.g. to compare both in tests) */
    public RestClient(boolean useGenerated) {
        this.useGenerated = useGenerated;
    }

    public <T> T create(Class<T> repositoryClass) {
        validateRepository(repositoryClass);
        if (useGenerated) {
            Object generated = generated(repositoryClass);
            if (Objects.nonNull(generated)) return repositoryClass.cast(generated);
        }
        RestInvocationHandler handler = new RestInvocationHandler(repositoryClass);
        return repositoryClass.cast(Proxy.newProxyInstance(repositoryClass.getClassLoader(), new Class<?>[]{repositoryClass}, handler));
    }

    /** The generated implementation, or null when the app has no commons-sdk-processor. */
    private static Object generated(Class<?> repositoryClass) {
        Object cached = GENERATED.get(repositoryClass);
        if (Objects.nonNull(cached)) return cached;
        try {
            Class<?> implementation = Class.forName(generatedName(repositoryClass), true, repositoryClass.getClassLoader());
            if (!GeneratedRepository.class.isAssignableFrom(implementation) || !repositoryClass.isAssignableFrom(implementation)) return null;
            Object instance = implementation.getDeclaredConstructor().newInstance();
            GENERATED.put(repositoryClass, instance);
            return instance;
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    /** com.app.UserRepository -> com.app.UserRepository_Rest; com.app.Api$Users -> com.app.Api_Users_Rest. */
    static String generatedName(Class<?> repositoryClass) {
        String name = repositoryClass.getName();
        int dot = name.lastIndexOf('.');
        String packagePrefix = dot < 0 ? "" : name.substring(0, dot + 1);
        return packagePrefix + name.substring(dot + 1).replace('$', '_') + "_Rest";
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
