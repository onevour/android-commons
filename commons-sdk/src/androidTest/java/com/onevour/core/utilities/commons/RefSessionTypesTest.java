package com.onevour.core.utilities.commons;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Every type RefSession keeps comes back as it was saved; missing or mistyped keys give null / the fallback. */
@RunWith(AndroidJUnit4.class)
public class RefSessionTypesTest {

    private enum Status { OPEN, CLOSED }

    public static class Visit {
        @Expose Date at;
        @Expose String note;
    }

    /** 2026-10-10 08:30:00.000 at UTC+7. */
    private static final long AT = 1_791_595_800_000L;

    private RefSession session;

    @Before
    public void setUp() {
        ContextHelper.init(ApplicationProvider.<Application>getApplicationContext());
        session = new RefSession();
    }

    @After
    public void tearDown() {
        session.delete("PRICE", "SYNCED_AT", "READ_IDS", "STOCK", "STATUS", "HASH", "DEVICE_ID", "NAME", "COUNT", "VISIT");
    }

    @Test
    public void decimal_keepsEveryDigit() {
        BigDecimal price = new BigDecimal("0.1").add(new BigDecimal("0.2"));
        session.saveDecimal("PRICE", price);
        assertEquals(new BigDecimal("0.3"), session.findDecimal("PRICE"));

        session.saveDecimal("PRICE", new BigDecimal("123456789012345678.99"));
        assertEquals(new BigDecimal("123456789012345678.99"), session.findDecimal("price"));
    }

    @Test
    public void date_comesBackToTheMillisecond() {
        Date syncedAt = new Date(1_760_000_123_456L);
        session.saveDate("SYNCED_AT", syncedAt);
        assertEquals(syncedAt, session.findDate("SYNCED_AT"));
    }

    @Test
    public void set_keepsItsOrder() {
        Set<String> ids = new LinkedHashSet<>(Arrays.asList("c", "a", "b"));
        session.saveSet("READ_IDS", ids);
        assertEquals(Arrays.asList("c", "a", "b"), Arrays.asList(session.findSet("READ_IDS").toArray()));
    }

    @Test
    public void genericType_isReadWithItsType() {
        Map<String, Integer> stock = new LinkedHashMap<>();
        stock.put("SKU-1", 5);
        stock.put("SKU-2", 0);
        session.save("STOCK", stock);
        Map<String, Integer> read = session.find("STOCK", new TypeToken<Map<String, Integer>>() {
        }.getType());
        assertEquals(stock, read);
    }

    @Test
    public void enum_byName_unknownNameGivesTheFallback() {
        session.saveEnum("STATUS", Status.CLOSED);
        assertEquals(Status.CLOSED, session.findEnum("STATUS", Status.class, Status.OPEN));

        session.saveString("STATUS", "ARCHIVED");          // a name an older / newer app does not know
        assertEquals(Status.OPEN, session.findEnum("STATUS", Status.class, Status.OPEN));
    }

    @Test
    public void bytesAndUuid_roundTrip() {
        byte[] hash = {0, 1, -128, 127, 42};
        session.saveBytes("HASH", hash);
        assertArrayEquals(hash, session.findBytes("HASH"));

        UUID deviceId = UUID.randomUUID();
        session.saveUuid("DEVICE_ID", deviceId);
        assertEquals(deviceId, session.findUuid("DEVICE_ID"));
    }

    @Test
    public void missingOrMistypedKeys_giveNullInsteadOfCrashing() {
        assertNull(session.findDecimal("PRICE"));
        assertNull(session.findDate("SYNCED_AT"));
        assertNull(session.findSet("READ_IDS"));
        assertNull(session.findUuid("DEVICE_ID"));
        assertEquals(Status.OPEN, session.findEnum("STATUS", Status.class, Status.OPEN));

        session.saveString("NAME", "toko");
        assertNull(session.findDate("NAME"));               // text, not a date
        assertNull(session.findDecimal("NAME"));
        assertNull(session.findUuid("NAME"));

        session.saveLong("COUNT", 7L);
        assertNull(session.findDecimal("COUNT"));            // a long, not text
    }

    @Test
    public void numbers_readAcrossTypes_withoutCrashing() {
        session.saveLong("COUNT", 7L);
        assertEquals(7, session.findInt("COUNT"));            // exact: converted
        assertEquals(7d, session.findDouble("COUNT"), 0d);
        assertEquals("7", session.findString("COUNT"));

        session.saveLong("COUNT", 5_000_000_000L);
        assertEquals(-1, session.findInt("COUNT", -1));        // does not fit an int: the fallback

        session.saveInt("COUNT", 5);
        assertEquals(5L, session.findLong("COUNT"));
        assertEquals(5d, session.findDouble("COUNT"), 0d);

        session.saveDouble("COUNT", 2.5);
        assertEquals(2.5d, session.findDouble("COUNT"), 0d);
        assertEquals(-1, session.findInt("COUNT", -1));        // not whole: the fallback

        session.save("COUNT", 9);                              // object path: JSON text
        assertEquals(9, session.findInt("COUNT"));

        session.saveString("COUNT", "abc");
        assertEquals(-1, session.findInt("COUNT", -1));
        assertEquals(false, session.findBoolean("COUNT"));
    }

    @Test
    public void contains_tellsMissingFromZero() {
        assertEquals(false, session.contains("COUNT"));
        assertEquals(0, session.findInt("COUNT"));
        session.saveInt("COUNT", 0);
        assertEquals(true, session.contains("count"));
        assertEquals(0, session.findInt("COUNT", -1));
    }

    @Test
    public void objectsWithDates_areStoredAsEpoch_andOldFormatsStillRead() {
        Visit visit = new Visit();
        visit.at = new Date(AT);
        visit.note = "  Toko A  ";
        session.save("VISIT", visit);
        String stored = session.findString("VISIT");
        assertEquals("{\"at\":1791595800000,\"note\":\"  Toko A  \"}", stored);    // not GsonHelper's format
        Visit read = session.find("VISIT", Visit.class);
        assertEquals(new Date(AT), read.at);
        assertEquals("  Toko A  ", read.note);                                          // kept as it was

        String[] storedByOlderApps = {
                "{\"at\":1791595800000}",                                // SDS (unix timestamp in ms)
                "{\"at\":\"2026-10-10T08:30:00.000+0700\"}",           // CSA
                "{\"at\":\"2026-10-10T08:30:00.000+07\"}",             // fuguh
        };
        for (String json : storedByOlderApps) {
            session.saveString("VISIT", json);                           // as an older version left it
            assertEquals(json, new Date(AT), session.find("VISIT", Visit.class).at);
        }
    }
}
