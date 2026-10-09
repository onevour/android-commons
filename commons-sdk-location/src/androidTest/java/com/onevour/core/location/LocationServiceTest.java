package com.onevour.core.location;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The capture on a device: when the app wants the position, {@link LocationService} runs
 * and the device's own fixes (the emulator's GPS) reach the app; when it stops wanting it, or
 * logs out, it all stops.
 */
@RunWith(AndroidJUnit4.class)
public class LocationServiceTest {

    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    private final List<LocationFix> delivered = new CopyOnWriteArrayList<>();

    private final CountDownLatch firstFix = new CountDownLatch(1);

    private volatile boolean wanted = true;

    @Before
    public void setUp() {
        InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(
                context.getPackageName(), Manifest.permission.ACCESS_FINE_LOCATION);
        Application app = (Application) context.getApplicationContext();
        LocationCapture.init(app, LocationCapture.Config.builder(() -> wanted, fix -> {
            delivered.add(fix);
            firstFix.countDown();
        }).build());
        LocationCapture.stop(context); // no last fix from an earlier run
    }

    @After
    public void tearDown() {
        LocationCapture.stop(context);
    }

    @Test
    public void whileTheAppWantsIt_fixesReachTheApp_untilItStops() throws Exception {
        try (ActivityScenario<LocationTestActivity> screen = ActivityScenario.launch(LocationTestActivity.class)) {
            screen.onActivity(LocationCapture::sync);

            assertTrue("a fix reaches the app", firstFix.await(90, TimeUnit.SECONDS));
            assertTrue(isRunning());

            wanted = false;
            screen.onActivity(LocationCapture::sync);
            waitUntilStopped();
            assertFalse("the app no longer wants it: stopped", isRunning());
        }
    }

    @Test
    public void notWanted_neverStarts() throws Exception {
        wanted = false;
        try (ActivityScenario<LocationTestActivity> screen = ActivityScenario.launch(LocationTestActivity.class)) {
            screen.onActivity(LocationCapture::sync);

            Thread.sleep(2_000);
            assertFalse(isRunning());
            assertTrue(delivered.isEmpty());
        }
    }

    private void waitUntilStopped() throws InterruptedException {
        long until = System.currentTimeMillis() + 10_000;
        while (isRunning() && System.currentTimeMillis() < until) Thread.sleep(200);
    }

    @SuppressWarnings("deprecation") // still returns the app's own services
    private boolean isRunning() {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LocationService.class.getName().equals(service.service.getClassName())) return true;
        }
        return false;
    }
}
