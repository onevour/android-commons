package com.onevour.sdk.impl.modules.location;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The address of a point, from OpenStreetMap's Nominatim reverse geocoding. Its usage policy: identify
 * the app (User-Agent), at most one request per second, keep the answers -- the caller stores them.
 */
final class OsmAddress {

    interface Callback {

        void onAddress(String address);

        void onError(String message);
    }

    private static final String TAG = OsmAddress.class.getSimpleName();

    private static final long MIN_GAP_MS = 1_100;

    private static final int TIMEOUT_MS = 15_000;

    /** One at a time, so the requests stay a second apart. */
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static long lastRequest;

    private OsmAddress() {
    }

    /** The callback runs on the main thread. */
    static void reverse(String userAgent, double latitude, double longitude, Callback callback) {
        EXECUTOR.execute(() -> {
            long wait = lastRequest + MIN_GAP_MS - SystemClock.elapsedRealtime();
            if (wait > 0) SystemClock.sleep(wait);
            lastRequest = SystemClock.elapsedRealtime();
            try {
                String address = fetch(userAgent, latitude, longitude);
                MAIN.post(() -> callback.onAddress(address));
            } catch (IOException | RuntimeException e) {
                Log.e(TAG, "reverse geocoding failed " + e.getMessage());
                MAIN.post(() -> callback.onError("Alamat tidak didapat, cek koneksi internet lalu coba lagi."));
            }
        });
    }

    private static String fetch(String userAgent, double latitude, double longitude) throws IOException {
        Uri uri = Uri.parse("https://nominatim.openstreetmap.org/reverse").buildUpon()
                .appendQueryParameter("format", "jsonv2")
                .appendQueryParameter("lat", String.format(Locale.US, "%.6f", latitude))
                .appendQueryParameter("lon", String.format(Locale.US, "%.6f", longitude))
                .appendQueryParameter("accept-language", "id")
                .build();
        HttpURLConnection connection = (HttpURLConnection) new URL(uri.toString()).openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", userAgent);
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + status);
            try (Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                if (!json.has("display_name")) return "Tidak ada alamat untuk titik ini";
                return json.get("display_name").getAsString();
            }
        } finally {
            connection.disconnect();
        }
    }
}
