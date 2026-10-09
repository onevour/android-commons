package com.onevour.core.utilities.beans;

// The BeanCopy before its 2026-10 fix, kept only to compare speed in BeanCopyBenchmarkTest.

import android.util.Log;

import com.google.gson.Gson;
import com.onevour.core.utilities.commons.ValueOf;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings({"CollectionAddAllCanBeReplacedWithConstructor", "unchecked", "rawtypes"})
class LegacyBeanCopy {

    private static final String TAG = "LegacyBeanCopy";

    private static final Map<String, Set<String>> cached = new HashMap<>();

    public static <S, T> T gson(S source, Class<T> target) {
        if (ValueOf.isNull(source)) throw new NullPointerException();
        Gson gson = new Gson();
        String json = gson.toJson(source);
        return gson.fromJson(json, target);
    }
    public static <S, T> T gson(S source, Type target) {
        if (ValueOf.isNull(source)) throw new NullPointerException();
        Gson gson = new Gson();
        String json = gson.toJson(source);
        return gson.fromJson(json, target);
    }

    public static <S, T> T value(S source, Class<T> target, String... ignore) {
        if (ValueOf.isNull(source)) throw new NullPointerException();
        try {
            Constructor constructor = target.getConstructor();
            T newInstance = (T) constructor.newInstance();
            copyValue(source, newInstance, ignore);
            return newInstance;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
                 InstantiationException e) {
            Log.e(TAG, "error copy value " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static <S, T> List<T> values(List<S> source, Class<T> target, String... ignore) {
        List<T> result = new ArrayList<>();
        if (ValueOf.isNull(source)) return result;
        if (source.isEmpty()) return result;
        for (Object o : source) {
            result.add(value(o, target, ignore));
        }
        return result;
    }

    public static <S, T> void copyValue(S source, T target, String... ignore) {
        Set<String> ignoreSet = new HashSet<>();
        ignoreSet.addAll(Arrays.asList(ignore));
        Class<?> clazzSource = source.getClass();
        Class<?> clazzTarget = target.getClass();

        // copy name
        StringBuilder sb = new StringBuilder();
        sb.append(clazzSource.getSimpleName());
        sb.append("To");
        sb.append(clazzTarget.getSimpleName());
        // class copy key name
        String name = sb.toString();
        // temp cached
        Set<String> sets = cached.get(name);
        if (Objects.isNull(sets)) {
            sets = new HashSet<>();
            List<Field> fieldSources = getAllModelFields(clazzSource);
            List<Field> fieldTargets = getAllModelFields(clazzTarget);
            for (Field field : fieldSources) {
                // ignore field whitelist and transient
                if (ignoreSet.contains(field.getName())) continue;
                if (Modifier.isTransient(field.getModifiers())) continue;
                // ignore name and type if not match
                for (Field fieldTarget : fieldTargets) {
                    // ignore field whitelist and transient
                    if (ignoreSet.contains(fieldTarget.getName())) continue;
                    if (!field.getName().equalsIgnoreCase(fieldTarget.getName())) continue;
                    if (!field.getType().equals(fieldTarget.getType())) continue;
                    if (Modifier.isTransient(fieldTarget.getModifiers())) continue;
                    try {
                        field.setAccessible(true);
                        fieldTarget.setAccessible(true);
                        fieldTarget.set(target, field.get(source));
                        sets.add(fieldTarget.getName());
                        break;
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            cached.put(name, sets);
        } else {
            assert sets != null;
            for (String fieldName : sets) {
                try {
                    Field fieldSource = clazzSource.getDeclaredField(fieldName);
                    Field fieldTarget = clazzTarget.getDeclaredField(fieldName);
                    fieldSource.setAccessible(true);
                    fieldTarget.setAccessible(true);
                    fieldTarget.set(target, fieldSource.get(source));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    protected static List<Field> getAllModelFields(Class aClass) {
        List<Field> fields = new ArrayList<>();
        do {
            Collections.addAll(fields, aClass.getDeclaredFields());
            aClass = aClass.getSuperclass();
        } while (aClass != null);
        return fields;
    }
}
