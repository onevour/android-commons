package com.onevour.core.location;

import java.util.Objects;

/**
 * The one rule for "is this a real location": anything within {@link #INVALID_RADIUS_KM} of
 * latitude/longitude 0,0 is treated as unknown. That covers missing values stored as 0.0, the
 * 1.0/1.0 placeholders (about 157 km from 0,0) and similar bogus fixes. Real stores, salesmen and
 * drivers are thousands of kilometres away.
 */
public final class Coordinates {

    public static final double INVALID_RADIUS_KM = 300.0;

    private static final double EARTH_RADIUS_KM = 6371.0;

    private Coordinates() {
    }

    public static boolean isValid(Double latitude, Double longitude) {
        if (Objects.isNull(latitude) || Objects.isNull(longitude)) return false;
        return distanceFromOriginKm(latitude, longitude) > INVALID_RADIUS_KM;
    }

    /** Great-circle (haversine) distance from latitude/longitude 0,0. */
    static double distanceFromOriginKm(double latitude, double longitude) {
        double lat = Math.toRadians(latitude);
        double lon = Math.toRadians(longitude);
        double a = Math.pow(Math.sin(lat / 2), 2) + Math.cos(lat) * Math.pow(Math.sin(lon / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }
}
