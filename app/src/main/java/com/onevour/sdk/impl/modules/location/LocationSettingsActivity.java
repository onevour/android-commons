package com.onevour.sdk.impl.modules.location;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.onevour.sdk.impl.databinding.ActivityLocationSettingsBinding;

import java.util.Locale;
import java.util.Objects;

/**
 * The capture's interval, minimum interval, distance and source, typed by the user: saved and applied
 * right away with {@code LocationCapture.reconfigure} (a running capture restarts with them).
 */
public class LocationSettingsActivity extends AppCompatActivity {

    private ActivityLocationSettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLocationSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SampleScreen.setUp(this, binding.toolbar, binding.content, "Jarak & waktu capture");
        if (Objects.isNull(savedInstanceState)) fill(LocationSample.settings());
        binding.presetDefault.setOnClickListener(v -> fill(LocationSample.Settings.defaults()));
        binding.presetRoute.setOnClickListener(v -> fill(new LocationSample.Settings(1, 1, 50f, true)));
        binding.save.setOnClickListener(v -> save());
    }

    private void fill(LocationSample.Settings settings) {
        binding.interval.setText(String.valueOf(settings.intervalMinutes));
        binding.minInterval.setText(String.valueOf(settings.minIntervalMinutes));
        binding.distance.setText(String.format(Locale.US, "%.0f", settings.minDistanceMetres));
        binding.gps.setChecked(settings.gps);
        binding.interval.setError(null);
        binding.minInterval.setError(null);
        binding.distance.setError(null);
    }

    private void save() {
        Long interval = number(binding.interval, 1);
        Long minInterval = number(binding.minInterval, 1);
        Long distance = number(binding.distance, 0);
        if (Objects.isNull(interval) || Objects.isNull(minInterval) || Objects.isNull(distance)) return;
        if (minInterval > interval) {
            binding.minInterval.setError("Tidak boleh lebih besar dari interval (" + interval + " menit)");
            binding.minInterval.requestFocus();
            return;
        }
        LocationSample.Settings settings = new LocationSample.Settings(interval, minInterval, distance, binding.gps.isChecked());
        LocationSample.apply(this, settings);
        Toast.makeText(this, "Dipakai: " + settings.describe(), Toast.LENGTH_LONG).show();
        finish();
    }

    /** The whole number typed, at least min; null (with the error on the field) otherwise. */
    private static Long number(EditText field, long min) {
        String text = field.getText().toString().trim();
        try {
            long value = Long.parseLong(text);
            if (value >= min) return value;
        } catch (NumberFormatException ignored) {
            // shown below
        }
        field.setError(min == 0 ? "Isi angka 0 atau lebih" : "Isi angka " + min + " atau lebih");
        field.requestFocus();
        return null;
    }
}
