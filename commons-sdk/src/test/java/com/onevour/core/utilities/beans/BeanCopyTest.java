package com.onevour.core.utilities.beans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** The copy rules stay as they were; the cache no longer mixes ignore lists, classes or inherited fields. */
public class BeanCopyTest {

    public static class Base {
        public long id;
        public String siteId;
    }

    public static class Order extends Base {
        public String name;
        public int qty;
        public Integer total;
        public transient String note;
        public static int created;
    }

    public static class OrderRow extends Base {
        public String NAME;                      // other case: still copied
        public int qty;
        public int total;                        // Integer vs int: not copied, as before
        public String note;
        public static int created;

        public OrderRow() {
        }
    }

    public static class First {
        public static class User {
            public String name;
            public int age;

            public User() {
            }
        }
    }

    public static class Second {
        public static class User {               // same simple name, other class
            public String name;
            public String phone;

            public User() {
            }
        }
    }

    public static class Visit {
        public Date at;
        public double lat;
        public List<String> tags = new ArrayList<>();

        public Visit() {
        }
    }

    private Order order;

    @Before
    public void setUp() {
        BeanCopy.clearMappings();
        order = new Order();
        order.id = 7;
        order.siteId = "S1";
        order.name = "Toko A";
        order.qty = 3;
        order.total = 15_000;
        order.note = "transient";
        Order.created = 99;
        OrderRow.created = 0;
    }

    @Test
    public void copiesSameNameAndType_inheritedToo_skipsTransientStaticAndOtherTypes() {
        OrderRow row = BeanCopy.value(order, OrderRow.class);
        assertEquals(7, row.id);
        assertEquals("S1", row.siteId);
        assertEquals("Toko A", row.NAME);
        assertEquals(3, row.qty);
        assertEquals(0, row.total);              // Integer -> int stays out
        assertNull(row.note);                    // transient in the source
        assertEquals(0, OrderRow.created);       // static fields are not copied
    }

    @Test
    public void theSecondCopy_ofInheritedOrOtherCaseFields_doesNotCrash() {
        BeanCopy.value(order, OrderRow.class);
        order.siteId = "S2";
        order.name = "Toko B";
        OrderRow again = BeanCopy.value(order, OrderRow.class);     // the old cache threw NoSuchFieldException here
        assertEquals("S2", again.siteId);
        assertEquals("Toko B", again.NAME);
    }

    @Test
    public void eachIgnoreList_isHonoured_whateverCameFirst() {
        OrderRow first = new OrderRow();
        BeanCopy.copyValue(order, first, "id");
        assertEquals(0, first.id);
        assertEquals("S1", first.siteId);

        OrderRow second = new OrderRow();
        BeanCopy.copyValue(order, second, "siteId", "qty");         // the old cache reused the "id" list
        assertEquals(7, second.id);
        assertNull(second.siteId);
        assertEquals(0, second.qty);

        OrderRow third = new OrderRow();
        BeanCopy.copyValue(order, third, "qty", "siteId");          // same list in another order: same mapping
        assertNull(third.siteId);
        assertEquals(7, third.id);
    }

    @Test
    public void classesWithTheSameSimpleName_doNotShareAMapping() {
        First.User source = new First.User();
        source.name = "Budi";
        source.age = 30;
        First.User copy = BeanCopy.value(source, First.User.class);
        Second.User other = BeanCopy.value(source, Second.User.class);
        assertEquals(30, copy.age);
        assertEquals("Budi", other.name);
        assertNull(other.phone);
    }

    @Test
    public void manyThreads_copyCorrectly() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            final int thread = t;
            results.add(pool.submit(() -> {
                for (int i = 0; i < 2_000; i++) {
                    Order source = new Order();
                    source.id = thread * 10_000L + i;
                    source.name = "n" + i;
                    OrderRow row = new OrderRow();
                    BeanCopy.copyValue(source, row, i % 2 == 0 ? new String[]{"qty"} : new String[]{});
                    if (row.id != source.id || !source.name.equals(row.NAME)) return false;
                }
                return true;
            }));
        }
        for (Future<Boolean> result : results) assertTrue(result.get());
        pool.shutdown();
    }

    @Test
    public void gson_isADeepCopy_keepingMillisecondsAndNaN() {
        Visit visit = new Visit();
        visit.at = new Date(1_791_595_800_123L);
        visit.lat = Double.NaN;
        visit.tags.addAll(Arrays.asList("a", "b"));
        Visit copy = BeanCopy.gson(visit, Visit.class);
        assertEquals(visit.at, copy.at);                            // milliseconds kept
        assertTrue(Double.isNaN(copy.lat));
        assertEquals(visit.tags, copy.tags);
        assertNotSame(visit.tags, copy.tags);
    }

    @Test(expected = NullPointerException.class)
    public void nullSource_isRefused() {
        BeanCopy.copyValue(null, new OrderRow());
    }
}
