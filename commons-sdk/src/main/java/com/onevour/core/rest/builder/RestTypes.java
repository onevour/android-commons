package com.onevour.core.rest.builder;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;

/**
 * Body types for the generated code, e.g. {@code RestTypes.of(List.class, UserResponse.class)} for
 * {@code List<UserResponse>}; what Gson needs to parse the body.
 */
public final class RestTypes {

    private RestTypes() {
    }

    public static Type of(Class<?> raw, Type... arguments) {
        return arguments.length == 0 ? raw : new Parameterized(raw, arguments);
    }

    public static Type array(Type component) {
        return component instanceof Class ? java.lang.reflect.Array.newInstance((Class<?>) component, 0).getClass() : new GenericArray(component);
    }

    private static final class Parameterized implements ParameterizedType {

        private final Class<?> raw;
        private final Type[] arguments;

        Parameterized(Class<?> raw, Type[] arguments) {
            this.raw = raw;
            this.arguments = arguments.clone();
        }

        @Override
        public Type[] getActualTypeArguments() {
            return arguments.clone();
        }

        @Override
        public Type getRawType() {
            return raw;
        }

        @Override
        public Type getOwnerType() {
            return raw.getEnclosingClass();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ParameterizedType)) return false;
            ParameterizedType that = (ParameterizedType) other;
            return raw.equals(that.getRawType()) && Arrays.equals(arguments, that.getActualTypeArguments())
                    && Objects.equals(getOwnerType(), that.getOwnerType());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(arguments) ^ raw.hashCode() ^ Objects.hashCode(getOwnerType());
        }

        @Override
        public String toString() {
            StringBuilder text = new StringBuilder(raw.getTypeName()).append('<');
            for (int i = 0; i < arguments.length; i++) {
                if (i > 0) text.append(", ");
                text.append(arguments[i].getTypeName());
            }
            return text.append('>').toString();
        }
    }

    private static final class GenericArray implements GenericArrayType {

        private final Type component;

        GenericArray(Type component) {
            this.component = component;
        }

        @Override
        public Type getGenericComponentType() {
            return component;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof GenericArrayType && component.equals(((GenericArrayType) other).getGenericComponentType());
        }

        @Override
        public int hashCode() {
            return component.hashCode();
        }

        @Override
        public String toString() {
            return component.getTypeName() + "[]";
        }
    }
}
