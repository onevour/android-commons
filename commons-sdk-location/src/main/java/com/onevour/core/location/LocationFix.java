package com.onevour.core.location;

import androidx.annotation.Nullable;

/** A fix good enough to deliver to the app (see {@link FixFilter}): where, how accurate, and when it was taken. */
public final class LocationFix {

    private final double latitude;

    private final double longitude;

    @Nullable
    private final Float accuracy;

    private final long time;

    public LocationFix(double latitude, double longitude, @Nullable Float accuracy, long time) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.time = time;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    /** Estimated horizontal accuracy in metres, or null when the fix had none. */
    @Nullable
    public Float getAccuracy() {
        return accuracy;
    }

    /** When the fix was taken, in milliseconds since the epoch. */
    public long getTime() {
        return time;
    }
}
