package com.onevour.core.utilities.commons;

import android.app.Activity;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Where RefSession keeps its values: a SQLite table (one row per key) behind an in-memory copy.
 * <ul>
 *     <li>Reads come from memory. The table is loaded once, in the background, when the app starts
 *     (ContextHelper.init); a read before that waits for it, usually a few milliseconds.</li>
 *     <li>A write changes memory right away (the next read sees it) and reaches the table on one
 *     background thread: several writes, or several writes of the same key, become one transaction.</li>
 *     <li>When the app goes to the background (no activity started) the pending writes are flushed,
 *     so Android killing the process later loses nothing.</li>
 *     <li>The first time, the values of the old SharedPreferences file "RefSession" are moved into the
 *     table and the file is emptied: users stay logged in after the update.</li>
 * </ul>
 * Values are Integer, Long, Float, Boolean or String, as SharedPreferences kept them.
 */
final class RefSessionStore {

    private static final String TAG = RefSessionStore.class.getSimpleName();

    static final String DATABASE = "ref_session.db";

    /** The SharedPreferences file RefSession used before. */
    static final String LEGACY_PREFERENCES = "RefSession";

    private static final String TABLE = "ref_session";

    private static final long LOAD_TIMEOUT_MS = 5_000;

    private static volatile RefSessionStore instance;

    private final Map<String, Object> values = new ConcurrentHashMap<>();

    /** Writes not in the table yet: key to value, or REMOVED. */
    private final Map<String, Object> pending = new HashMap<>();

    private static final Object REMOVED = new Object();

    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ref-session-writer");
        thread.setDaemon(true);
        return thread;
    });

    private final CountDownLatch loaded = new CountDownLatch(1);

    private final Helper helper;

    private boolean drainScheduled;

    private RefSessionStore(Context context, String databaseName) {
        helper = new Helper(context, databaseName);
    }

    /** From ContextHelper.init: starts loading the values in the background. */
    static void preload(Application application) {
        get(application);
    }

    static RefSessionStore get() {
        Application application = ContextHelper.getApplication();
        if (Objects.isNull(application)) throw new IllegalStateException("call ContextHelper.init(application) first");
        return get(application);
    }

    private static RefSessionStore get(Application application) {
        RefSessionStore store = instance;
        if (Objects.nonNull(store)) return store;
        synchronized (RefSessionStore.class) {
            if (Objects.isNull(instance)) {
                RefSessionStore created = new RefSessionStore(application, DATABASE);
                created.watchBackground(application);
                created.writer.execute(() -> created.load(application));
                instance = created;
            }
            return instance;
        }
    }

    /* ---------- reads: memory ---------- */

    @Nullable
    Object get(String key) {
        awaitLoaded();
        return values.get(key);
    }

    boolean contains(String key) {
        awaitLoaded();
        return values.containsKey(key);
    }

    Set<String> keys() {
        awaitLoaded();
        return Collections.unmodifiableSet(new HashSet<>(values.keySet()));
    }

    /* ---------- writes: memory now, table in the background ---------- */

    void put(Map<String, Object> changes) {
        awaitLoaded();
        for (Map.Entry<String, Object> change : changes.entrySet()) {
            requireSupported(change.getValue());
        }
        synchronized (pending) {
            for (Map.Entry<String, Object> change : changes.entrySet()) {
                values.put(change.getKey(), change.getValue());
                pending.put(change.getKey(), change.getValue());
            }
            scheduleDrain();
        }
    }

    void remove(Collection<String> keys) {
        awaitLoaded();
        synchronized (pending) {
            for (String key : keys) {
                values.remove(key);
                pending.put(key, REMOVED);
            }
            scheduleDrain();
        }
    }

    /** Waits until every write so far is in the table; true when they all made it. */
    boolean flush() {
        Future<Boolean> done;
        synchronized (pending) {
            done = writer.submit(this::drain);
            drainScheduled = true;
        }
        try {
            return done.get(LOAD_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Log.e(TAG, "cannot flush the writes " + e.getMessage());
            return false;
        }
    }

    private void scheduleDrain() {
        if (drainScheduled) return;
        drainScheduled = true;
        writer.execute(this::drain);
    }

    /** On the writer thread: every pending write in one transaction. */
    private boolean drain() {
        Map<String, Object> batch;
        synchronized (pending) {
            drainScheduled = false;
            if (pending.isEmpty()) return true;
            batch = new HashMap<>(pending);
            pending.clear();
        }
        SQLiteDatabase database;
        try {
            database = helper.getWritableDatabase();
        } catch (RuntimeException e) {
            Log.e(TAG, "cannot open " + DATABASE + ", writes kept for the next try: " + e.getMessage());
            requeue(batch);
            return false;
        }
        database.beginTransaction();
        try {
            for (Map.Entry<String, Object> entry : batch.entrySet()) {
                if (entry.getValue() == REMOVED) {
                    database.delete(TABLE, "key = ?", new String[]{entry.getKey()});
                } else {
                    database.insertWithOnConflict(TABLE, null, row(entry.getKey(), entry.getValue()), SQLiteDatabase.CONFLICT_REPLACE);
                }
            }
            database.setTransactionSuccessful();
            return true;
        } catch (SQLiteException e) {
            Log.e(TAG, "cannot write " + batch.size() + " values, kept for the next try: " + e.getMessage());
            requeue(batch);
            return false;
        } finally {
            database.endTransaction();
        }
    }

    /** A failed batch goes back to pending, unless a newer write of the key came meanwhile. */
    private void requeue(Map<String, Object> batch) {
        synchronized (pending) {
            for (Map.Entry<String, Object> entry : batch.entrySet()) {
                if (!pending.containsKey(entry.getKey())) pending.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /* ---------- loading and migration ---------- */

    private void load(Context context) {
        try {
            SQLiteDatabase database = helper.getWritableDatabase();
            try (Cursor cursor = database.query(TABLE, new String[]{"key", "type", "value"}, null, null, null, null, null)) {
                while (cursor.moveToNext()) {
                    Object value = decode(cursor.getString(1), cursor.getString(2));
                    if (Objects.nonNull(value)) values.put(cursor.getString(0), value);
                }
            }
            migrateLegacy(context, database);
        } catch (RuntimeException e) {                      // SQLiteException, or no database at all
            Log.e(TAG, "cannot load " + DATABASE + ": " + e.getMessage());
        } finally {
            loaded.countDown();
        }
    }

    /** Moves the old SharedPreferences values into the table (a value already in the table wins). */
    private void migrateLegacy(Context context, SQLiteDatabase database) {
        SharedPreferences legacy = context.getSharedPreferences(LEGACY_PREFERENCES, Context.MODE_PRIVATE);
        Map<String, ?> old = legacy.getAll();
        if (old.isEmpty()) return;
        database.beginTransaction();
        try {
            int moved = 0;
            for (Map.Entry<String, ?> entry : old.entrySet()) {
                Object value = entry.getValue();
                if (!isSupported(value) || values.containsKey(entry.getKey())) continue;
                database.insertWithOnConflict(TABLE, null, row(entry.getKey(), value), SQLiteDatabase.CONFLICT_IGNORE);
                values.put(entry.getKey(), value);
                moved++;
            }
            database.setTransactionSuccessful();
            Log.i(TAG, "moved " + moved + " values from SharedPreferences " + LEGACY_PREFERENCES);
        } finally {
            database.endTransaction();
        }
        legacy.edit().clear().commit();
    }

    private void awaitLoaded() {
        if (loaded.getCount() == 0) return;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.w(TAG, "RefSession read on the main thread before its values were loaded: waiting");
        }
        try {
            if (!loaded.await(LOAD_TIMEOUT_MS, TimeUnit.MILLISECONDS)) Log.e(TAG, "values not loaded in time");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /* ---------- flush when the app goes to the background ---------- */

    private void watchBackground(Application application) {
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            private int started;

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                started++;
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                started--;
                // no screen left: the process may be killed any time from now on
                if (started <= 0 && !activity.isChangingConfigurations()) flush();
            }

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle state) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle state) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });
    }

    /* ---------- encoding ---------- */

    private static ContentValues row(String key, Object value) {
        ContentValues row = new ContentValues(3);
        row.put("key", key);
        row.put("type", type(value));
        row.put("value", String.valueOf(value));
        return row;
    }

    private static String type(Object value) {
        if (value instanceof Integer) return "I";
        if (value instanceof Long) return "L";
        if (value instanceof Float) return "F";
        if (value instanceof Boolean) return "B";
        return "S";
    }

    @Nullable
    private static Object decode(String type, String text) {
        try {
            switch (type) {
                case "I":
                    return Integer.valueOf(text);
                case "L":
                    return Long.valueOf(text);
                case "F":
                    return Float.valueOf(text);
                case "B":
                    return Boolean.valueOf(text);
                default:
                    return text;
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "unreadable " + type + " value, skipped");
            return null;
        }
    }

    private static boolean isSupported(Object value) {
        return value instanceof String || value instanceof Integer || value instanceof Long
                || value instanceof Float || value instanceof Boolean;
    }

    private static void requireSupported(Object value) {
        if (!isSupported(value)) throw new IllegalArgumentException("RefSession stores String, Integer, Long, Float or Boolean, not " + value);
    }

    /* ---------- tests ---------- */

    /** Tests: as if the process restarted, values loaded again from the table. */
    static void reloadForTest() {
        synchronized (RefSessionStore.class) {
            RefSessionStore old = instance;
            if (Objects.nonNull(old)) {
                old.flush();
                old.writer.shutdown();
                old.helper.close();
            }
            instance = null;
        }
    }

    private static final class Helper extends SQLiteOpenHelper {

        Helper(Context context, String name) {
            super(context, name, null, 1);
            setWriteAheadLoggingEnabled(true);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            database.execSQL("CREATE TABLE " + TABLE + " (key TEXT PRIMARY KEY NOT NULL, type TEXT NOT NULL, value TEXT NOT NULL)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
            // version 1 only
        }
    }
}
