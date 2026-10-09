package com.onevour.sdk.impl.modules.location;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.onevour.core.location.LocationFix;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The sample's tracking history, kept across restarts in a JSON file: every location the library
 * delivered, and the positions the transactions stamped. A real app stores / sends these its own way.
 */
public final class LocationHistory {

    private static final String TAG = LocationHistory.class.getSimpleName();

    /** What put the point in the history. */
    public enum Type {
        TRACKING("Tracking"),
        ABSEN_MASUK("Absen masuk"),
        ABSEN_PULANG("Absen pulang"),
        CHECK_IN("Check-in"),
        REGISTER("Register customer");

        public final String label;

        Type(String label) {
            this.label = label;
        }
    }

    public static final class Entry {

        public Type type;

        public double latitude;

        public double longitude;

        public Float accuracy;

        public long time;

        /** From OpenStreetMap, once asked; null before. */
        public String address;
    }

    private static final String FILE = "location_history.json";

    private static final int MAX_ENTRIES = 2000;

    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor();

    private static final Gson GSON = new Gson();

    private static List<Entry> entries;

    private LocationHistory() {
    }

    public static synchronized void add(Context context, Type type, LocationFix fix) {
        Entry entry = new Entry();
        entry.type = type;
        entry.latitude = fix.getLatitude();
        entry.longitude = fix.getLongitude();
        entry.accuracy = fix.getAccuracy();
        entry.time = fix.getTime();
        List<Entry> list = load(context);
        list.add(entry);
        while (list.size() > MAX_ENTRIES) list.remove(0);
        save(context, new ArrayList<>(list));
    }

    /** Oldest first. */
    public static synchronized List<Entry> all(Context context) {
        return new ArrayList<>(load(context));
    }

    /** Keeps the address of an entry of {@link #all}. */
    public static synchronized void setAddress(Context context, Entry entry, String address) {
        entry.address = address;
        save(context, new ArrayList<>(load(context)));
    }

    public static synchronized void clear(Context context) {
        load(context).clear();
        save(context, new ArrayList<>());
    }

    private static List<Entry> load(Context context) {
        if (Objects.nonNull(entries)) return entries;
        entries = new ArrayList<>();
        File file = new File(context.getFilesDir(), FILE);
        if (!file.exists()) return entries;
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            List<Entry> stored = GSON.fromJson(reader, new TypeToken<List<Entry>>() {
            }.getType());
            if (Objects.nonNull(stored)) entries.addAll(stored);
        } catch (IOException | RuntimeException e) {
            Log.e(TAG, "cannot read the history " + e.getMessage());
        }
        return entries;
    }

    private static void save(Context context, List<Entry> snapshot) {
        File file = new File(context.getApplicationContext().getFilesDir(), FILE);
        WRITER.execute(() -> {
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(snapshot, writer);
            } catch (IOException e) {
                Log.e(TAG, "cannot write the history " + e.getMessage());
            }
        });
    }
}
