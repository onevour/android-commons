package com.onevour.sdk.impl.modules.location;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.location.Priority;
import com.onevour.core.location.LocationCapture;
import com.onevour.core.location.LocationFix;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * The sample's side of {@link LocationCapture}, what an app gives it: whether the position is wanted
 * now (a switch on the screen, kept across restarts) and what to do with a location (here: list it).
 * Every fix the service got is kept too, to see which ones the library delivered and which it dropped.
 * The interval, distance and GPS are the user's (settings screen), to try other values or an emulator route.
 */
public final class LocationSample {

    /** One fix of the service, and whether it reached the app. */
    public static final class Row {

        public final Location location;

        public boolean delivered;

        Row(Location location) {
            this.location = location;
        }
    }

    public interface Listener {

        void onChanged();
    }

    private static final String PREFS = "location_sample";

    private static final String WANTED = "wanted";

    private static final String INTERVAL_MINUTES = "interval_minutes";

    private static final String MIN_INTERVAL_MINUTES = "min_interval_minutes";

    private static final String MIN_DISTANCE_METRES = "min_distance_metres";

    private static final String GPS = "gps";

    /** What the user set on the settings screen; the library's defaults until then. */
    public static final class Settings {

        public final long intervalMinutes;

        public final long minIntervalMinutes;

        public final float minDistanceMetres;

        /** GPS for every fix (an emulator route only feeds the GPS) instead of Wi-Fi / cell. */
        public final boolean gps;

        public Settings(long intervalMinutes, long minIntervalMinutes, float minDistanceMetres, boolean gps) {
            this.intervalMinutes = intervalMinutes;
            this.minIntervalMinutes = minIntervalMinutes;
            this.minDistanceMetres = minDistanceMetres;
            this.gps = gps;
        }

        public static Settings defaults() {
            return new Settings(TimeUnit.MILLISECONDS.toMinutes(LocationCapture.INTERVAL_MS),
                    TimeUnit.MILLISECONDS.toMinutes(LocationCapture.MIN_INTERVAL_MS),
                    LocationCapture.MIN_DISTANCE_METRES, false);
        }

        /** E.g. "20 menit (min. 10 menit), 500 m, hemat baterai". */
        public String describe() {
            return String.format(Locale.getDefault(), "%d menit (min. %d menit), %.0f m, %s",
                    intervalMinutes, minIntervalMinutes, minDistanceMetres, gps ? "GPS" : "hemat baterai");
        }
    }

    private static final int MAX_ROWS = 200;

    private static final List<Row> ROWS = new ArrayList<>();

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static Context context;

    private static Listener listener;

    private LocationSample() {
    }

    /** From Application#onCreate. */
    public static LocationCapture.Config config(Context appContext) {
        context = appContext.getApplicationContext();
        LocationCapture.Config.Builder builder = LocationCapture.Config.builder(LocationSample::isWanted, LocationSample::onLocation)
                .onFix(LocationSample::onFix)
                .notification("Sample: lokasi aktif", "Mengambil lokasi perangkat untuk sample", android.R.drawable.ic_menu_mylocation);
        Settings settings = settings();
        builder.interval(TimeUnit.MINUTES.toMillis(settings.intervalMinutes), TimeUnit.MINUTES.toMillis(settings.minIntervalMinutes), settings.minDistanceMetres)
                .priority(settings.gps ? Priority.PRIORITY_HIGH_ACCURACY : Priority.PRIORITY_BALANCED_POWER_ACCURACY);
        return builder.build();
    }

    public static boolean isWanted() {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(WANTED, false);
    }

    public static void setWanted(boolean wanted) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(WANTED, wanted).apply();
    }

    public static Settings settings() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Settings defaults = Settings.defaults();
        return new Settings(prefs.getLong(INTERVAL_MINUTES, defaults.intervalMinutes),
                prefs.getLong(MIN_INTERVAL_MINUTES, defaults.minIntervalMinutes),
                prefs.getFloat(MIN_DISTANCE_METRES, defaults.minDistanceMetres),
                prefs.getBoolean(GPS, defaults.gps));
    }

    /** Kept, and applied to the capture right away (restarted when running). From a visible screen. */
    public static void apply(Context screen, Settings settings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putLong(INTERVAL_MINUTES, settings.intervalMinutes)
                .putLong(MIN_INTERVAL_MINUTES, settings.minIntervalMinutes)
                .putFloat(MIN_DISTANCE_METRES, settings.minDistanceMetres)
                .putBoolean(GPS, settings.gps)
                .apply();
        LocationCapture.reconfigure(screen, config(screen));
    }

    public static void setListener(Listener value) {
        listener = value;
    }

    /** Newest first. */
    public static synchronized List<Row> rows() {
        return new ArrayList<>(ROWS);
    }

    public static synchronized void clear() {
        ROWS.clear();
        notifyChanged();
    }

    /** Every fix of the service (LocationCapture.Config onFix). */
    private static synchronized void onFix(Location location) {
        ROWS.add(0, new Row(location));
        while (ROWS.size() > MAX_ROWS) ROWS.remove(ROWS.size() - 1);
        notifyChanged();
    }

    /** A fix the library let through (LocationCapture.Config onLocation): an app would store / send it here. */
    private static synchronized void onLocation(LocationFix fix) {
        LocationHistory.add(context, LocationHistory.Type.TRACKING, fix);
        for (Row row : ROWS) {
            if (row.location.getTime() == fix.getTime()) {
                row.delivered = true;
                break;
            }
        }
        notifyChanged();
    }

    private static void notifyChanged() {
        MAIN.post(() -> {
            if (Objects.nonNull(listener)) listener.onChanged();
        });
    }
}
