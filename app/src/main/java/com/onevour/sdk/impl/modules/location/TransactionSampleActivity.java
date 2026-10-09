package com.onevour.sdk.impl.modules.location;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.onevour.core.location.LocationCapture;
import com.onevour.core.location.LocationFix;
import com.onevour.sdk.impl.databinding.ActivityTransactionSampleBinding;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A screen of the app that stamps where the user is -- absen, check-in, register customer: it does not
 * wait for the tracking service, it asks one exact fix with {@link LocationCapture#current}, shows a
 * loading state while the GPS works, then "saves" the transaction with that fix. Absen masuk also
 * starts the tracking, absen pulang stops it: that decision is the app's, not the library's. The map
 * below shows where it was stamped, with the fix's accuracy as a circle.
 */
public class TransactionSampleActivity extends AppCompatActivity {

    public static final String EXTRA_KIND = "kind";

    private static final double MAP_ZOOM = 17.0;

    /** How long the user waits for the GPS at most. */
    private static final long TIMEOUT_MS = 15_000;

    /** What the screen stamps, and how old a fix it accepts. */
    public enum Kind {
        /** Absen masuk / pulang: where the user is now. */
        ABSEN("Absen", 0),
        /** Check-in at a store: where the user is now. */
        CHECK_IN("Check-in toko", 0),
        /** The customer's position: a GPS fix of the last 2 minutes is good enough. */
        REGISTER("Register customer", 120_000);

        final String title;

        final long maxAgeMs;

        Kind(String title, long maxAgeMs) {
            this.title = title;
            this.maxAgeMs = maxAgeMs;
        }
    }

    private Kind kind;

    private final SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private ActivityTransactionSampleBinding binding;

    private LocationCapture.Pending pending;

    private Marker fixMarker;

    private Polygon accuracyCircle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // OpenStreetMap asks every app to identify itself when downloading tiles
        Configuration.getInstance().setUserAgentValue(getPackageName());
        binding = ActivityTransactionSampleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        kind = Kind.valueOf(Objects.requireNonNull(getIntent().getStringExtra(EXTRA_KIND)));
        SampleScreen.setUp(this, binding.toolbar, binding.content, kind.title + " (simulasi)");
        binding.explanation.setText(String.format(Locale.getDefault(),
                "Saat tombol ditekan, layar ini minta lokasi GPS lewat LocationCapture.current (%s, tunggu maks. %d detik), tidak menunggu service tracking.",
                kind.maxAgeMs == 0 ? "harus diambil sekarang" : "boleh fix GPS " + kind.maxAgeMs / 60_000 + " menit terakhir",
                TIMEOUT_MS / 1000));
        renderAction();
        binding.checkIn.setOnClickListener(v -> stamp());
        setUpMap();
    }

    private void renderAction() {
        if (kind != Kind.ABSEN) {
            binding.checkIn.setText(kind == Kind.CHECK_IN ? "Check-in" : "Simpan customer");
            return;
        }
        binding.checkIn.setText(LocationSample.isWanted() ? "Absen pulang" : "Absen masuk");
    }

    private void stamp() {
        long started = SystemClock.elapsedRealtime();
        showLoading(true);
        binding.resultCard.setVisibility(View.GONE);
        pending = LocationCapture.current(this, kind.maxAgeMs, TIMEOUT_MS, new LocationCapture.CurrentListener() {
            @Override
            public void onFix(@NonNull LocationFix fix) {
                pending = null;
                showLoading(false);
                long waited = SystemClock.elapsedRealtime() - started;
                // an app saves the transaction here, with fix.getLatitude() / fix.getLongitude()
                String saved = saved(fix);
                binding.resultCard.setVisibility(View.VISIBLE);
                binding.resultTitle.setText(saved);
                SampleChips.good(binding.resultStatus, "Tersimpan");
                binding.result.setText(String.format(Locale.getDefault(), "%.6f, %.6f", fix.getLatitude(), fix.getLongitude()));
                binding.resultChips.setVisibility(View.VISIBLE);
                SampleChips.accuracy(binding.chipAccuracy, fix.getAccuracy());
                SampleChips.neutral(binding.chipFixTime, "⏲ fix " + time.format(fix.getTime()));
                SampleChips.neutral(binding.chipWaited, String.format(Locale.getDefault(), "⏱ menunggu %.1f dtk", waited / 1000.0));
                binding.resultCard.setContentDescription(String.format(Locale.getDefault(), "%s. %s, %s, %s, %s",
                        saved, binding.result.getText(), binding.chipAccuracy.getText(), binding.chipFixTime.getText(), binding.chipWaited.getText()));
                showOnMap(fix);
            }

            @Override
            public void onNoFix(@NonNull LocationCapture.NoFix reason) {
                pending = null;
                showLoading(false);
                binding.resultCard.setVisibility(View.VISIBLE);
                binding.resultTitle.setText(kind.title + " gagal");
                SampleChips.bad(binding.resultStatus, "Gagal");
                binding.result.setText(message(reason));
                binding.resultChips.setVisibility(View.GONE);
                binding.resultCard.setContentDescription(kind.title + " gagal. " + message(reason));
            }
        });
    }

    /** Before a fix: around the last known point of the history (or Jakarta). */
    private void setUpMap() {
        binding.map.setTileSource(TileSourceFactory.MAPNIK);
        binding.map.setMultiTouchControls(true);
        binding.map.getOverlays().add(new CopyrightOverlay(this));
        List<LocationHistory.Entry> history = LocationHistory.all(this);
        GeoPoint start = history.isEmpty() ? new GeoPoint(-6.1754, 106.8272)
                : new GeoPoint(history.get(history.size() - 1).latitude, history.get(history.size() - 1).longitude);
        binding.map.getController().setZoom(MAP_ZOOM);
        binding.map.getController().setCenter(start);
    }

    /** The stamped fix: a marker, and a circle as wide as its accuracy. */
    private void showOnMap(LocationFix fix) {
        GeoPoint point = new GeoPoint(fix.getLatitude(), fix.getLongitude());
        if (Objects.nonNull(fixMarker)) binding.map.getOverlays().remove(fixMarker);
        if (Objects.nonNull(accuracyCircle)) binding.map.getOverlays().remove(accuracyCircle);
        if (Objects.nonNull(fix.getAccuracy())) {
            accuracyCircle = new Polygon(binding.map);
            accuracyCircle.setPoints(Polygon.pointsAsCircle(point, fix.getAccuracy()));
            accuracyCircle.getFillPaint().setColor(0x33008577);
            accuracyCircle.getOutlinePaint().setColor(0xAA008577);
            accuracyCircle.getOutlinePaint().setStrokeWidth(3f);
            accuracyCircle.setInfoWindow(null);
            binding.map.getOverlays().add(accuracyCircle);
        }
        fixMarker = new Marker(binding.map);
        fixMarker.setPosition(point);
        fixMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        fixMarker.setTitle(kind.title);
        fixMarker.setSnippet(time.format(fix.getTime()));
        binding.map.getOverlays().add(fixMarker);
        binding.map.getController().setZoom(MAP_ZOOM);
        binding.map.getController().animateTo(point);
        binding.map.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.map.onResume();
    }

    @Override
    protected void onPause() {
        binding.map.onPause();
        super.onPause();
    }

    /** The transaction is saved; absen also decides whether the tracking runs. */
    private String saved(LocationFix fix) {
        if (kind == Kind.CHECK_IN) {
            LocationHistory.add(this, LocationHistory.Type.CHECK_IN, fix);
            return "Check-in tersimpan";
        }
        if (kind == Kind.REGISTER) {
            LocationHistory.add(this, LocationHistory.Type.REGISTER, fix);
            return "Customer tersimpan dengan lokasi ini";
        }
        boolean masuk = !LocationSample.isWanted();
        LocationHistory.add(this, masuk ? LocationHistory.Type.ABSEN_MASUK : LocationHistory.Type.ABSEN_PULANG, fix);
        LocationSample.setWanted(masuk);
        LocationCapture.sync(this); // visible screen: start (masuk) or stop (pulang) the tracking now
        renderAction();
        return masuk ? "Absen masuk tersimpan, tracking mulai" : "Absen pulang tersimpan, tracking berhenti";
    }

    /** The app words the reason. */
    private static String message(LocationCapture.NoFix reason) {
        switch (reason) {
            case NO_PERMISSION:
                return "izin lokasi tepat belum diberikan.";
            case LOCATION_OFF:
                return "lokasi HP mati, nyalakan dulu.";
            case MOCK:
                return "terdeteksi fake GPS.";
            default:
                return "sinyal GPS tidak didapat, coba di tempat terbuka.";
        }
    }

    private void showLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.progress.announceForAccessibility(loading ? "Mengambil lokasi GPS" : "");
        binding.checkIn.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        // the screen is gone: GPS off, no callback into a dead screen
        if (Objects.nonNull(pending)) pending.cancel();
        super.onDestroy();
    }
}
