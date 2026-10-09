package com.onevour.sdk.impl.modules.location;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.onevour.core.location.LocationCapture;
import com.onevour.core.location.LocationService;
import com.onevour.sdk.impl.R;
import com.onevour.sdk.impl.databinding.ActivityLocationCaptureBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Tries android-commons' location capture on a real phone: start / stop it as an app would, set its
 * interval and distance, and see every fix the service got -- its accuracy and source -- and whether
 * the library delivered it or dropped it (fake GPS, accuracy over 100 m, near 0,0, too soon after the
 * last one).
 */
public class LocationCaptureActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 7001;

    private static final int STOP_COLOR = 0xFFB3261E;

    private final SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private final Rows adapter = new Rows();

    private ActivityLocationCaptureBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLocationCaptureBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SampleScreen.setUp(this, binding.toolbar, binding.content, "Location Capture");
        binding.rows.setLayoutManager(new LinearLayoutManager(this));
        binding.rows.setAdapter(adapter);
        binding.permission.setOnClickListener(v -> askPermissions());
        binding.locationSettings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
        binding.toggle.setOnClickListener(v -> {
            LocationSample.setWanted(!LocationSample.isWanted());
            LocationCapture.sync(this);
            render();
            binding.getRoot().postDelayed(this::render, 800); // the service state settles a moment later
        });
        binding.clear.setOnClickListener(v -> LocationSample.clear());
        binding.settings.setOnClickListener(v -> startActivity(new Intent(this, LocationSettingsActivity.class)));
        binding.openTransaction.setOnClickListener(v -> startActivity(new Intent(this, SampleHomeActivity.class)));
        binding.openTransactionValue.setText("Home, absen, check-in, register, riwayat tracking");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // a visible screen, like an app's home: the capture runs when it is wanted
        LocationCapture.sync(this);
        LocationSample.setListener(this::render);
        render();
        binding.getRoot().postDelayed(this::render, 800);
    }

    @Override
    protected void onPause() {
        LocationSample.setListener(null);
        super.onPause();
    }

    private void askPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), REQUEST_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        LocationCapture.sync(this);
        render();
    }

    private void render() {
        if (Objects.isNull(binding) || isFinishing()) return;
        renderStatus();
        LocationSample.Settings settings = LocationSample.settings();
        binding.settingsValue.setText(settings.describe());
        List<LocationSample.Row> rows = LocationSample.rows();
        int delivered = 0;
        for (LocationSample.Row row : rows) {
            if (row.delivered) delivered++;
        }
        SampleChips.neutral(binding.countReceived, "diterima " + rows.size());
        SampleChips.good(binding.countDelivered, "diteruskan " + delivered);
        if (rows.size() - delivered > 0) {
            SampleChips.bad(binding.countDropped, "dibuang " + (rows.size() - delivered));
        } else {
            SampleChips.neutral(binding.countDropped, "dibuang 0");
        }
        binding.summary.setContentDescription(String.format(Locale.getDefault(),
                "Fix diterima %d, diteruskan %d, dibuang %d", rows.size(), delivered, rows.size() - delivered));
        binding.explanation.setText(String.format(Locale.getDefault(),
                "Diteruskan = lolos filter library: bukan fake GPS, akurasi maks. %.0f m, minimal %d detik dari fix sebelumnya (90%% interval minimum).",
                LocationCapture.MAX_ACCURACY_METRES, settings.minIntervalMinutes * 60 * 9 / 10));
        binding.empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        binding.clear.setEnabled(!rows.isEmpty());
        adapter.submit(rows);
    }

    private void renderStatus() {
        boolean fine = LocationCapture.hasForegroundLocationPermission(this);
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean locationOn = Objects.nonNull(manager) && LocationManagerCompat.isLocationEnabled(manager);
        boolean wanted = LocationSample.isWanted();
        boolean running = isServiceRunning();
        if (fine) SampleChips.good(binding.statusPermission, "Tepat");
        else if (coarse) SampleChips.warn(binding.statusPermission, "Perkiraan saja");
        else SampleChips.bad(binding.statusPermission, "Belum");
        binding.permission.setVisibility(fine ? View.GONE : View.VISIBLE);
        if (locationOn) SampleChips.good(binding.statusLocation, "Nyala");
        else SampleChips.bad(binding.statusLocation, "Mati");
        binding.locationSettings.setVisibility(locationOn ? View.GONE : View.VISIBLE);
        if (wanted) SampleChips.good(binding.statusWanted, "Ya");
        else SampleChips.neutral(binding.statusWanted, "Tidak");
        if (running) SampleChips.good(binding.statusService, "Berjalan");
        else if (wanted) SampleChips.warn(binding.statusService, "Berhenti");
        else SampleChips.neutral(binding.statusService, "Berhenti");
        binding.toggle.setText(wanted ? "Hentikan tracking" : "Mulai tracking");
        ViewCompat.setBackgroundTintList(binding.toggle, ColorStateList.valueOf(wanted
                ? STOP_COLOR : ContextCompat.getColor(this, R.color.colorPrimary)));
    }

    @SuppressWarnings("deprecation") // still returns the app's own services
    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LocationService.class.getName().equals(service.service.getClassName())) return true;
        }
        return false;
    }

    private final class Rows extends RecyclerView.Adapter<Rows.Holder> {

        private final List<LocationSample.Row> rows = new ArrayList<>();

        void submit(List<LocationSample.Row> newRows) {
            rows.clear();
            rows.addAll(newRows);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_capture_fix, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            LocationSample.Row row = rows.get(position);
            Location location = row.location;
            boolean mock = LocationCompat.isMock(location);
            holder.time.setText(time.format(location.getTime()));
            if (row.delivered) SampleChips.good(holder.status, "Diteruskan");
            else SampleChips.bad(holder.status, "Dibuang");
            holder.coordinates.setText(String.format(Locale.getDefault(), "%.6f, %.6f", location.getLatitude(), location.getLongitude()));
            SampleChips.accuracy(holder.accuracy, location.hasAccuracy() ? location.getAccuracy() : null);
            SampleChips.neutral(holder.source, "sumber " + location.getProvider());
            holder.mock.setVisibility(mock ? View.VISIBLE : View.GONE);
            if (mock) SampleChips.bad(holder.mock, "FAKE GPS");
            holder.itemView.setContentDescription(String.format(Locale.getDefault(), "%s, %s, %s, %s, %s%s",
                    holder.time.getText(), holder.status.getText(), holder.coordinates.getText(),
                    holder.accuracy.getText(), holder.source.getText(), mock ? ", fake GPS" : ""));
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        final class Holder extends RecyclerView.ViewHolder {

            final TextView time;

            final TextView status;

            final TextView coordinates;

            final TextView accuracy;

            final TextView source;

            final TextView mock;

            Holder(View view) {
                super(view);
                time = view.findViewById(R.id.time);
                status = view.findViewById(R.id.status);
                coordinates = view.findViewById(R.id.coordinates);
                accuracy = view.findViewById(R.id.chip_accuracy);
                source = view.findViewById(R.id.chip_source);
                mock = view.findViewById(R.id.chip_mock);
            }
        }
    }
}
