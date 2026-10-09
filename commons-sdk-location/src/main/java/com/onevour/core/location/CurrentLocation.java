package com.onevour.core.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import androidx.core.location.LocationManagerCompat;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.Objects;

/**
 * One exact fix now, for a transaction (check-in, absen...): GPS on just for this request, then off
 * again. Independent of the tracking service. See {@link LocationCapture#current}.
 */
final class CurrentLocation implements LocationCapture.Pending {

    private static final String TAG = CurrentLocation.class.getSimpleName();

    private final CancellationTokenSource cancellation = new CancellationTokenSource();

    private boolean done;

    private CurrentLocation() {
    }

    static LocationCapture.Pending request(Context context, long maxAgeMs, long timeoutMs, LocationCapture.CurrentListener listener) {
        CurrentLocation pending = new CurrentLocation();
        pending.start(context.getApplicationContext(), maxAgeMs, timeoutMs, listener);
        return pending;
    }

    private void start(Context context, long maxAgeMs, long timeoutMs, LocationCapture.CurrentListener listener) {
        if (!LocationCapture.hasForegroundLocationPermission(context)) {
            finish(listener, null, LocationCapture.NoFix.NO_PERMISSION);
            return;
        }
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (Objects.isNull(manager) || !LocationManagerCompat.isLocationEnabled(manager)) {
            finish(listener, null, LocationCapture.NoFix.LOCATION_OFF);
            return;
        }
        CurrentLocationRequest request = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(maxAgeMs)
                .setDurationMillis(timeoutMs)
                .build();
        try {
            LocationServices.getFusedLocationProviderClient(context)
                    .getCurrentLocation(request, cancellation.getToken())
                    .addOnCompleteListener(task -> {
                        if (task.isCanceled()) return;
                        Location location = task.isSuccessful() ? task.getResult() : null;
                        if (Objects.isNull(location)) {
                            finish(listener, null, LocationCapture.NoFix.TIMEOUT);
                        } else if (LocationCapture.isMock(location)) {
                            finish(listener, null, LocationCapture.NoFix.MOCK);
                        } else if (!Coordinates.isValid(location.getLatitude(), location.getLongitude())) {
                            finish(listener, null, LocationCapture.NoFix.TIMEOUT);
                        } else {
                            finish(listener, new LocationFix(location.getLatitude(), location.getLongitude(),
                                    location.hasAccuracy() ? location.getAccuracy() : null, location.getTime()), null);
                        }
                    });
        } catch (SecurityException e) { // permission revoked in between
            Log.e(TAG, "cannot get the current location " + e.getMessage());
            finish(listener, null, LocationCapture.NoFix.NO_PERMISSION);
        }
    }

    private void finish(LocationCapture.CurrentListener listener, LocationFix fix, LocationCapture.NoFix reason) {
        if (done) return;
        done = true;
        if (Objects.nonNull(fix)) {
            listener.onFix(fix);
            return;
        }
        listener.onNoFix(reason);
    }

    @Override
    public void cancel() {
        done = true;
        cancellation.cancel();
    }
}
