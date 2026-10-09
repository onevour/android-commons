package com.onevour.core.utilities.commons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.Manifest;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

public class PermissionHelperTest {

    @Test
    public void phoneState_isNotRequired() {
        assertFalse(new PermissionHelper().permissions().contains(Manifest.permission.READ_PHONE_STATE));
    }

    @Test
    public void labels_nameEachSettingsGroupOnce() {
        assertEquals("Kamera, Lokasi", PermissionHelper.labels(new HashSet<>(Arrays.asList(
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA))));
        assertEquals("Telepon", PermissionHelper.label(Manifest.permission.READ_PHONE_STATE));
        assertEquals("Perangkat di sekitar", PermissionHelper.label(Manifest.permission.BLUETOOTH_SCAN));
    }

    private static final int GRANTED = android.content.pm.PackageManager.PERMISSION_GRANTED;
    private static final int DENIED = android.content.pm.PackageManager.PERMISSION_DENIED;

    @Test
    public void evaluate_preciseLocationGranted_startsTracking() {
        PermissionHelper.Result result = PermissionHelper.evaluate(
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA},
                new int[]{GRANTED, GRANTED, GRANTED});
        org.junit.Assert.assertTrue(result.allRequiredGranted());
        org.junit.Assert.assertTrue(result.foregroundLocationGranted);
    }

    @Test
    public void evaluate_approximateOnlyOrCameraDenied_namesWhatIsMissing() {
        PermissionHelper.Result result = PermissionHelper.evaluate(
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.CAMERA, Manifest.permission.READ_PHONE_STATE},
                new int[]{DENIED, GRANTED, DENIED, DENIED});
        assertFalse(result.allRequiredGranted());
        assertFalse(result.foregroundLocationGranted);
        assertEquals("Izin belum diberikan: Kamera, Lokasi", result.message());
    }

    // ------------------------------------------------------------------------------------
    // when Settings is the only way left
    // ------------------------------------------------------------------------------------

    private static final java.util.Set<String> CAMERA_AND_LOCATION = new HashSet<>(Arrays.asList(
            Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION));

    @Test
    public void neverAnswered_isNotPermanentlyDenied_evenWithoutRationale() {
        PermissionHelper.resetSession();
        // fresh install / permission reset / a second request while the first dialog is open:
        // no rationale, but the salesman never said no -> show the dialog, not Settings
        org.junit.Assert.assertTrue(PermissionHelper.permanentlyDenied(CAMERA_AND_LOCATION, p -> false).isEmpty());
    }

    @Test
    public void deniedOnce_withRationale_asksAgain() {
        PermissionHelper.resetSession();
        PermissionHelper.onRequestPermissionsResult(PermissionHelper.REQUEST_CODE,
                new String[]{Manifest.permission.CAMERA}, new int[]{DENIED});
        org.junit.Assert.assertTrue(PermissionHelper.permanentlyDenied(CAMERA_AND_LOCATION, p -> true).isEmpty());
    }

    @Test
    public void deniedWithoutRationale_goesToSettings_onlyForThatPermission() {
        PermissionHelper.resetSession();
        PermissionHelper.onRequestPermissionsResult(PermissionHelper.REQUEST_CODE,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION}, new int[]{DENIED, GRANTED});
        assertEquals(new HashSet<>(Arrays.asList(Manifest.permission.CAMERA)),
                PermissionHelper.permanentlyDenied(CAMERA_AND_LOCATION, p -> false));
    }

    @Test
    public void grantedLater_isForgotten_andOtherRequestCodesAreIgnored() {
        PermissionHelper.resetSession();
        PermissionHelper.onRequestPermissionsResult(PermissionHelper.REQUEST_CODE,
                new String[]{Manifest.permission.CAMERA}, new int[]{DENIED});
        PermissionHelper.onRequestPermissionsResult(PermissionHelper.REQUEST_CODE,
                new String[]{Manifest.permission.CAMERA}, new int[]{GRANTED});
        PermissionHelper.onRequestPermissionsResult(42, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, new int[]{DENIED});
        org.junit.Assert.assertTrue(PermissionHelper.permanentlyDenied(CAMERA_AND_LOCATION, p -> false).isEmpty());
    }

    // ------------------------------------------------------------------------------------
    // library additions
    // ------------------------------------------------------------------------------------

    @Test
    public void deniedMessage_namesTheDeniedGroups() {
        PermissionHelper helper = new PermissionHelper();
        assertEquals("Izin belum diberikan: Kamera, Lokasi",
                helper.deniedMessage(new HashSet<>(Arrays.asList("location", "camera"))));
    }

    @Test
    public void textsCanBeChanged_andTheDefaultsComeBack() {
        PermissionHelper.resetSession();
        PermissionHelper.Texts english = new PermissionHelper.Texts();
        english.missing = "Permission not granted: %s";
        english.location = "Location";
        english.camera = "Camera";
        PermissionHelper.setTexts(english);
        PermissionHelper.Result result = PermissionHelper.evaluate(
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA}, new int[]{DENIED, DENIED});
        assertEquals("Permission not granted: Camera, Location", result.message());
        PermissionHelper.resetSession();
        assertEquals("Kamera", PermissionHelper.label(Manifest.permission.CAMERA));
    }

    @Test
    public void optionalGroups_areNotInTheDefaultRequest() {
        PermissionHelper helper = new PermissionHelper();
        assertFalse(helper.permissions().containsAll(helper.phone()));
        org.junit.Assert.assertTrue(helper.permissions("location", "camera").containsAll(helper.location()));
    }

    // ------------------------------------------------------------------------------------
    // run(...): granted once all are allowed, denied with the groups not allowed
    // ------------------------------------------------------------------------------------

    private static final class Recorder implements PermissionHelper.Callback {
        boolean granted;
        java.util.Set<String> denied;

        @Override
        public void granted() {
            granted = true;
        }

        @Override
        public void denied(java.util.Set<String> deniedGroups) {
            denied = deniedGroups;
        }
    }

    @Test
    public void run_allAllowed_runsGranted() {
        PermissionHelper.resetSession();
        Recorder recorder = new Recorder();
        PermissionHelper.waitForTest(recorder, "location", "camera");
        PermissionHelper.onRequestPermissionsResult(PermissionHelper.REQUEST_CODE,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA}, new int[]{GRANTED, GRANTED});
        org.junit.Assert.assertTrue(recorder.granted);
        org.junit.Assert.assertNull(recorder.denied);
    }

    @Test
    public void run_oneRefused_namesOnlyItsGroup() {
        PermissionHelper.resetSession();
        Recorder recorder = new Recorder();
        PermissionHelper.waitForTest(recorder, "location", "camera");
        PermissionHelper.onRequestPermissionsResult(PermissionHelper.REQUEST_CODE,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA},
                new int[]{GRANTED, GRANTED, DENIED});
        assertFalse(recorder.granted);
        assertEquals(new HashSet<>(Arrays.asList("camera")), recorder.denied);
    }

    @Test
    public void run_approximateLocationOnly_isLocationDenied() {
        PermissionHelper.resetSession();
        Recorder recorder = new Recorder();
        PermissionHelper.waitForTest(recorder, "location");
        PermissionHelper.onRequestPermissionsResult(PermissionHelper.REQUEST_CODE,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, new int[]{DENIED, GRANTED});
        assertEquals(new HashSet<>(Arrays.asList("location")), recorder.denied);
    }

    @Test
    public void run_dismissedDialog_deniesEveryGroup_andAnswersOnce() {
        PermissionHelper.resetSession();
        Recorder recorder = new Recorder();
        PermissionHelper.waitForTest(recorder, "location", "camera");
        PermissionHelper.onRequestPermissionsResult(PermissionHelper.REQUEST_CODE, new String[0], new int[0]);
        assertEquals(new HashSet<>(Arrays.asList("camera", "location")), recorder.denied);

        Recorder late = new Recorder();
        PermissionHelper.onRequestPermissionsResult(PermissionHelper.REQUEST_CODE,
                new String[]{Manifest.permission.CAMERA}, new int[]{GRANTED});      // nothing waits any more
        assertFalse(late.granted);
    }
}
