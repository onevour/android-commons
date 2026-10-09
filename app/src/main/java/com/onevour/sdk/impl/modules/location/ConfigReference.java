package com.onevour.sdk.impl.modules.location;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.onevour.sdk.impl.R;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Which interval and distance give how many points per hour while moving, as a dialog: points per hour
 * fall between 60 / interval and 60 / minimum interval (the fused provider mostly delivers at the
 * minimum). Tapping a row fills the settings form.
 */
final class ConfigReference {

    private enum Battery {
        VERY_LOW, LOW, MEDIUM, HIGH
    }

    private static final class Row {

        final String title;
        final LocationSample.Settings settings;
        final String perHour;
        final Battery battery;
        final String use;

        Row(String title, long interval, long minInterval, float distance, boolean gps, String perHour, Battery battery, String use) {
            this.title = title;
            this.settings = new LocationSample.Settings(interval, minInterval, distance, gps);
            this.perHour = perHour;
            this.battery = battery;
            this.use = use;
        }
    }

    private static final List<Row> ROWS = Arrays.asList(
            new Row("Sangat jarang", 30, 20, 500f, false, "2–3", Battery.VERY_LOW, "Cukup tahu area kerja"),
            new Row("Default library", 20, 10, 500f, false, "3–6", Battery.VERY_LOW, "Kehadiran di lapangan"),
            new Row("Jarang", 15, 12, 300f, false, "4–5", Battery.LOW, "Rute kasar"),
            new Row("Sedang", 10, 8, 300f, false, "6–7,5", Battery.LOW, "Rute per jam cukup jelas"),
            new Row("Rapat", 6, 5, 200f, false, "10–12", Battery.LOW, "Garis rute dashboard lebih halus"),
            new Row("Sangat rapat", 5, 3, 200f, false, "12–20", Battery.MEDIUM, "Pantau kunjungan dekat-dekat"),
            new Row("Uji route emulator", 1, 1, 50f, true, "±60", Battery.HIGH, "Hanya untuk emulator"));

    private ConfigReference() {
    }

    static void show(Context context, Consumer<LocationSample.Settings> onPick) {
        LayoutInflater inflater = LayoutInflater.from(context);
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(16 * context.getResources().getDisplayMetrics().density);
        list.setPadding(padding, padding / 2, padding, 0);
        TextView intro = new TextView(context);
        intro.setText("Jumlah titik saat HP bergerak. Saat pindah kurang dari jarak minimum (macet, di toko) titik tidak dikirim. "
                + "Biasanya lokasi datang di interval minimum, jadi angka atas lebih sering tercapai.");
        intro.setTextColor(0xFF455A64);
        intro.setPadding(0, 0, 0, padding / 2);
        list.addView(intro);
        ScrollView scroll = new ScrollView(context);
        scroll.addView(list);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Referensi konfigurasi")
                .setView(scroll)
                .setNegativeButton("Tutup", null)
                .create();
        for (Row row : ROWS) {
            View item = inflater.inflate(R.layout.item_config_reference, list, false);
            TextView title = item.findViewById(R.id.title);
            TextView perHour = item.findViewById(R.id.per_hour);
            TextView values = item.findViewById(R.id.values);
            TextView battery = item.findViewById(R.id.battery);
            TextView use = item.findViewById(R.id.use);
            title.setText(row.title);
            SampleChips.neutral(perHour, row.perHour + " titik/jam");
            values.setText(String.format(Locale.getDefault(), "Interval %d menit · min. %d menit · %.0f m · %s",
                    row.settings.intervalMinutes, row.settings.minIntervalMinutes, row.settings.minDistanceMetres,
                    row.settings.gps ? "GPS" : "hemat baterai"));
            switch (row.battery) {
                case VERY_LOW:
                    SampleChips.good(battery, "baterai sangat hemat");
                    break;
                case LOW:
                    SampleChips.good(battery, "baterai hemat");
                    break;
                case MEDIUM:
                    SampleChips.warn(battery, "baterai sedang");
                    break;
                default:
                    SampleChips.bad(battery, "baterai boros");
                    break;
            }
            SampleChips.neutral(use, row.use);
            item.setContentDescription(String.format(Locale.getDefault(), "%s, %s titik per jam, %s, %s, %s. Ketuk untuk mengisi form",
                    row.title, row.perHour, values.getText(), battery.getText(), row.use));
            item.setOnClickListener(v -> {
                onPick.accept(row.settings);
                dialog.dismiss();
            });
            list.addView(item);
        }
        dialog.show();
    }
}
