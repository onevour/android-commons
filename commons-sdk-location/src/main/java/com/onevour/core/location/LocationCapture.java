package com.onevour.core.location;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.location.Priority;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Captures the device's position while the app says so -- only that. When to track (absen,
 * check-in, working hours) and what to do with a fix (store it, send it) stay in the app:
 * {@link Config#shouldTrack} and {@link Config#onLocation}.
 * <ul>
 *     <li>{@link LocationService}: a location foreground service asking a battery-saving fix
 *     every 10 to 20 minutes, only after moving 500 m;</li>
 *     <li>{@link FixFilter}: only real, accurate fixes, at most one per 9 minutes, reach the app;</li>
 *     <li>{@link LocationWatchdogWorker}: stops the service once the app no longer wants it.</li>
 * </ul>
 * Location "while using the app" is all it needs: the service is only started from a visible screen
 * through {@link #sync}, never from the background.
 * <p>
 * A transaction that must stamp where the user is (check-in, absen) does not wait for the tracking:
 * it asks one exact fix now with {@link #current}.
 * <p>
 * Module {@code commons-sdk-location}: its manifest brings the location permissions and the service, and
 * it brings play-services-location and WorkManager. The app only asks the permission at run time.
 */
public final class LocationCapture {

    private static final String TAG = LocationCapture.class.getSimpleName();

    /** Fixes less accurate than this are not delivered. */
    public static final double MAX_ACCURACY_METRES = 100.0;

    /** A battery-saving fix this often... */
    public static final long INTERVAL_MS = TimeUnit.MINUTES.toMillis(20);

    /** ...sooner (down to this) only when another app already got one... */
    public static final long MIN_INTERVAL_MS = TimeUnit.MINUTES.toMillis(10);

    /** ...and only once the device moved this far. */
    public static final float MIN_DISTANCE_METRES = 500f;

    /** A fix closer in time to the last delivered one than this is a duplicate. */
    public static final long MIN_GAP_MS = minGap(MIN_INTERVAL_MS);

    private static final String WATCHDOG_WORK = "location-watchdog";

    /** The library's own preferences: when the last fix was delivered, kept across app restarts. */
    private static final String PREFS = "onevour_location";

    private static final String LAST_FIX_TIME = "last_fix_time";

    private static Application application;

    private static Config config;

    private static FixFilter filter;

    private LocationCapture() {
    }

    /** What the app decides. */
    public static final class Config {

        private final BooleanSupplier shouldTrack;
        private final Consumer<LocationFix> onLocation;
        private final Consumer<Location> onFix;
        private final long intervalMs;
        private final long minIntervalMs;
        private final float minDistanceMetres;
        private final int priority;
        private final String notificationTitle;
        private final String notificationText;
        @DrawableRes
        private final int notificationIcon;

        private Config(Builder builder) {
            this.shouldTrack = builder.shouldTrack;
            this.onLocation = builder.onLocation;
            this.onFix = builder.onFix;
            this.intervalMs = builder.intervalMs;
            this.minIntervalMs = builder.minIntervalMs;
            this.minDistanceMetres = builder.minDistanceMetres;
            this.priority = builder.priority;
            this.notificationTitle = builder.notificationTitle;
            this.notificationText = builder.notificationText;
            this.notificationIcon = builder.notificationIcon;
        }

        long intervalMs() {
            return intervalMs;
        }

        long minIntervalMs() {
            return minIntervalMs;
        }

        float minDistanceMetres() {
            return minDistanceMetres;
        }

        int priority() {
            return priority;
        }

        String notificationTitle() {
            return notificationTitle;
        }

        String notificationText() {
            return notificationText;
        }

        @DrawableRes
        int notificationIcon() {
            return notificationIcon;
        }

        /**
         * @param shouldTrack whether the app wants the position now (asked at start, on each fix, by the watchdog)
         * @param onLocation  a fix good enough to keep (see {@link FixFilter}); the app stores / sends it
         */
        public static Builder builder(BooleanSupplier shouldTrack, Consumer<LocationFix> onLocation) {
            return new Builder(shouldTrack, onLocation);
        }

        public static final class Builder {

            private final BooleanSupplier shouldTrack;
            private final Consumer<LocationFix> onLocation;
            private Consumer<Location> onFix = location -> {
            };
            private long intervalMs = INTERVAL_MS;
            private long minIntervalMs = MIN_INTERVAL_MS;
            private float minDistanceMetres = MIN_DISTANCE_METRES;
            private int priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY;
            private String notificationTitle = "Tracking Lokasi Aktif";
            private String notificationText = "Sedang mengambil lokasi perangkat...";
            @DrawableRes
            private int notificationIcon = android.R.drawable.ic_menu_mylocation;

            private Builder(BooleanSupplier shouldTrack, Consumer<LocationFix> onLocation) {
                this.shouldTrack = Objects.requireNonNull(shouldTrack);
                this.onLocation = Objects.requireNonNull(onLocation);
            }

            /** Every fix, also those not delivered to onLocation (e.g. the app's last known position). */
            public Builder onFix(Consumer<Location> onFix) {
                this.onFix = Objects.requireNonNull(onFix);
                return this;
            }

            /**
             * How often to ask a fix, instead of {@link #INTERVAL_MS} / {@link #MIN_INTERVAL_MS} /
             * {@link #MIN_DISTANCE_METRES}, e.g. a short one to try the capture on a test route. Fixes
             * closer in time than 90% of minIntervalMs to the last delivered one are dropped.
             */
            public Builder interval(long intervalMs, long minIntervalMs, float minDistanceMetres) {
                if (minIntervalMs <= 0 || intervalMs < minIntervalMs || minDistanceMetres < 0) {
                    throw new IllegalArgumentException("need 0 < minIntervalMs <= intervalMs and minDistanceMetres >= 0");
                }
                this.intervalMs = intervalMs;
                this.minIntervalMs = minIntervalMs;
                this.minDistanceMetres = minDistanceMetres;
                return this;
            }

            /**
             * The fused provider's priority instead of {@link Priority#PRIORITY_BALANCED_POWER_ACCURACY}
             * (Wi-Fi / cell, no GPS). {@link Priority#PRIORITY_HIGH_ACCURACY} turns the GPS on for every
             * fix -- costly on a phone, but the only source an emulator route feeds.
             */
            public Builder priority(int priority) {
                if (priority != Priority.PRIORITY_HIGH_ACCURACY && priority != Priority.PRIORITY_BALANCED_POWER_ACCURACY
                        && priority != Priority.PRIORITY_LOW_POWER && priority != Priority.PRIORITY_PASSIVE) {
                    throw new IllegalArgumentException("not a com.google.android.gms.location.Priority: " + priority);
                }
                this.priority = priority;
                return this;
            }

            /** The ongoing notification while tracking. */
            public Builder notification(String title, String text, @DrawableRes int icon) {
                this.notificationTitle = Objects.requireNonNull(title);
                this.notificationText = Objects.requireNonNull(text);
                this.notificationIcon = icon;
                return this;
            }

            public Config build() {
                return new Config(this);
            }
        }
    }

    /** Once, from {@link Application#onCreate}. */
    public static void init(@NonNull Application app, @NonNull Config trackingConfig) {
        application = app;
        config = trackingConfig;
        SharedPreferences prefs = prefs(app);
        filter = new FixFilter(new FixFilter.LastFix() {
            @Override
            public long time() {
                return prefs.getLong(LAST_FIX_TIME, 0L);
            }

            @Override
            public void save(long time) {
                prefs.edit().putLong(LAST_FIX_TIME, time).apply();
            }
        }, minGap(trackingConfig.minIntervalMs));
    }

    /** Just under the shortest interval: the next real fix passes, the same fix delivered twice does not. */
    private static long minGap(long minIntervalMs) {
        return minIntervalMs / 10 * 9;
    }

    static Config config() {
        if (Objects.isNull(config)) throw new IllegalStateException("LocationCapture.init was not called");
        return config;
    }

    /** The app's decision; false before {@link #init}. */
    public static boolean shouldCaptureNow() {
        if (Objects.isNull(config)) return false;
        try {
            return config.shouldTrack.getAsBoolean();
        } catch (RuntimeException e) {
            Log.e(TAG, "the app could not decide whether to track " + e.getMessage());
            return false;
        }
    }

    /**
     * Schedules the watchdog that, every 15 minutes, stops the service once the app no longer wants
     * it, and runs it once now. It never starts the service.
     */
    public static void startWatchdog() {
        WorkManager workManager = WorkManager.getInstance(application);
        workManager.enqueue(new OneTimeWorkRequest.Builder(LocationWatchdogWorker.class).build());
        workManager.enqueueUniquePeriodicWork(WATCHDOG_WORK, ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(LocationWatchdogWorker.class, 15, TimeUnit.MINUTES).build());
    }

    /**
     * Cancels watchdogs an app scheduled under its own name before it used this library: their worker
     * class is gone, and WorkManager would keep failing to run them every 15 minutes.
     */
    public static void cancelOldWatchdogs(Context context, String... uniqueWorkNames) {
        WorkManager workManager = WorkManager.getInstance(context);
        for (String name : uniqueWorkNames) workManager.cancelUniqueWork(name);
    }

    /**
     * From a visible screen: starts the service when the app wants the position and location is
     * allowed, stops it when not. "While using the app" location is enough here.
     */
    public static void sync(Context context) {
        if (shouldCaptureNow() && hasForegroundLocationPermission(context)) {
            Intent intent = new Intent(context, LocationService.class);
            intent.setAction(LocationService.ACTION_START);
            try {
                ContextCompat.startForegroundService(context, intent);
            } catch (RuntimeException e) { // not allowed to start a foreground service right now
                Log.e(TAG, "cannot start tracking " + e.getMessage());
            }
            return;
        }
        context.stopService(new Intent(context, LocationService.class));
    }

    /**
     * From a visible screen: replaces the config (e.g. an interval / distance the user set) and
     * restarts a wanted capture with it -- the service reads the interval when it starts.
     */
    public static void reconfigure(@NonNull Context context, @NonNull Config newConfig) {
        init((Application) context.getApplicationContext(), newConfig);
        context.stopService(new Intent(context, LocationService.class));
        sync(context);
    }

    /** Stops the service and the watchdog, e.g. on logout. */
    public static void stop(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WATCHDOG_WORK);
        context.stopService(new Intent(context, LocationService.class));
        prefs(context).edit().remove(LAST_FIX_TIME).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Precise location granted; "while using the app" is all it needs. */
    public static boolean hasForegroundLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /** A fix of the service: handed to the app, and delivered as a location when good enough. */
    static void onServiceFix(Location location) {
        Config current = config();
        try {
            current.onFix.accept(location);
        } catch (RuntimeException e) {
            Log.e(TAG, "app could not take the fix " + e.getMessage());
        }
        LocationFix fix = filter.accept(location.getLatitude(), location.getLongitude(),
                location.hasAccuracy() ? location.getAccuracy() : null, location.getTime(), isMock(location));
        if (Objects.isNull(fix)) return;
        try {
            current.onLocation.accept(fix);
        } catch (RuntimeException e) {
            // the app's table or API failed: this fix is lost, the tracking (and the app) go on
            Log.e(TAG, "app could not take the location " + e.getMessage(), e);
        }
    }

    /** Why {@link #current} gave no fix; the app words the message. */
    public enum NoFix {
        /** Precise location not granted. */
        NO_PERMISSION,
        /** Location is turned off on the phone. */
        LOCATION_OFF,
        /** The fix came from a fake GPS app. */
        MOCK,
        /** No fix within the time given (no GPS signal, indoors...). */
        TIMEOUT
    }

    /** The result of {@link #current}, on the main thread, exactly once (never after {@link Pending#cancel}). */
    public interface CurrentListener {

        void onFix(@NonNull LocationFix fix);

        void onNoFix(@NonNull NoFix reason);
    }

    /** A {@link #current} request still running. */
    public interface Pending {

        /** E.g. when the screen closes: the GPS goes off and the listener is not called. */
        void cancel();
    }

    /**
     * One exact (GPS) fix now, for a transaction: a fix at most maxAgeMs old is reused, otherwise the
     * GPS is turned on for at most timeoutMs. Fake GPS is refused. Works without the tracking service
     * and without {@link #init}; needs precise location "while using the app".
     *
     * @param maxAgeMs  0 for a fix taken now
     * @param timeoutMs how long to wait for the GPS
     */
    public static Pending current(@NonNull Context context, long maxAgeMs, long timeoutMs, @NonNull CurrentListener listener) {
        return CurrentLocation.request(context, maxAgeMs, timeoutMs, listener);
    }

    /** Fix produced by a mock-location (fake GPS) app. */
    @SuppressWarnings("deprecation")
    static boolean isMock(Location location) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return location.isMock();
        return location.isFromMockProvider();
    }
}
