package com.onevour.core.utilities.commons;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RefSession {

    private final String TAG = RefSession.class.getSimpleName();

    /**
     * RefSession's own Gson, apart from GsonHelper: an app's GsonHelper.initialize(...) (its date
     * format for the server, ...) does not change how values are stored on the phone.
     * Only @Expose fields, as before; a Date written as epoch milliseconds and read from that or from
     * any text format apps stored before; strings kept as they are; NaN and Infinity allowed.
     */
    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .serializeSpecialFloatingPointValues()
            .registerTypeAdapter(Date.class, new DateAdapter())
            .create();

    protected final String EDITOR_DEFAULT = RefSession.class.getSimpleName();


    private SharedPreferences getSharedPreferences(String Key) {
        return ContextHelper.getApplication().getSharedPreferences(Key, Context.MODE_PRIVATE);
    }

    private SharedPreferences.Editor editor(String Key) {
        SharedPreferences sharedPreferences = ContextHelper.getApplication().getSharedPreferences(Key, Context.MODE_PRIVATE);
        return sharedPreferences.edit();
    }

    /**
     * get
     */
    public <T> T find(Class<T> cls) {
        String key = cls.getSimpleName().toUpperCase();
        return find(key, cls);
    }

    public <T> T find(String key, Class<T> cls) {
        String valueString = getSharedPreferences(EDITOR_DEFAULT).getString(key.toUpperCase(), null);
        if (null == valueString) return null;
        return GSON.fromJson(valueString, cls);
    }

    public <T> List<T> findCollection(String key, Class<T> cls) {
        String valueString = getSharedPreferences(EDITOR_DEFAULT).getString(key.toUpperCase(), null);
        if (null == valueString) return null;
        String value = findString(key);
        if (ValueOf.isEmpty(value)) return null;
        return GSON.fromJson(valueString, TypeToken.getParameterized(ArrayList.class, cls).getType());
    }


    /** Whether a value is stored under this key (0 / false / "" included). */
    public boolean contains(String key) {
        return getSharedPreferences(EDITOR_DEFAULT).contains(key.toUpperCase());
    }

    /**
     * The numbers below never throw: a value stored as another type is converted when that is exact
     * (5L read as int, "5" read as long), otherwise the fallback comes back and a warning is logged.
     */
    public int findInt(String key) {
        return findInt(key, 0);
    }

    public int findInt(String key, int fallback) {
        BigDecimal number = number(key);
        if (Objects.isNull(number)) return fallback;
        try {
            return number.intValueExact();
        } catch (ArithmeticException e) {
            Log.w(TAG, "not an int: ".concat(key));
            return fallback;
        }
    }

    public float findFloat(String key) {
        return findFloat(key, 0f);
    }

    public float findFloat(String key, float fallback) {
        BigDecimal number = number(key);
        return Objects.isNull(number) ? fallback : number.floatValue();
    }

    public long findLong(String key) {
        return findLong(key, 0L);
    }

    public long findLong(String key, long fallback) {
        BigDecimal number = number(key);
        if (Objects.isNull(number)) return fallback;
        try {
            return number.longValueExact();
        } catch (ArithmeticException e) {
            Log.w(TAG, "not a long: ".concat(key));
            return fallback;
        }
    }

    public double findDouble(String key) {
        return findDouble(key, 0d);
    }

    public double findDouble(String key, double fallback) {
        Object value = raw(key);
        if (Objects.isNull(value)) return fallback;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(unquote(value.toString()));     // saveDouble keeps text, NaN included
        } catch (NumberFormatException e) {
            Log.w(TAG, "not a double: ".concat(key));
            return fallback;
        }
    }

    public boolean findBoolean(String key) {
        return findBoolean(key, false);
    }

    public boolean findBoolean(String key, boolean fallback) {
        Object value = raw(key);
        if (value instanceof Boolean) return (Boolean) value;
        if (Objects.nonNull(value)) {
            String text = unquote(value.toString());
            if ("true".equalsIgnoreCase(text)) return true;
            if ("false".equalsIgnoreCase(text)) return false;
            Log.w(TAG, "not a boolean: ".concat(key));
        }
        return fallback;
    }

    /** The text of the value (a number as "5"), or null when missing. */
    public String findString(String key) {
        Object value = raw(key);
        return Objects.isNull(value) ? null : value.toString();
    }

    /** The stored value as it is (Integer, Long, Float, Boolean or String), or null when missing. */
    private Object raw(String key) {
        SharedPreferences preferences = getSharedPreferences(EDITOR_DEFAULT);
        String upperKey = key.toUpperCase();
        try {
            return preferences.getString(upperKey, null);             // most values are text
        } catch (ClassCastException e) {
            return preferences.getAll().get(upperKey);
        }
    }

    private BigDecimal number(String key) {
        Object value = raw(key);
        if (Objects.isNull(value)) return null;
        if (value instanceof Integer || value instanceof Long) return BigDecimal.valueOf(((Number) value).longValue());
        if (value instanceof Float) return new BigDecimal(value.toString());
        try {
            return new BigDecimal(unquote(value.toString()).trim());
        } catch (NumberFormatException e) {
            Log.w(TAG, "not a number: ".concat(key));
            return null;
        }
    }

    /** save(key, "5") keeps JSON text, quotes included. */
    private static String unquote(String text) {
        if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) return text.substring(1, text.length() - 1);
        return text;
    }

    /**
     * create or replace multiple
     */
    public void save(Object... values) {
        for (Object value : values) {
            String key = value.getClass().getSimpleName().toUpperCase();
            save(key, value, false);
        }
    }

    /**
     * create or replace single
     */
    public void save(Object value) {
        save(value.getClass().getSimpleName().toUpperCase(), value, false);
    }

    public void saveString(String key, String value) {
        save(key, value, true);
    }

    public void saveInt(String key, Integer value) {
        save(key, value, true);
    }

    public void saveLong(String key, Long value) {
        save(key, value, true);
    }

    public void saveFloat(String key, Float value) {
        save(key, value, true);
    }

    public void saveDouble(String key, Double value) {
        save(key, value, true);
    }

    public void saveBoolean(String key, Boolean value) {
        save(key, value, true);
    }

    public void save(String key, Object value) {
        save(key, value, false);
    }

    public void saveCollection(String key, List<?> value) {
        String valueString = GSON.toJson(value);
        save(key, valueString, true);
    }

    public void save(String keyRef, Object value, boolean isNative) {
        if (ValueOf.isEmpty(keyRef) || ValueOf.isNull(value)) {
            throw new NullPointerException("cannot store null object, key and empty key");
        }
        String key = keyRef.trim().toUpperCase();
        SharedPreferences.Editor editor = editor(EDITOR_DEFAULT);
        if (isNative) {
            if (value instanceof Integer) {
                editor.putInt(key, (int) value);
            } else if (value instanceof Float) {
                editor.putFloat(key, (float) value);
            } else if (value instanceof Long) {
                editor.putLong(key, (long) value);
            } else if (value instanceof Double) {
                editor.putString(key, String.valueOf(value));
            } else if (value instanceof Boolean) {
                editor.putBoolean(key, (boolean) value);
            } else if (value instanceof String) {
                editor.putString(key, (String) value);
            } else
                throw new IllegalArgumentException("Only int, float, boolean, string acceptable when native set true");
        } else {
            String valueString = GSON.toJson(value);
            editor.putString(key, valueString);
        }
        if (!editor.commit()) {
            throw new IllegalStateException("cannot commit save keys ".concat(key));
        }
    }

    /*
     * more types: money, time, sets, generic types, enums, bytes, ids. Each stored under its key
     * like the ones above; a missing key, or a value stored as another type, gives null (or the default)
     */

    /** Money and other exact numbers, kept as text so no precision is lost (unlike double). */
    public void saveDecimal(String key, BigDecimal value) {
        save(key, requireValue(value).toPlainString(), true);
    }

    public BigDecimal findDecimal(String key) {
        String value = findText(key);
        if (Objects.isNull(value)) return null;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            Log.w(TAG, "not a decimal: ".concat(key));
            return null;
        }
    }

    /** A point in time, kept as epoch milliseconds: no text format, no locale. */
    public void saveDate(String key, Date value) {
        save(key, requireValue(value).getTime(), true);
    }

    public Date findDate(String key) {
        SharedPreferences preferences = getSharedPreferences(EDITOR_DEFAULT);
        String upperKey = key.toUpperCase();
        if (!preferences.contains(upperKey)) return null;
        try {
            return new Date(preferences.getLong(upperKey, 0L));
        } catch (ClassCastException e) {
            Log.w(TAG, "not a date: ".concat(key));
            return null;
        }
    }

    /** A set of strings, in the order they were added. */
    public void saveSet(String key, Set<String> value) {
        save(key, GSON.toJson(new ArrayList<>(requireValue(value))), true);
    }

    public Set<String> findSet(String key) {
        return toSet(key, findText(key));
    }

    /**
     * Any generic type, e.g. Map&lt;String, Product&gt; or List&lt;List&lt;Item&gt;&gt;, saved with
     * {@link #save(String, Object)}: {@code find("PRICES", new TypeToken<Map<String, Price>>(){}.getType())}.
     * Model fields need @Expose, like every object here.
     */
    public <T> T find(String key, Type type) {
        String value = findText(key);
        if (Objects.isNull(value)) return null;
        try {
            return GSON.fromJson(value, type);
        } catch (JsonParseException e) {
            Log.w(TAG, "cannot read ".concat(key).concat(" as ").concat(type.toString()));
            return null;
        }
    }

    /** An enum, kept by name. */
    public <E extends Enum<E>> void saveEnum(String key, E value) {
        save(key, requireValue(value).name(), true);
    }

    /** The stored constant, or fallback when the key is missing or the name is no longer in the enum. */
    public <E extends Enum<E>> E findEnum(String key, Class<E> type, E fallback) {
        return toEnum(type, findText(key), fallback);
    }

    /** Small binary values (a hash, a key); keep files on disk and save their path instead. */
    public void saveBytes(String key, byte[] value) {
        save(key, Base64.encodeToString(requireValue(value), Base64.NO_WRAP), true);
    }

    public byte[] findBytes(String key) {
        return toBytes(key, findText(key));
    }

    public void saveUuid(String key, UUID value) {
        save(key, requireValue(value).toString(), true);
    }

    public UUID findUuid(String key) {
        return toUuid(key, findText(key));
    }

    private Set<String> toSet(String key, String value) {
        if (Objects.isNull(value)) return null;
        try {
            List<String> items = GSON.fromJson(value, new TypeToken<List<String>>() {
            }.getType());
            return Objects.isNull(items) ? null : new LinkedHashSet<>(items);
        } catch (JsonParseException e) {
            Log.w(TAG, "not a set: ".concat(key));
            return null;
        }
    }

    private <E extends Enum<E>> E toEnum(Class<E> type, String value, E fallback) {
        if (Objects.isNull(value)) return fallback;
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "unknown ".concat(type.getSimpleName()).concat(" ").concat(value));
            return fallback;
        }
    }

    private byte[] toBytes(String key, String value) {
        if (Objects.isNull(value)) return null;
        try {
            return Base64.decode(value, Base64.NO_WRAP);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "not bytes: ".concat(key));
            return null;
        }
    }

    private UUID toUuid(String key, String value) {
        if (Objects.isNull(value)) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "not a uuid: ".concat(key));
            return null;
        }
    }

    /** A value saved as text, or null when missing or saved as another type. */
    private String findText(String key) {
        try {
            return getSharedPreferences(EDITOR_DEFAULT).getString(key.toUpperCase(), null);
        } catch (ClassCastException e) {
            return null;
        }
    }

    private static <T> T requireValue(T value) {
        if (Objects.isNull(value)) {
            throw new NullPointerException("cannot store null object, delete the key instead");
        }
        return value;
    }

    /*
     * Secrets (tokens, PINs, personal data): the same types with a "Secure" suffix. Each value is
     * encrypted with an AES-GCM key kept in the Android Keystore (RefSessionCipher) and stored in this
     * same file under its key + "_SECURE", so the clear text is never on disk.
     * - A value that cannot be decrypted (key lost after a reset, a backup restored on another phone)
     *   reads as missing and is removed: handle it as "not logged in", never as a crash.
     * - Decrypted values are kept in memory for the process, so a read decrypts once.
     */

    /** Stored keys of secrets end with this. */
    public static final String SECURE_SUFFIX = "_SECURE";

    /** Decrypted secrets of this process, by stored key: "type|text". */
    private static final Map<String, String> SECURE_CACHE = new ConcurrentHashMap<>();

    public void saveStringSecure(String key, String value) {
        putSecure(key, 'S', requireValue(value));
    }

    public void saveIntSecure(String key, Integer value) {
        putSecure(key, 'I', String.valueOf(requireValue(value)));
    }

    public void saveLongSecure(String key, Long value) {
        putSecure(key, 'L', String.valueOf(requireValue(value)));
    }

    public void saveFloatSecure(String key, Float value) {
        putSecure(key, 'F', String.valueOf(requireValue(value)));
    }

    public void saveDoubleSecure(String key, Double value) {
        putSecure(key, 'D', String.valueOf(requireValue(value)));
    }

    public void saveBooleanSecure(String key, Boolean value) {
        putSecure(key, 'B', String.valueOf(requireValue(value)));
    }

    /** An object or a generic value (Map...), as JSON: model fields need @Expose. */
    public void saveSecure(String key, Object value) {
        putSecure(key, 'J', GSON.toJson(requireValue(value)));
    }

    public void saveCollectionSecure(String key, List<?> value) {
        putSecure(key, 'J', GSON.toJson(requireValue(value)));
    }

    public void saveDecimalSecure(String key, BigDecimal value) {
        putSecure(key, 'N', requireValue(value).toPlainString());
    }

    public void saveDateSecure(String key, Date value) {
        putSecure(key, 'T', String.valueOf(requireValue(value).getTime()));
    }

    public void saveSetSecure(String key, Set<String> value) {
        putSecure(key, 'G', GSON.toJson(new ArrayList<>(requireValue(value))));
    }

    public <E extends Enum<E>> void saveEnumSecure(String key, E value) {
        putSecure(key, 'E', requireValue(value).name());
    }

    public void saveBytesSecure(String key, byte[] value) {
        putSecure(key, 'Y', Base64.encodeToString(requireValue(value), Base64.NO_WRAP));
    }

    public void saveUuidSecure(String key, UUID value) {
        putSecure(key, 'U', requireValue(value).toString());
    }

    public boolean containsSecure(String key) {
        return Objects.nonNull(secureText(key));
    }

    /** The text of any secret (a number as "5"), or null. */
    public String findStringSecure(String key) {
        return secureText(key);
    }

    public int findIntSecure(String key) {
        return findIntSecure(key, 0);
    }

    public int findIntSecure(String key, int fallback) {
        BigDecimal number = secureNumber(key);
        if (Objects.isNull(number)) return fallback;
        try {
            return number.intValueExact();
        } catch (ArithmeticException e) {
            Log.w(TAG, "not an int: ".concat(key));
            return fallback;
        }
    }

    public long findLongSecure(String key) {
        return findLongSecure(key, 0L);
    }

    public long findLongSecure(String key, long fallback) {
        BigDecimal number = secureNumber(key);
        if (Objects.isNull(number)) return fallback;
        try {
            return number.longValueExact();
        } catch (ArithmeticException e) {
            Log.w(TAG, "not a long: ".concat(key));
            return fallback;
        }
    }

    public float findFloatSecure(String key) {
        return findFloatSecure(key, 0f);
    }

    public float findFloatSecure(String key, float fallback) {
        BigDecimal number = secureNumber(key);
        return Objects.isNull(number) ? fallback : number.floatValue();
    }

    public double findDoubleSecure(String key) {
        return findDoubleSecure(key, 0d);
    }

    public double findDoubleSecure(String key, double fallback) {
        String value = secureText(key);
        if (Objects.isNull(value)) return fallback;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            Log.w(TAG, "not a double: ".concat(key));
            return fallback;
        }
    }

    public boolean findBooleanSecure(String key) {
        return findBooleanSecure(key, false);
    }

    public boolean findBooleanSecure(String key, boolean fallback) {
        String value = secureText(key);
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        return fallback;
    }

    public <T> T findSecure(String key, Class<T> type) {
        return findSecure(key, (Type) type);
    }

    public <T> T findSecure(String key, Type type) {
        String value = secureText(key);
        if (Objects.isNull(value)) return null;
        try {
            return GSON.fromJson(value, type);
        } catch (JsonParseException e) {
            Log.w(TAG, "cannot read ".concat(key).concat(" as ").concat(type.toString()));
            return null;
        }
    }

    public <T> List<T> findCollectionSecure(String key, Class<T> type) {
        return findSecure(key, TypeToken.getParameterized(ArrayList.class, type).getType());
    }

    public BigDecimal findDecimalSecure(String key) {
        return secureNumber(key);
    }

    public Date findDateSecure(String key) {
        BigDecimal millis = secureNumber(key);
        if (Objects.isNull(millis)) return null;
        try {
            return new Date(millis.longValueExact());
        } catch (ArithmeticException e) {
            Log.w(TAG, "not a date: ".concat(key));
            return null;
        }
    }

    public Set<String> findSetSecure(String key) {
        return toSet(key, secureText(key));
    }

    public <E extends Enum<E>> E findEnumSecure(String key, Class<E> type, E fallback) {
        return toEnum(type, secureText(key), fallback);
    }

    public byte[] findBytesSecure(String key) {
        return toBytes(key, secureText(key));
    }

    public UUID findUuidSecure(String key) {
        return toUuid(key, secureText(key));
    }

    public void deleteSecure(String... keys) {
        String[] storedKeys = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            if (Objects.isNull(keys[i]) || keys[i].trim().isEmpty()) throw new NullPointerException("cannot remove empty key");
            storedKeys[i] = secureKey(keys[i]);
            SECURE_CACHE.remove(storedKeys[i]);
        }
        delete(storedKeys);
    }

    /** Every secret, e.g. on logout; the other values stay. */
    public void clearSecure() {
        SECURE_CACHE.clear();
        SharedPreferences preferences = getSharedPreferences(EDITOR_DEFAULT);
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : preferences.getAll().keySet()) {
            if (key.endsWith(SECURE_SUFFIX)) editor.remove(key);
        }
        if (!editor.commit()) throw new IllegalStateException("cannot clear secure values");
    }

    private static String secureKey(String key) {
        return key.trim().toUpperCase().concat(SECURE_SUFFIX);
    }

    private void putSecure(String key, char type, String text) {
        if (ValueOf.isEmpty(key)) throw new NullPointerException("cannot store an empty key");
        String storedKey = secureKey(key);
        String plain = type + "|" + text;
        String sealed;
        try {
            sealed = RefSessionCipher.encrypt(plain);
        } catch (GeneralSecurityException e) {
            // the secret is not stored: say so rather than keep it in clear text
            throw new IllegalStateException("cannot encrypt ".concat(storedKey), e);
        }
        save(storedKey, sealed, true);
        SECURE_CACHE.put(storedKey, plain);
    }

    /** The decrypted text of a secret, or null when missing or unreadable (then it is removed). */
    private String secureText(String key) {
        if (ValueOf.isEmpty(key)) return null;
        String storedKey = secureKey(key);
        String plain = SECURE_CACHE.get(storedKey);
        if (Objects.isNull(plain)) {
            String sealed = findText(storedKey);
            if (Objects.isNull(sealed)) return null;
            try {
                plain = RefSessionCipher.decrypt(sealed);
            } catch (GeneralSecurityException | IllegalArgumentException e) {
                Log.w(TAG, "cannot decrypt ".concat(storedKey).concat(", removed: ").concat(String.valueOf(e.getMessage())));
                getSharedPreferences(EDITOR_DEFAULT).edit().remove(storedKey).apply();
                return null;
            }
            SECURE_CACHE.put(storedKey, plain);
        }
        int separator = plain.indexOf('|');
        return separator < 0 ? null : plain.substring(separator + 1);
    }

    private BigDecimal secureNumber(String key) {
        String value = secureText(key);
        if (Objects.isNull(value)) return null;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            Log.w(TAG, "not a number: ".concat(key));
            return null;
        }
    }

    /** Tests: forget what was decrypted, as a new process would. */
    static void clearSecureMemory() {
        SECURE_CACHE.clear();
    }

    public <T> void delete(Class<T> cls) {
        String key = cls.getSimpleName().toUpperCase();
        delete(key);
    }

    public void delete(Object... objs) {
        for (Object o : objs) {
            delete(o.getClass().getSimpleName().toUpperCase());
        }
    }

    public void delete(String... keys) {
        SharedPreferences.Editor editor = editor(EDITOR_DEFAULT);
        StringBuilder sb = new StringBuilder("|");
        for (String key : keys) {
            if (null == key || key.trim().isEmpty()) {
                throw new NullPointerException("cannot remove empty key");
            }
            sb.append(key).append("|");
            editor.remove(key.toUpperCase());
            Log.d(TAG, "remove ".concat(key));
        }
        if (!editor.commit()) {
            throw new IllegalStateException("cannot commit remove keys ".concat(sb.toString()));
        }
    }


    /** Dates of RefSession: written as epoch milliseconds, read from any format apps stored before. */
    static final class DateAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {

        /** Text formats earlier versions of the apps stored, tried in this order. */
        private static final String[] PATTERNS = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",     // CSA: 2026-10-10T08:30:00.000+0700
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",     // fuguh: 2026-10-10T08:30:00.000+07, ...Z
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",   // ISO with +07:00
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd",
                "MMM d, yyyy h:mm:ss a",          // Gson's default (en-US)
                "MMM d, yyyy, h:mm:ss a",
        };

        @Override
        public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
            return Objects.isNull(src) ? JsonNull.INSTANCE : new JsonPrimitive(src.getTime());
        }

        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            if (Objects.isNull(json) || json.isJsonNull()) return null;
            if (!json.isJsonPrimitive()) throw new JsonParseException("not a date: " + json);
            JsonPrimitive primitive = json.getAsJsonPrimitive();
            // epoch milliseconds: what this adapter writes, and SDS's GsonUnixTimestampAdapter before it
            if (primitive.isNumber()) return new Date(primitive.getAsNumber().longValue());
            String text = primitive.getAsString().trim();
            if (text.isEmpty()) return null;
            if (text.matches("-?\\d+")) return new Date(Long.parseLong(text));
            Date date = parse(text);
            if (Objects.isNull(date)) throw new JsonParseException("unknown date format: " + text);
            return date;
        }

        static Date parse(String text) {
            // newer ICU puts a narrow no-break space before AM / PM
            String normalised = text.replace('\u202F', ' ').replace('\u00A0', ' ');
            for (String pattern : PATTERNS) {
                Date date = parseExactly(new SimpleDateFormat(pattern, Locale.US), normalised);
                if (Objects.nonNull(date)) return date;
            }
            return parseExactly(DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.US), normalised);
        }

        /** The whole text must match, not just its beginning ("2026-10-10" must not pass for a full time). */
        private static Date parseExactly(DateFormat format, String text) {
            format.setLenient(false);
            ParsePosition position = new ParsePosition(0);
            Date date = format.parse(text, position);
            return Objects.nonNull(date) && position.getIndex() == text.length() ? date : null;
        }
    }
}
