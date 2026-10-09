package com.onevour.core.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class FixFilterTest {

    private static final long T0 = 1_700_000_000_000L;

    private long lastFix;
    private FixFilter filter;

    @Before
    public void setUp() {
        lastFix = 0;
        filter = new FixFilter(new FixFilter.LastFix() {
            @Override
            public long time() {
                return lastFix;
            }

            @Override
            public void save(long time) {
                lastFix = time;
            }
        }, LocationCapture.MIN_GAP_MS);
    }

    @Test
    public void aRealAccurateFix_isDelivered() {
        LocationFix fix = filter.accept(-6.2, 106.8, 12f, T0, false);

        assertNotNull(fix);
        assertEquals(-6.2, fix.getLatitude(), 0.0);
        assertEquals(Float.valueOf(12f), fix.getAccuracy());
        assertEquals(T0, lastFix);
    }

    @Test
    public void fakeGps_isDropped() {
        assertNull(filter.accept(-6.2, 106.8, 12f, T0, true));
        assertEquals(0, lastFix);
    }

    @Test
    public void inaccurateOrUnknownAccuracy_isDropped() {
        assertNull(filter.accept(-6.2, 106.8, 150f, T0, false));
        assertNull(filter.accept(-6.2, 106.8, null, T0, false));
        assertNotNull(filter.accept(-6.2, 106.8, 100f, T0, false));
    }

    @Test
    public void nearZeroZero_orWithoutTime_isDropped() {
        assertNull(filter.accept(0.0, 0.0, 12f, T0, false));
        assertNull(filter.accept(-6.2, 106.8, 12f, 0, false));
    }

    @Test
    public void aFixTooSoonAfterTheLastDelivered_isADuplicate() {
        assertNotNull(filter.accept(-6.2, 106.8, 12f, T0, false));
        assertNull(filter.accept(-6.3, 106.8, 12f, T0 + LocationCapture.MIN_GAP_MS - 1, false));
        assertNotNull(filter.accept(-6.4, 106.8, 12f, T0 + LocationCapture.MIN_GAP_MS, false));
        assertEquals(T0 + LocationCapture.MIN_GAP_MS, lastFix);
    }

    @Test
    public void theDefaultGap_letsTheNextFixOfTheShortestIntervalThrough() {
        assertEquals(TimeUnit.MINUTES.toMillis(9), LocationCapture.MIN_GAP_MS);
    }
}
