package com.onevour.core.utilities.beans;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The old BeanCopy against the new one on the same copies; prints times, asserts nothing. */
public class BeanCopyBenchmarkTest {

    public static class Product {
        public long id;
        public String code;
        public String name;
        public double price;
        public int stock;
        public String category;
        public String unit;
        public boolean active;
    }

    public static class ProductRow {
        public long id;
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

    @Test
    public void compare() {
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            Product product = new Product();
            product.id = i;
            product.code = "P" + i;
            product.name = "Produk " + i;
            product.price = 1000 + i;
            product.stock = i % 50;
            product.category = "C" + (i % 9);
            product.unit = "pcs";
            product.active = true;
            products.add(product);
        }
        for (int round = 1; round <= 3; round++) {
            long oldStart = System.nanoTime();
            LegacyBeanCopy.values(products, ProductRow.class, "id");
            long oldTime = System.nanoTime() - oldStart;
            long newStart = System.nanoTime();
            BeanCopy.values(products, ProductRow.class, "id");
            long newTime = System.nanoTime() - newStart;
            System.out.printf(Locale.US, "BeanCopy round %d, 10000 copies: old %.1f ms, new %.1f ms%n",
                    round, oldTime / 1e6, newTime / 1e6);
        }
    }
}
