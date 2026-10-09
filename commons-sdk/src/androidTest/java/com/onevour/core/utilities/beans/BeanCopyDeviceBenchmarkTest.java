package com.onevour.core.utilities.beans;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The old BeanCopy against the new one on a phone (ART); logcat tag BeanCopyBenchmark, asserts nothing. */
@RunWith(AndroidJUnit4.class)
public class BeanCopyDeviceBenchmarkTest {

    private static final String TAG = "BeanCopyBenchmark";

    public static class Base {
        public long id;
        public String siteId;
        public String salesmanId;
    }

    public static class Product extends Base {
        public String code;
        public String name;
        public double price;
        public int stock;
        public String category;
        public String unit;
        public boolean active;
    }

    public static class ProductRow extends Base {
        public String code;
        public String name;
        public double price;
        public int stock;
        public String category;
        public String unit;
        public boolean active;

        public ProductRow() {
        }
    }

    /** Flat classes: the old BeanCopy only survives a second copy without inherited fields. */
    public static class FlatProduct {
        public long id;
        public String code;
        public String name;
        public double price;
        public int stock;
        public String category;
        public String unit;
        public boolean active;
    }

    public static class FlatRow {
        public long id;
        public String code;
        public String name;
        public double price;
        public int stock;
        public String category;
        public String unit;
        public boolean active;

        public FlatRow() {
        }
    }

    @Test
    public void compare() {
        List<FlatProduct> flat = new ArrayList<>();
        List<Product> inherited = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            FlatProduct f = new FlatProduct();
            f.id = i; f.code = "P" + i; f.name = "Produk " + i; f.price = 1000 + i; f.stock = i % 50; f.category = "C"; f.unit = "pcs"; f.active = true;
            flat.add(f);
            Product p = new Product();
            p.id = i; p.siteId = "S1"; p.salesmanId = "M1"; p.code = f.code; p.name = f.name; p.price = f.price; p.stock = f.stock; p.category = "C"; p.unit = "pcs"; p.active = true;
            inherited.add(p);
        }
        for (int round = 1; round <= 3; round++) {
            long oldFlat = time(() -> LegacyBeanCopy.values(flat, FlatRow.class, "id"));
            long newFlat = time(() -> BeanCopy.values(flat, FlatRow.class, "id"));
            String oldInherited;
            try {
                oldInherited = String.format(Locale.US, "%.1f ms", time(() -> LegacyBeanCopy.values(inherited, ProductRow.class)) / 1e6);
            } catch (RuntimeException e) {
                oldInherited = "crash (" + e.getCause() + ")";
            }
            long newInherited = time(() -> BeanCopy.values(inherited, ProductRow.class));
            Log.i(TAG, String.format(Locale.US,
                    "round %d, 10000 copies | flat classes: old %.1f ms, new %.1f ms | with a superclass: old %s, new %.1f ms",
                    round, oldFlat / 1e6, newFlat / 1e6, oldInherited, newInherited / 1e6));
        }
    }

    private static long time(Runnable work) {
        long start = System.nanoTime();
        work.run();
        return System.nanoTime() - start;
    }
}
