package com.onevour.sdk.impl.modules.location;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.onevour.core.location.LocationCapture;
import com.onevour.sdk.impl.R;
import com.onevour.sdk.impl.databinding.ActivityLocationHistoryBinding;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * The tracking history ({@link LocationHistory}): as a list, newest first, or on a map -- the tracking
 * as a line, absen / check-in / register as markers; the list as a timeline per day. Tapping a point
 * shows it on the map.
 */
public class HistoryActivity extends AppCompatActivity {

    private static final double SINGLE_POINT_ZOOM = 17.0;

    private final SimpleDateFormat time = new SimpleDateFormat("dd MMM HH:mm:ss", Locale.getDefault());

    private final Rows adapter = new Rows();

    private ActivityLocationHistoryBinding binding;

    private List<LocationHistory.Entry> entries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // OpenStreetMap asks every app to identify itself when downloading tiles
        Configuration.getInstance().setUserAgentValue(getPackageName());
        binding = ActivityLocationHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SampleScreen.setUp(this, binding.toolbar, binding.content, "Riwayat tracking");
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);
        binding.map.setTileSource(TileSourceFactory.MAPNIK);
        binding.map.setMultiTouchControls(true);
        binding.mode.setOnCheckedChangeListener((group, checkedId) -> showMode());
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.map.onResume();
        load();
    }

    @Override
    protected void onPause() {
        binding.map.onPause();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_location_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() != R.id.clear_history) return super.onOptionsItemSelected(item);
        new AlertDialog.Builder(this)
                .setTitle("Hapus riwayat?")
                .setMessage("Semua titik tracking dan transaksi di riwayat sample ini dihapus.")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Hapus", (dialog, which) -> {
                    LocationHistory.clear(this);
                    load();
                })
                .show();
        return true;
    }

    private void load() {
        entries = LocationHistory.all(this);
        renderSummary();
        List<LocationHistory.Entry> newestFirst = new ArrayList<>(entries);
        Collections.reverse(newestFirst);
        adapter.submit(newestFirst);
        drawMap();
        showMode();
    }

    /** Movement and accuracy of the tracking points, and how many transactions. */
    private void renderSummary() {
        int points = 0;
        int transactions = 0;
        float metres = 0;
        float accuracySum = 0;
        int withAccuracy = 0;
        Float worst = null;
        int good = 0;
        int fair = 0;
        int poor = 0;
        LocationHistory.Entry first = null;
        LocationHistory.Entry previous = null;
        for (LocationHistory.Entry entry : entries) {
            if (entry.type != LocationHistory.Type.TRACKING) {
                transactions++;
                continue;
            }
            points++;
            if (Objects.isNull(first)) first = entry;
            if (Objects.nonNull(previous)) metres += distance(previous, entry);
            previous = entry;
            if (Objects.isNull(entry.accuracy)) {
                poor++;
                continue;
            }
            accuracySum += entry.accuracy;
            withAccuracy++;
            if (Objects.isNull(worst) || entry.accuracy > worst) worst = entry.accuracy;
            if (entry.accuracy <= 20f) good++;
            else if (entry.accuracy <= LocationCapture.MAX_ACCURACY_METRES) fair++;
            else poor++;
        }
        binding.statPoints.setText(String.valueOf(points));
        binding.statDistance.setText(SampleChips.metres(metres));
        binding.statAvgMove.setText(points > 1 ? SampleChips.metres(metres / (points - 1)) : "-");
        binding.statAvgAccuracy.setText(withAccuracy > 0 ? SampleChips.metres(accuracySum / withAccuracy) : "-");
        binding.statWorstAccuracy.setText(Objects.nonNull(worst) ? SampleChips.metres(worst) : "-");
        binding.statDuration.setText(points > 1 ? duration(previous.time - first.time) : "-");
        binding.summaryDetail.setText(String.format(Locale.getDefault(),
                "Akurasi: %d titik ≤ 20 m · %d titik 21–%.0f m · %d titik lebih buruk\nTransaksi (absen, check-in, register): %d",
                good, fair, LocationCapture.MAX_ACCURACY_METRES, poor, transactions));
        binding.summary.setContentDescription(String.format(Locale.getDefault(),
                "Ringkasan: %s titik tracking, total pindah %s, rata-rata pindah %s, akurasi rata-rata %s, akurasi terburuk %s, durasi %s. %s",
                binding.statPoints.getText(), binding.statDistance.getText(), binding.statAvgMove.getText(),
                binding.statAvgAccuracy.getText(), binding.statWorstAccuracy.getText(), binding.statDuration.getText(),
                binding.summaryDetail.getText()));
    }

    private static String duration(long ms) {
        long minutes = ms / 60_000;
        if (minutes >= 60) return String.format(Locale.getDefault(), "%d jam %d mnt", minutes / 60, minutes % 60);
        if (minutes >= 1) return String.format(Locale.getDefault(), "%d mnt %d dtk", minutes, ms / 1000 % 60);
        return ms / 1000 + " dtk";
    }

    private void showMode() {
        boolean empty = entries.isEmpty();
        boolean map = binding.mode.getCheckedRadioButtonId() == R.id.mode_map;
        binding.empty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.list.setVisibility(!empty && !map ? View.VISIBLE : View.GONE);
        binding.map.setVisibility(!empty && map ? View.VISIBLE : View.GONE);
    }

    /** The tracking as a line in time order, the transactions as markers. */
    private void drawMap() {
        binding.map.getOverlays().clear();
        List<GeoPoint> track = new ArrayList<>();
        List<GeoPoint> all = new ArrayList<>();
        for (LocationHistory.Entry entry : entries) {
            GeoPoint point = new GeoPoint(entry.latitude, entry.longitude);
            all.add(point);
            if (entry.type == LocationHistory.Type.TRACKING) {
                track.add(point);
                continue;
            }
            Marker marker = new Marker(binding.map);
            marker.setPosition(point);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(entry.type.label);
            marker.setSnippet(Objects.nonNull(entry.address) ? time.format(entry.time) + "\n" + entry.address : time.format(entry.time));
            binding.map.getOverlays().add(marker);
        }
        if (track.size() > 1) {
            Polyline line = new Polyline(binding.map);
            line.setPoints(track);
            line.getOutlinePaint().setColor(ContextCompat.getColor(this, R.color.colorPrimary));
            line.getOutlinePaint().setStrokeWidth(8f);
            binding.map.getOverlays().add(0, line);
        }
        for (GeoPoint point : track) { // each tracking fix as a small dot on the line
            Marker dot = new Marker(binding.map);
            dot.setPosition(point);
            dot.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            dot.setIcon(ContextCompat.getDrawable(this, R.drawable.history_track_dot));
            dot.setInfoWindow(null);
            binding.map.getOverlays().add(dot);
        }
        binding.map.getOverlays().add(new CopyrightOverlay(this));
        binding.map.invalidate();
        if (all.isEmpty()) return;
        binding.map.addOnFirstLayoutListener((view, left, top, right, bottom) -> fit(all));
        if (binding.map.getWidth() > 0) fit(all);
    }

    private void fit(List<GeoPoint> points) {
        if (points.size() == 1) {
            binding.map.getController().setZoom(SINGLE_POINT_ZOOM);
            binding.map.getController().setCenter(points.get(0));
            return;
        }
        binding.map.zoomToBoundingBox(BoundingBox.fromGeoPoints(points).increaseByScale(1.2f), false);
    }

    private void showOnMap(LocationHistory.Entry entry) {
        binding.mode.check(R.id.mode_map);
        binding.map.post(() -> {
            binding.map.getController().setZoom(SINGLE_POINT_ZOOM);
            binding.map.getController().animateTo(new GeoPoint(entry.latitude, entry.longitude));
        });
    }

    private static float distance(LocationHistory.Entry from, LocationHistory.Entry to) {
        float[] result = new float[1];
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, result);
        return result[0];
    }

    /** The timeline, newest first, grouped by day: a day header, then its points. */
    private final class Rows extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int DAY = 0;

        private static final int POINT = 1;

        private final SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault());

        private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        /** A day (String) or a point (Entry). */
        private final List<Object> items = new ArrayList<>();

        /** Newest first. */
        private final List<LocationHistory.Entry> entries = new ArrayList<>();

        /** Points whose address is being asked. */
        private final Set<LocationHistory.Entry> asked = new HashSet<>();

        void submit(List<LocationHistory.Entry> newestFirst) {
            entries.clear();
            entries.addAll(newestFirst);
            items.clear();
            String day = null;
            for (LocationHistory.Entry entry : newestFirst) {
                String entryDay = dayFormat.format(entry.time);
                if (!entryDay.equals(day)) {
                    items.add(entryDay);
                    day = entryDay;
                }
                items.add(entry);
            }
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position) instanceof String ? DAY : POINT;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == DAY) return new DayHolder(inflater.inflate(R.layout.holder_history_day, parent, false));
            return new PointHolder(inflater.inflate(R.layout.holder_history_timeline, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof DayHolder) {
                ((DayHolder) holder).day.setText((String) items.get(position));
                return;
            }
            PointHolder point = (PointHolder) holder;
            LocationHistory.Entry entry = (LocationHistory.Entry) items.get(position);
            boolean tracking = entry.type == LocationHistory.Type.TRACKING;
            point.time.setText(timeFormat.format(entry.time));
            point.title.setText(entry.type.label);
            point.title.setTextColor(tracking ? ContextCompat.getColor(HistoryActivity.this, R.color.colorPrimaryDark) : Color.rgb(0xB0, 0x3A, 0x00));
            point.dot.setImageResource(tracking ? R.drawable.history_track_dot : R.drawable.history_event_dot);
            // the line runs through the day: none above its first point, none below its last
            point.lineTop.setVisibility(position > 0 && items.get(position - 1) instanceof LocationHistory.Entry ? View.VISIBLE : View.INVISIBLE);
            point.lineBottom.setVisibility(position + 1 < items.size() && items.get(position + 1) instanceof LocationHistory.Entry ? View.VISIBLE : View.INVISIBLE);
            point.detail.setText(String.format(Locale.getDefault(), "%.6f, %.6f", entry.latitude, entry.longitude));
            SampleChips.accuracy(point.chipAccuracy, entry.accuracy);
            LocationHistory.Entry previous = tracking ? previousTracking(entry) : null;
            point.chipMoved.setVisibility(Objects.nonNull(previous) ? View.VISIBLE : View.GONE);
            point.chipInterval.setVisibility(Objects.nonNull(previous) ? View.VISIBLE : View.GONE);
            if (Objects.nonNull(previous)) {
                SampleChips.neutral(point.chipMoved, String.format(Locale.getDefault(), "↗ pindah %s", SampleChips.metres(distance(previous, entry))));
                SampleChips.neutral(point.chipInterval, "⏱ selang " + duration(entry.time - previous.time));
            }
            bindAddress(point, entry);
            point.itemView.setOnClickListener(v -> showOnMap(entry));
            point.itemView.setContentDescription(point.time.getText() + ", " + point.title.getText() + ", "
                    + point.detail.getText() + ", " + point.chipAccuracy.getText()
                    + (Objects.nonNull(previous) ? ", " + point.chipMoved.getText() + ", " + point.chipInterval.getText() + " dari titik sebelumnya" : "")
                    + ". Ketuk untuk lihat di peta");
        }

        /** The address once asked; until then a button that asks OpenStreetMap. */
        private void bindAddress(PointHolder point, LocationHistory.Entry entry) {
            boolean known = Objects.nonNull(entry.address);
            boolean asking = asked.contains(entry);
            point.address.setVisibility(known || asking ? View.VISIBLE : View.GONE);
            point.address.setText(known ? entry.address : "Mengambil alamat dari OpenStreetMap...");
            point.getAddress.setVisibility(known || asking ? View.GONE : View.VISIBLE);
            point.getAddress.setOnClickListener(v -> {
                asked.add(entry);
                notifyItemChanged(point.getBindingAdapterPosition());
                OsmAddress.reverse(getPackageName(), entry.latitude, entry.longitude, new OsmAddress.Callback() {
                    @Override
                    public void onAddress(String address) {
                        asked.remove(entry);
                        LocationHistory.setAddress(HistoryActivity.this, entry, address);
                        refresh(entry);
                    }

                    @Override
                    public void onError(String message) {
                        asked.remove(entry);
                        refresh(entry);
                        Toast.makeText(HistoryActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
            });
        }

        private void refresh(LocationHistory.Entry entry) {
            int position = items.indexOf(entry);
            if (position >= 0) notifyItemChanged(position);
        }

        /** The tracking point before this one, across days (entries are newest first). */
        private LocationHistory.Entry previousTracking(LocationHistory.Entry entry) {
            for (int i = entries.indexOf(entry) + 1; i < entries.size(); i++) {
                if (entries.get(i).type == LocationHistory.Type.TRACKING) return entries.get(i);
            }
            return null;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        final class DayHolder extends RecyclerView.ViewHolder {

            final TextView day;

            DayHolder(View view) {
                super(view);
                day = view.findViewById(R.id.day);
            }
        }

        final class PointHolder extends RecyclerView.ViewHolder {

            final TextView time;

            final TextView title;

            final TextView detail;

            final ImageView dot;

            final TextView address;

            final TextView chipAccuracy;

            final TextView chipMoved;

            final TextView chipInterval;

            final Button getAddress;

            final View lineTop;

            final View lineBottom;

            PointHolder(View view) {
                super(view);
                time = view.findViewById(R.id.time);
                title = view.findViewById(R.id.title);
                detail = view.findViewById(R.id.detail);
                dot = view.findViewById(R.id.dot);
                address = view.findViewById(R.id.address);
                chipAccuracy = view.findViewById(R.id.chip_accuracy);
                chipMoved = view.findViewById(R.id.chip_moved);
                chipInterval = view.findViewById(R.id.chip_interval);
                getAddress = view.findViewById(R.id.get_address);
                lineTop = view.findViewById(R.id.line_top);
                lineBottom = view.findViewById(R.id.line_bottom);
            }
        }
    }
}
