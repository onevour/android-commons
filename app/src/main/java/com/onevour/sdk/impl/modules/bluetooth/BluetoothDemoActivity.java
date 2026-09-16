package com.onevour.sdk.impl.modules.bluetooth;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.onevour.core.utilities.eventbus.MessageEvent;
import com.onevour.sdk.impl.R;
import com.onevour.sdk.impl.databinding.ActivityBluetoothBinding;
import com.onevour.sdk.impl.databinding.ActivityBluetoothDemoBinding;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class BluetoothDemoActivity extends AppCompatActivity {

    public final String[] FPS = {
            "UNKNOWN",
            "JEMPOL KANAN",
            "TELUNJUK KANAN",
            "JARI TENGAH KANAN",
            "JARI MANIS KANAN",
            "KELINGKING KANAN",
            "JEMPOL KIRI",
            "TELUNJUK KIRI",
            "JARI TENGAH KIRI",
            "JARI MANIS KIRI",
            "KELINGKING KIRI"
    };

    ActivityBluetoothDemoBinding binding;

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBluetoothDemoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.event.setText(null);
        standby();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent me) {
        String event = me.getEvent();
        String message = me.getValue();
        if ("BT".equalsIgnoreCase(event)) {
            if (message.contains("|")) {
                // event
                String[] parts = message.split("\\|");
                updateEvent(Integer.parseInt(parts[0]), parts[1], message);
            } else {
                // result
            }
        }
    }

    private void updateEvent(Integer code, String message, String originalMessage) {
        if ("NFC_STANDBY".equalsIgnoreCase(message)) {
            binding.event.setText("Letakan KTP EL anda pada scanner");
        }
        if ("NFC_READ".equalsIgnoreCase(message)) {
            binding.event.setText("Sedang membaca KTP EL...");
        }
        if (code == 11) { // "FPS_SCAN".equalsIgnoreCase(message)
            String[] parts = originalMessage.split("\\|");
            if (parts.length < 4) {
                binding.event.setText("Posisi sidikjari tidak diketahuin");
                return;
            }
            int pos1 = Integer.parseInt(parts[2]);
            int pos2 = Integer.parseInt(parts[3]);
            if (pos1 > 0 && pos2 > 0) {
                binding.event.setText("Silahkan Letakan jari\n" + FPS[pos1] + " / " + FPS[pos2] + "\nAnda di Tempat yang Tersedia");
            } else {
                binding.event.setText("Posisi sidikjari tidak diketahuin");
            }
        }
        if (code == 13) {
            binding.event.setText("Verifikasi berhasil");
        }

    }

    private void standby() {
        String newLine = System.getProperty("line.separator");
        binding.info.setText("Silahkan tap KTP Elektronik Anda\n");
        binding.info.append("Mohon untuk tidak memindahkan KTP Anda\n");
        binding.info.append("Hingga proses verifikasi berhail");
    }

}