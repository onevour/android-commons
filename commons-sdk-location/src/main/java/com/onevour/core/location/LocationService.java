package com.onevour.core.location;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.Objects;

/**
 * The tracking itself (see {@link LocationCapture}): a battery-saving fix every 10 to 20 minutes, only
 * after moving 500 m, handed to the app ({@link LocationCapture.Config}). Declared by the app.
 */
public class LocationService extends Service {

    private static final String TAG = LocationService.class.getSimpleName();

    /** The only start: from a visible screen, see {@link LocationCapture#sync}. */
    static final String ACTION_START = "com.onevour.core.location.START";

    private static final int NOTIFICATION_ID = 1001;

    private static final String CHANNEL = "loc_chan";

    /** A fix this close in time to the previous one is the same fix delivered twice. */
    private static final long SAME_FIX_MS = 10_000;

    private FusedLocationProviderClient fusedClient;

    private LocationRequest locationRequest;

    private LocationCallback locationCallback;

    private Location last;

    private boolean updatesActive;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        // battery saving (Wi-Fi / cell, no GPS), every 10 to 20 minutes and only after moving 500 m
        // (unless the app set its own); the exact position is the app's own business, see LocationCapture.current
        LocationCapture.Config config = LocationCapture.config();
        locationRequest = new LocationRequest.Builder(config.intervalMs())
                .setPriority(config.priority())
                .setMinUpdateIntervalMillis(config.minIntervalMs())
                .setMinUpdateDistanceMeters(config.minDistanceMetres())
                .build();
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                Location location = result.getLastLocation();
                if (Objects.isNull(location)) return;
                // the app no longer wants the position: stop right away instead of waiting for the watchdog
                if (!LocationCapture.shouldCaptureNow()) {
                    Log.d(TAG, "tracking time over, stop tracking");
                    stopCapture();
                    return;
                }
                if (Objects.nonNull(last) && location.getTime() - last.getTime() < SAME_FIX_MS) return;
                last = location;
                LocationCapture.onServiceFix(location);
            }
        };
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // only started from a visible screen: a restart by the system would be in the background,
        // where location "while using the app" gives nothing -- not sticky, the app's next
        // visible screen starts it again
        if (Objects.isNull(intent) || !ACTION_START.equals(intent.getAction())
                || !LocationCapture.hasForegroundLocationPermission(this)) {
            Log.w(TAG, "not started from the app or location permission not granted");
            stopCapture();
            return START_NOT_STICKY;
        }
        // the app does not want the position now
        if (!LocationCapture.shouldCaptureNow()) {
            Log.d(TAG, "not tracking time, stop tracking");
            stopCapture();
            return START_NOT_STICKY;
        }
        startCapture();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        // otherwise the fused client keeps delivering to a callback of a dead service
        removeUpdates();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /** Enters the foreground and starts the location updates if they are not running yet. */
    private void startCapture() {
        try {
            showNotification();
        } catch (RuntimeException e) { // SecurityException / ForegroundServiceStartNotAllowedException
            Log.e(TAG, "cannot start location foreground service " + e.getMessage());
            stopSelf();
            return;
        }
        if (updatesActive) return;
        try {
            fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            updatesActive = true;
        } catch (SecurityException e) {
            Log.e(TAG, "cannot request location updates " + e.getMessage());
        }
    }

    private void showNotification() {
        LocationCapture.Config config = LocationCapture.config();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL)
                .setContentTitle(config.notificationTitle())
                .setContentText(config.notificationText())
                .setSmallIcon(config.notificationIcon())
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification);
            return;
        }
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
    }

    private void stopCapture() {
        removeUpdates();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void removeUpdates() {
        updatesActive = false;
        if (Objects.isNull(fusedClient) || Objects.isNull(locationCallback)) return;
        fusedClient.removeLocationUpdates(locationCallback);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL, "Location Service", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
}
