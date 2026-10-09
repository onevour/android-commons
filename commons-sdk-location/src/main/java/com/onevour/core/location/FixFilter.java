package com.onevour.core.location;

import android.util.Log;

/**
 * Which tracking fixes reach the app ({@link LocationCapture.Config} onLocation): real ones (no fake
 * GPS), accurate to {@link LocationCapture#MAX_ACCURACY_METRES}, not near 0,0, and at least
 * minGapMs ({@link LocationCapture#MIN_GAP_MS} by default) after the last one delivered -- the same fix delivered twice is
 * dropped.
 */
public class FixFilter {

    private static final String TAG = FixFilter.class.getSimpleName();

    /** When the last fix was delivered (kept across app restarts). */
    public interface LastFix {

        long time();

        void save(long time);
    }

    private final LastFix lastFix;

    private final long minGapMs;

    public FixFilter(LastFix lastFix, long minGapMs) {
        this.lastFix = lastFix;
        this.minGapMs = minGapMs;
    }

    /** @return the fix to deliver, or null when it is not good enough or too soon */
    public LocationFix accept(double latitude, double longitude, Float accuracy, long time, boolean mock) {
        if (mock) {
            Log.w(TAG, "mock location, dropped");
            return null;
        }
        if (accuracy == null || accuracy > LocationCapture.MAX_ACCURACY_METRES) {
            Log.w(TAG, "location accuracy too low, dropped");
            return null;
        }
        if (!Coordinates.isValid(latitude, longitude) || 0 == time) {
            Log.w(TAG, "invalid location (near 0,0) or without time, dropped");
            return null;
        }
        long last = lastFix.time();
        if (0 != last && time - last < minGapMs) {
            Log.d(TAG, "too soon after the last fix, dropped");
            return null;
        }
        lastFix.save(time);
        return new LocationFix(latitude, longitude, accuracy, time);
    }
}
