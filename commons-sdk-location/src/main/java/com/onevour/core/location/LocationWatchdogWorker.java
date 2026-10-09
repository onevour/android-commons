package com.onevour.core.location;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Watchdog: stops the tracking once the app no longer wants it ({@link LocationCapture#shouldCaptureNow}) while the user stands
 * still and no fix arrives to let the service stop itself. It never starts the tracking -- that needs
 * a visible screen with location "while using the app" (see {@link LocationCapture#sync}).
 */
public class LocationWatchdogWorker extends Worker {

    private static final String TAG = LocationWatchdogWorker.class.getSimpleName();

    public LocationWatchdogWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        if (LocationCapture.shouldCaptureNow() || !isRunning(context)) return Result.success();
        Log.d(TAG, "not tracking time, stop tracking");
        context.stopService(new Intent(context, LocationService.class));
        return Result.success();
    }

    @SuppressWarnings("deprecation") // still returns the app's own services
    private static boolean isRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LocationService.class.getName().equals(service.service.getClassName())) return true;
        }
        return false;
    }
}
