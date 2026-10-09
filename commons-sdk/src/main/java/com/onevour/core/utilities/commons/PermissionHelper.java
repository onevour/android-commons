package com.onevour.core.utilities.commons;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Asks the runtime permissions a screen needs, by group (location, camera, storage, bluetooth), the
 * way sds-mobile does:
 * <ul>
 *     <li>only the missing ones are asked; a second request while a dialog is still on screen is not
 *     sent;</li>
 *     <li>Settings (App info) is opened only when every missing permission was refused in this
 *     session and Android will not show its dialog again, with a toast naming them;</li>
 *     <li>{@link #evaluate} turns the answer into what is still missing and a message.</li>
 * </ul>
 * The screen forwards the answer: {@code onRequestPermissionsResult(...)} calls
 * {@link #onRequestPermissionsResult} (a base activity can do it for every screen).
 * <pre>{@code
 * if (new PermissionHelper().requestIfNeeded(this)) startWork();       // all granted already
 *
 * public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
 *     super.onRequestPermissionsResult(requestCode, permissions, grantResults);
 *     PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
 *     if (requestCode != PermissionHelper.REQUEST_CODE) return;
 *     PermissionHelper.Result result = PermissionHelper.evaluate(permissions, grantResults);
 *     if (result.allRequiredGranted()) startWork(); else showMessage(result.message());
 * }
 * }</pre>
 * Texts are Indonesian by default; {@link #setTexts} changes them.
 * <p>
 * To run a method only once its permissions are allowed, see {@link #run} or the
 * {@code @NeedsPermission} / {@code @OnPermissionDenied} annotations (com.onevour.core.permission).
 */
public class PermissionHelper {

    private static final String TAG = PermissionHelper.class.getSimpleName();

    public static final int REQUEST_CODE = 1000;

    /** A request is on screen until this time (ms); a second one meanwhile is not sent. */
    static final long PENDING_TIMEOUT_MS = 60_000;

    /**
     * Permissions the system answered "denied" in this process (see {@link #onRequestPermissionsResult}).
     * Settings is only the way out for one of these that no longer shows a rationale: Android
     * refused it without a dialog. A "never answered" permission looks the same to
     * shouldShowRequestPermissionRationale (false), so an "asked before" flag alone sends users
     * to App info right after a fresh install or a permission reset, before any dialog.
     */
    private static final Set<String> DENIED_THIS_SESSION = Collections.synchronizedSet(new HashSet<>());

    private static volatile long pendingUntil;

    private static volatile Texts texts = new Texts();

    /* ---------- groups ---------- */

    public Set<String> location() {
        Set<String> permissions = new HashSet<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        return permissions;
    }

    public Set<String> camera() {
        return new HashSet<>(Collections.singleton(Manifest.permission.CAMERA));
    }

    /** Not in {@link #permissions()}: ask it only where the app really reads the phone state. */
    public Set<String> phone() {
        return new HashSet<>(Collections.singleton(Manifest.permission.READ_PHONE_STATE));
    }

    /**
     * Only what Android can still grant: READ_MEDIA_IMAGES from Android 13, READ_EXTERNAL_STORAGE up
     * to 12L, WRITE_EXTERNAL_STORAGE up to 10. Asking for one Android no longer grants (WRITE on 11+)
     * is refused without a dialog every time, and would send users to Settings in a loop.
     */
    public Set<String> storage() {
        Set<String> permissions = new HashSet<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        return permissions;
    }

    public Set<String> bluetooth() {
        Set<String> permissions = new HashSet<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
        } else {
            permissions.add(Manifest.permission.BLUETOOTH);
        }
        return permissions;
    }

    /** Android 13+ only (empty before): not in {@link #permissions()}, e.g. for a tracking notification. */
    public Set<String> notifications() {
        Set<String> permissions = new HashSet<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        return permissions;
    }

    /** The required groups: location, camera, storage, bluetooth ("phone" is not required). */
    public Map<String, Set<String>> permissionMap() {
        Map<String, Set<String>> maps = new HashMap<>();
        maps.put("location", location());
        maps.put("camera", camera());
        maps.put("storage", storage());
        maps.put("bluetooth", bluetooth());
        return maps;
    }

    /** The groups the annotations and {@link #permissions(String...)} know. */
    public static final String[] GROUPS = {"location", "camera", "storage", "bluetooth", "phone", "notifications"};

    /** Any group, the optional ones (phone, notifications) included. */
    public Set<String> group(String name) {
        switch (name) {
            case "location":
                return location();
            case "camera":
                return camera();
            case "storage":
                return storage();
            case "bluetooth":
                return bluetooth();
            case "phone":
                return phone();
            case "notifications":
                return notifications();
            default:
                throw new IllegalArgumentException("unknown permission group " + name);
        }
    }

    public Set<String> permissions(String... groups) {
        Set<String> permissions = new HashSet<>();
        for (String group : groups) {
            permissions.addAll(group(group));
        }
        return permissions;
    }

    public Set<String> permissions() {
        Set<String> permissions = new HashSet<>();
        for (Set<String> values : permissionMap().values()) permissions.addAll(values);
        return permissions;
    }

    /* ---------- asking ---------- */

    /** Whether all these are granted; asks nothing. */
    public boolean isGranted(Context context, Collection<String> permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    /** Every required group; true when all are granted already. */
    public boolean requestIfNeeded(Context context) {
        return requestIfNeeded(context, permissions());
    }

    public boolean isGrantLocation(Context context) {
        return requestIfNeeded(context, location());
    }

    public boolean isGrantCamera(Context context) {
        return requestIfNeeded(context, camera());
    }

    public boolean isGrantBluetooth(Context context) {
        return requestIfNeeded(context, bluetooth());
    }

    /**
     * Asks the missing ones of these permissions (or opens Settings when Android will not ask any
     * more); true when all are granted already, false while asking.
     */
    public boolean requestIfNeeded(Context context, Collection<String> permissions) {
        return ask(context, permissions) == GRANTED;
    }

    /** {@link #ask}: all granted already. */
    public static final int GRANTED = 0;
    /** {@link #ask}: the system dialog is on screen; the answer comes to onRequestPermissionsResult. */
    public static final int ASKING = 1;
    /** {@link #ask}: Android will not ask any more, App info was opened. */
    public static final int OPENED_SETTINGS = 2;
    /** {@link #ask}: another request's dialog is still on screen, nothing was asked. */
    public static final int BUSY = 3;
    /** {@link #ask}: missing, but this is not a screen (no Activity), nothing was asked. */
    public static final int NOT_ASKED = 4;

    /**
     * Asks the missing ones of these permissions, or opens Settings when Android will not ask any
     * more; one of {@link #GRANTED}, {@link #ASKING}, {@link #OPENED_SETTINGS}, {@link #BUSY},
     * {@link #NOT_ASKED}. Granted ones are recognised from any context.
     */
    public int ask(Context context, Collection<String> permissions) {
        Set<String> requestList = new HashSet<>();
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                requestList.add(permission);
            }
        }
        if (requestList.isEmpty()) return GRANTED;
        Activity activity = activityOf(context);
        if (Objects.isNull(activity)) {
            Log.w(TAG, "not on a screen (no Activity), cannot request permissions");
            return NOT_ASKED;
        }
        if (isRequestPending()) {
            // the dialog of an earlier request (e.g. the home screen's, while sync asks too) is still
            // waiting for the user: don't stack a second one, and never jump to Settings
            Log.d(TAG, "permission request already on screen");
            return BUSY;
        }
        Set<String> permanentlyDenied = permanentlyDenied(requestList,
                permission -> ActivityCompat.shouldShowRequestPermissionRationale(activity, permission));
        if (!permanentlyDenied.isEmpty() && permanentlyDenied.size() == requestList.size()) {
            // every remaining permission is permanently denied: only then is Settings the only way
            // out; say which ones, App info doesn't
            Toast.makeText(activity, String.format(texts.openSettings, labels(permanentlyDenied)), Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
            activity.startActivity(intent);
            return OPENED_SETTINGS;
        }
        pendingUntil = SystemClock.uptimeMillis() + PENDING_TIMEOUT_MS;
        ActivityCompat.requestPermissions(activity, requestList.toArray(new String[0]), REQUEST_CODE);
        return ASKING;
    }

    /* ---------- run something once the permissions are granted ---------- */

    /** What to do once a {@link #run} request is answered. */
    public interface Callback {

        /** Every permission of the groups was allowed. */
        void granted();

        /**
         * Some were not: the groups not fully allowed (e.g. ["camera"]), all of them when Android
         * would not ask any more and Settings was opened, or when there was no screen to ask from.
         */
        void denied(Set<String> deniedGroups);
    }

    /** The {@link #run} waiting for the dialog's answer: one dialog at a time. */
    private static volatile Waiting waiting;

    private static final class Waiting {
        final Callback callback;
        final Map<String, Set<String>> groups;

        Waiting(Callback callback, Map<String, Set<String>> groups) {
            this.callback = callback;
            this.groups = groups;
        }
    }

    /**
     * Runs granted() now when the permissions of these groups are allowed already; otherwise asks
     * for them and runs granted() once all are allowed, denied(groups) when one is refused. Used by
     * the code generated for {@code @NeedsPermission}; the screen (an Activity, a Fragment or a
     * View) forwards {@link #onRequestPermissionsResult}. Callbacks run on the main thread.
     */
    public void run(Object screen, Callback callback, String... groups) {
        Map<String, Set<String>> byGroup = new java.util.LinkedHashMap<>();
        Set<String> permissions = new HashSet<>();
        for (String group : groups) {
            Set<String> members = group(group);
            byGroup.put(group, members);
            permissions.addAll(members);
        }
        Set<String> all = Collections.unmodifiableSet(new TreeSet<>(byGroup.keySet()));
        Context context = contextOf(screen);
        if (Objects.isNull(context)) {
            Log.w(TAG, "no screen to ask the permissions from");
            callback.denied(all);
            return;
        }
        switch (ask(context, permissions)) {
            case GRANTED:
                callback.granted();
                break;
            case ASKING:
                waiting = new Waiting(callback, byGroup);
                break;
            case BUSY:
                // another dialog is on screen: this call is dropped, the user answers that one first
                Log.d(TAG, "a permission dialog is already on screen, call dropped");
                break;
            default:                                            // OPENED_SETTINGS, NOT_ASKED
                callback.denied(deniedGroups(context, byGroup));
                break;
        }
    }

    /**
     * Inline: {@code run(this, () -> save(), denied -> showMessage(denied), "location", "camera")}.
     * granted runs now when allowed already, or once all are allowed; denied gets the groups not
     * allowed. Same rules as {@link #run(Object, Callback, String...)}.
     */
    public void run(Object screen, Runnable granted, java.util.function.Consumer<Set<String>> denied, String... groups) {
        Objects.requireNonNull(granted);
        Objects.requireNonNull(denied);
        run(screen, new Callback() {
            @Override
            public void granted() {
                granted.run();
            }

            @Override
            public void denied(Set<String> deniedGroups) {
                denied.accept(deniedGroups);
            }
        }, groups);
    }

    /** Inline, nothing to do on a refusal: {@code run(this, () -> save(), "location")}. */
    public void run(Object screen, Runnable granted, String... groups) {
        run(screen, granted, deniedGroups -> Log.d(TAG, "not allowed: " + deniedGroups), groups);
    }

    /** The groups with a permission this context does not hold. */
    private Set<String> deniedGroups(Context context, Map<String, Set<String>> byGroup) {
        Set<String> denied = new TreeSet<>();
        for (Map.Entry<String, Set<String>> group : byGroup.entrySet()) {
            if (!isGranted(context, group.getValue())) denied.add(group.getKey());
        }
        return Collections.unmodifiableSet(denied);
    }

    /** The screen of an Activity, a Fragment, a View or a Context; null when there is none. */
    @Nullable
    static Context contextOf(Object screen) {
        if (screen instanceof androidx.fragment.app.Fragment) return ((androidx.fragment.app.Fragment) screen).getActivity();
        if (screen instanceof android.view.View) return ((android.view.View) screen).getContext();
        if (screen instanceof Context) return (Context) screen;
        return null;
    }

    @Nullable
    static Activity activityOf(Context context) {
        Context current = context;
        while (current instanceof android.content.ContextWrapper) {
            if (current instanceof Activity) return (Activity) current;
            current = ((android.content.ContextWrapper) current).getBaseContext();
        }
        return null;
    }

    /** Records the system's answer; call from {@code onRequestPermissionsResult} of every screen that asks. */
    public static void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_CODE) return;
        pendingUntil = 0;
        // an empty answer: the dialog was dismissed (another app, a configuration change)
        boolean allGranted = grantResults.length > 0;
        for (int i = 0; i < permissions.length && i < grantResults.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                DENIED_THIS_SESSION.remove(permissions[i]);
            } else {
                DENIED_THIS_SESSION.add(permissions[i]);
                allGranted = false;
            }
        }
        Waiting answered = waiting;
        waiting = null;
        if (Objects.isNull(answered)) return;
        if (allGranted) {
            answered.callback.granted();
            return;
        }
        // a group is denied when one of its permissions was answered "no"; an empty answer
        // (the dialog dismissed) leaves every group denied
        Set<String> refused = new HashSet<>();
        for (int i = 0; i < permissions.length && i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) refused.add(permissions[i]);
        }
        Set<String> denied = new TreeSet<>();
        for (Map.Entry<String, Set<String>> group : answered.groups.entrySet()) {
            if (grantResults.length == 0 || !Collections.disjoint(group.getValue(), refused)) denied.add(group.getKey());
        }
        answered.callback.denied(Collections.unmodifiableSet(denied));
    }

    /**
     * What a request answered. Location "while using the app" is enough; the phone permission is
     * never required.
     */
    @NonNull
    public static Result evaluate(@NonNull String[] permissions, @NonNull int[] grantResults) {
        Set<String> missing = new HashSet<>();
        boolean foregroundLocationGranted = false;
        for (int i = 0; i < permissions.length && i < grantResults.length; i++) {
            String permission = permissions[i];
            boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
            if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permission) && granted) foregroundLocationGranted = true;
            if (granted || Manifest.permission.READ_PHONE_STATE.equals(permission)) continue;
            missing.add(permission);
        }
        return new Result(missing, foregroundLocationGranted);
    }

    public static final class Result {

        /** Required permissions the user denied (phone is never here). */
        public final Set<String> missing;

        /** Precise location was granted by this answer: location work (tracking) can start now. */
        public final boolean foregroundLocationGranted;

        Result(Set<String> missing, boolean foregroundLocationGranted) {
            this.missing = Collections.unmodifiableSet(missing);
            this.foregroundLocationGranted = foregroundLocationGranted;
        }

        public boolean allRequiredGranted() {
            return missing.isEmpty();
        }

        /** "Izin belum diberikan: Kamera, Lokasi", or null when nothing required is missing. */
        @Nullable
        public String message() {
            return missing.isEmpty() ? null : String.format(texts.missing, labels(missing));
        }
    }

    /* ---------- texts ---------- */

    /** What the user reads; Indonesian by default. Each %s is the list of permission names. */
    public static final class Texts {
        public String missing = "Izin belum diberikan: %s";
        public String openSettings = "Izinkan di Pengaturan > Izin: %s";
        public String location = "Lokasi";
        public String camera = "Kamera";
        public String phone = "Telepon";
        public String nearbyDevices = "Perangkat di sekitar";
        public String notifications = "Notifikasi";
        public String media = "Foto dan video";
    }

    /** E.g. English texts, once from Application#onCreate. */
    public static void setTexts(@NonNull Texts value) {
        texts = Objects.requireNonNull(value);
    }

    /**
     * "Izin belum diberikan: Kamera, Lokasi" for the groups an @OnPermissionDenied method (or a
     * {@link Callback#denied}) received.
     */
    public String deniedMessage(Set<String> groups) {
        return String.format(texts.missing, labels(permissions(groups.toArray(new String[0]))));
    }

    /** "Kamera, Lokasi" for these permissions, as Settings groups them. */
    static String labels(Set<String> permissions) {
        Set<String> names = new TreeSet<>();
        for (String permission : permissions) names.add(label(permission));
        return String.join(", ", names);
    }

    static String label(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                return texts.location;
            case Manifest.permission.CAMERA:
                return texts.camera;
            case Manifest.permission.READ_PHONE_STATE:
                return texts.phone;
            case Manifest.permission.BLUETOOTH:
            case Manifest.permission.BLUETOOTH_SCAN:
            case Manifest.permission.BLUETOOTH_CONNECT:
            case Manifest.permission.BLUETOOTH_ADVERTISE:
                return texts.nearbyDevices;
            case Manifest.permission.POST_NOTIFICATIONS:
                return texts.notifications;
            default:
                return texts.media;
        }
    }

    /* ---------- for the logic above and its tests ---------- */

    static boolean isRequestPending() {
        return SystemClock.uptimeMillis() < pendingUntil;
    }

    /**
     * The missing permissions Settings has to grant: answered "denied" in this session and no
     * rationale any more (Android won't show the dialog again).
     */
    static Set<String> permanentlyDenied(Set<String> missing, Predicate<String> showRationale) {
        Set<String> result = new HashSet<>();
        for (String permission : missing) {
            if (DENIED_THIS_SESSION.contains(permission) && !showRationale.test(permission)) result.add(permission);
        }
        return result;
    }

    /** Tests: as if run(...) were waiting for the dialog of these groups. */
    static void waitForTest(Callback callback, String... groups) {
        Map<String, Set<String>> byGroup = new java.util.LinkedHashMap<>();
        for (String group : groups) byGroup.put(group, new PermissionHelper().group(group));
        waiting = new Waiting(callback, byGroup);
    }

    /** Tests: forget answers, pending requests and changed texts. */
    static void resetSession() {
        DENIED_THIS_SESSION.clear();
        pendingUntil = 0;
        texts = new Texts();
        waiting = null;
    }
}
