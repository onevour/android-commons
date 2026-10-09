package com.onevour.core.utilities.beans;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.onevour.core.utilities.commons.ValueOf;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Copies the values of one object into another: fields with the same name (case does not matter)
 * and exactly the same type, transient and static fields left out, superclass fields included.
 * <p>
 * Which field goes where is worked out once per source class, target class and ignore list, then
 * reused: the same copy is fast, and safe from several threads.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class BeanCopy {

    private static final String TAG = BeanCopy.class.getSimpleName();

    /** Source field to target field, by (source class, target class, ignored names). */
    private static final Map<Mapping, List<FieldPair>> MAPPINGS = new ConcurrentHashMap<>();

    /** Deep copies: every non-transient field, a Date with its milliseconds, NaN allowed. */
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Date.class, new EpochDateAdapter())
            .serializeSpecialFloatingPointValues()
            .create();

    /** A deep copy through JSON: lists and nested objects are new ones too. */
    public static <S, T> T gson(S source, Class<T> target) {
        if (ValueOf.isNull(source)) throw new NullPointerException("cannot copy a null source");
        return GSON.fromJson(GSON.toJson(source), target);
    }

    public static <S, T> T gson(S source, Type target) {
        if (ValueOf.isNull(source)) throw new NullPointerException("cannot copy a null source");
        return GSON.fromJson(GSON.toJson(source), target);
    }

    /** A new target (it needs a public no-argument constructor) with the source's values. */
    public static <S, T> T value(S source, Class<T> target, String... ignore) {
        if (ValueOf.isNull(source)) throw new NullPointerException("cannot copy a null source");
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

    /** Copies the source's values into an existing target, except the fields named in ignore. */
    public static <S, T> void copyValue(S source, T target, String... ignore) {
        if (Objects.isNull(source)) throw new NullPointerException("cannot copy a null source");
        if (Objects.isNull(target)) throw new NullPointerException("cannot copy into a null target");
        Mapping key = new Mapping(source.getClass(), target.getClass(), ignore);
        List<FieldPair> pairs = MAPPINGS.get(key);
        if (Objects.isNull(pairs)) {
            pairs = map(key);
            MAPPINGS.put(key, pairs);
        }
        for (FieldPair pair : pairs) {
            try {
                pair.target.set(target, pair.source.get(source));
            } catch (IllegalAccessException e) {
                Log.e(TAG, "cannot copy " + pair.source.getName() + ": " + e.getMessage());
            }
        }
    }

    /** For each source field, the first target field of the same name (any case) and type. */
    private static List<FieldPair> map(Mapping key) {
        List<FieldPair> pairs = new ArrayList<>();
        List<Field> targetFields = getAllModelFields(key.target);
        for (Field field : getAllModelFields(key.source)) {
            if (!copyable(field, key.ignore)) continue;
            for (Field fieldTarget : targetFields) {
                if (!copyable(fieldTarget, key.ignore)) continue;
                if (!field.getName().equalsIgnoreCase(fieldTarget.getName())) continue;
                if (!field.getType().equals(fieldTarget.getType())) continue;
                try {
                    field.setAccessible(true);
                    fieldTarget.setAccessible(true);
                    pairs.add(new FieldPair(field, fieldTarget));
                    break;
                } catch (RuntimeException e) {             // a field the platform does not open
                    Log.e(TAG, "cannot open " + field.getName() + ": " + e.getMessage());
                }
            }
        }
        return Collections.unmodifiableList(pairs);
    }

    private static boolean copyable(Field field, Collection<String> ignore) {
        int modifiers = field.getModifiers();
        return !ignore.contains(field.getName())
                && !Modifier.isTransient(modifiers)
                && !Modifier.isStatic(modifiers)
                && !field.isSynthetic();
    }

    protected static List<Field> getAllModelFields(Class aClass) {
        List<Field> fields = new ArrayList<>();
        do {
            Collections.addAll(fields, aClass.getDeclaredFields());
            aClass = aClass.getSuperclass();
        } while (aClass != null);
        return fields;
    }

    /** Tests: forget the mappings worked out so far. */
    static void clearMappings() {
        MAPPINGS.clear();
    }

    /** Source class, target class and ignored names: the classes themselves, not their (maybe renamed) names. */
    private static final class Mapping {

        final Class<?> source;
        final Class<?> target;
        final Collection<String> ignore;
        private final int hash;

        Mapping(Class<?> source, Class<?> target, String[] ignore) {
            this.source = source;
            this.target = target;
            this.ignore = Collections.unmodifiableSet(new TreeSet<>(Arrays.asList(ignore)));
            this.hash = Objects.hash(source, target, this.ignore);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Mapping)) return false;
            Mapping that = (Mapping) other;
            return source == that.source && target == that.target && ignore.equals(that.ignore);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static final class FieldPair {

        final Field source;
        final Field target;

        FieldPair(Field source, Field target) {
            this.source = source;
            this.target = target;
        }
    }

    /** Gson's default Date text has no milliseconds: copies keep them as epoch milliseconds. */
    private static final class EpochDateAdapter extends TypeAdapter<Date> {

        @Override
        public void write(JsonWriter out, Date value) throws IOException {
            if (Objects.isNull(value)) {
                out.nullValue();
                return;
            }
            out.value(value.getTime());
        }

        @Override
        public Date read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return new Date(in.nextLong());
        }
    }
}
