package com.onevour.core.rest.handler;

import com.onevour.core.rest.annotations.Body;
import com.onevour.core.rest.annotations.Delete;
import com.onevour.core.rest.annotations.Get;
import com.onevour.core.rest.annotations.Header;
import com.onevour.core.rest.annotations.Patch;
import com.onevour.core.rest.annotations.Path;
import com.onevour.core.rest.annotations.Post;
import com.onevour.core.rest.annotations.Put;
import com.onevour.core.rest.annotations.Query;
import com.onevour.core.rest.components.HttpHeaders;
import com.onevour.core.rest.listener.HttpListener;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * The Proxy fallback of a @RestRepository, when its code was not generated (no commons-sdk-processor):
 * reads the method's annotations at run time and builds the same {@link RestCall} the generated
 * code does; the body's type comes from the listener.
 */
public class RestInvocationHandler implements InvocationHandler {

    private final Class<?> repositoryClass;

    public RestInvocationHandler(Class<?> repositoryClass) {
        this.repositoryClass = repositoryClass;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) return method.invoke(this, args);
        if (method.getReturnType() != Void.TYPE) {
            throw new IllegalArgumentException("Repository method must return void");
        }
        RestCall call = call(method);
        Annotation[][] annotations = method.getParameterAnnotations();
        Class<?>[] types = method.getParameterTypes();
        HttpHeaders httpHeaders = null;
        HttpListener listener = null;
        for (int i = 0; i < annotations.length; i++) {
            Object value = args != null ? args[i] : null;
            for (Annotation annotation : annotations[i]) {
                if (annotation instanceof Header) call.header(((Header) annotation).value(), value);
                if (annotation instanceof Path) call.path(((Path) annotation).value(), value);
                if (annotation instanceof Body) call.body(value);
                if (annotation instanceof Query) call.query(((Query) annotation).value(), value);
            }
            if (Objects.isNull(httpHeaders) && HttpHeaders.class.isAssignableFrom(types[i])) httpHeaders = (HttpHeaders) value;
            if (Objects.isNull(listener) && HttpListener.class.isAssignableFrom(types[i])) listener = (HttpListener) value;
        }
        call.headers(httpHeaders);
        call.send(null, listener);
        return null;
    }

    private RestCall call(Method method) {
        String source = repositoryClass.getSimpleName() + "." + method.getName();
        Post post = method.getAnnotation(Post.class);
        if (Objects.nonNull(post)) return new RestCall("POST", post.key() + post.url(), post.connect(), post.read(), post.contentType(), source);
        Get get = method.getAnnotation(Get.class);
        if (Objects.nonNull(get)) return new RestCall("GET", get.key() + get.url(), get.connect(), get.read(), get.contentType(), source);
        Put put = method.getAnnotation(Put.class);
        if (Objects.nonNull(put)) return new RestCall("PUT", put.key() + put.url(), put.connect(), put.read(), put.contentType(), source);
        Patch patch = method.getAnnotation(Patch.class);
        if (Objects.nonNull(patch)) return new RestCall("PATCH", patch.key() + patch.url(), patch.connect(), patch.read(), patch.contentType(), source);
        Delete delete = method.getAnnotation(Delete.class);
        if (Objects.nonNull(delete)) return new RestCall("DELETE", delete.key() + delete.url(), delete.connect(), delete.read(), delete.contentType(), source);
        throw new IllegalArgumentException("Http method not found");
    }
}
