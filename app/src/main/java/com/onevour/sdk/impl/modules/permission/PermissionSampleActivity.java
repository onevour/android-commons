package com.onevour.sdk.impl.modules.permission;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.onevour.core.permission.NeedsPermission;
import com.onevour.core.permission.OnPermissionDenied;
import com.onevour.core.utilities.commons.PermissionHelper;
import com.onevour.sdk.impl.databinding.ActivityPermissionSampleBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

/**
 * Runtime permissions of android-commons: methods that run only once their groups are allowed,
 * declared with @NeedsPermission (the processor generates PermissionSampleActivityPermissions) or
 * asked inline with PermissionHelper.run.
 */
public class PermissionSampleActivity extends AppCompatActivity {

    private final SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private final PermissionHelper permission = new PermissionHelper();

    private ActivityPermissionSampleBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPermissionSampleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        com.onevour.sdk.impl.modules.location.SampleScreen.setUp(this, binding.toolbar, binding.content, "Permission");
        // annotation: call the generated ...WithPermissionCheck, not the method
        binding.photo.setOnClickListener(v -> PermissionSampleActivityPermissions.takePhotoWithPermissionCheck(this, "foto-1"));
        binding.checkIn.setOnClickListener(v -> PermissionSampleActivityPermissions.checkInWithPermissionCheck(this, "Toko A"));
        // inline: for anything else
        binding.inline.setOnClickListener(v -> permission.run(this,
                () -> log("inline: kamera diizinkan, jalan"),
                denied -> log("inline: ditolak " + denied), "camera"));
        binding.clear.setOnClickListener(v -> binding.log.setText(""));
    }

    @NeedsPermission("camera")
    void takePhoto(String name) {
        log("takePhoto(" + name + ") jalan: kamera diizinkan");
    }

    @OnPermissionDenied("camera")
    void onCameraDenied() {
        log("takePhoto tidak jalan: kamera ditolak");
    }

    @NeedsPermission({"location", "camera"})
    void checkIn(String store) {
        log("checkIn(" + store + ") jalan: lokasi dan kamera diizinkan");
    }

    @OnPermissionDenied({"camera", "location"})
    void onCheckInDenied(Set<String> deniedGroups) {
        log("checkIn tidak jalan, ditolak: " + deniedGroups);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // a base activity does this once for every screen
        PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void log(String line) {
        binding.log.append(time.format(new Date()) + "  " + line + "\n");
    }
}
