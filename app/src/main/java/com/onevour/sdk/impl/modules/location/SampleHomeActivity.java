package com.onevour.sdk.impl.modules.location;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.onevour.core.location.LocationCapture;
import com.onevour.sdk.impl.databinding.ActivitySampleHomeBinding;

import java.util.Objects;

/**
 * The home screen of a field app using the library: it only keeps the tracking in line with what the
 * app wants ({@link LocationCapture#sync} when visible), and opens the screens that stamp a position
 * themselves -- absen, check-in, register customer -- with {@link LocationCapture#current}.
 */
public class SampleHomeActivity extends AppCompatActivity {

    private ActivitySampleHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySampleHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SampleScreen.setUp(this, binding.toolbar, binding.content, "Home (simulasi)");
        binding.absen.setOnClickListener(v -> open(TransactionSampleActivity.Kind.ABSEN));
        binding.checkIn.setOnClickListener(v -> open(TransactionSampleActivity.Kind.CHECK_IN));
        binding.register.setOnClickListener(v -> open(TransactionSampleActivity.Kind.REGISTER));
        binding.history.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // visible screen: the one place the tracking may start (or stop when no longer wanted)
        LocationCapture.sync(this);
        boolean masuk = LocationSample.isWanted();
        if (masuk) SampleChips.good(binding.statusAbsen, "Sudah masuk");
        else SampleChips.neutral(binding.statusAbsen, "Belum masuk");
        if (masuk) SampleChips.good(binding.statusTracking, "Jalan di belakang");
        else SampleChips.neutral(binding.statusTracking, "Mati");
        binding.statusSettings.setText("Jarak & waktu: " + LocationSample.settings().describe());
        binding.absenValue.setText(masuk ? "Absen pulang: tracking berhenti" : "Absen masuk: tracking mulai");
    }

    private void open(TransactionSampleActivity.Kind kind) {
        startActivity(new Intent(this, TransactionSampleActivity.class).putExtra(TransactionSampleActivity.EXTRA_KIND, kind.name()));
    }
}
