package com.onevour.core.utilities.commons;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;

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
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The *Secure methods: every type comes back, nothing is readable in the file, a lost key reads as missing. */
@RunWith(AndroidJUnit4.class)
public class RefSessionSecureTest {

    private enum Role { SALES, DRIVER }

    public static class Profile {
        @Expose String name;
        @Expose int level;

        Profile(String name, int level) {
            this.name = name;
            this.level = level;
        }
    }

    private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJidWRpIn0.secret-signature";

    private Application app;

    private RefSession session;

    @Before
    public void setUp() {
        app = ApplicationProvider.getApplicationContext();
        ContextHelper.init(app);
        session = new RefSession();
        session.clearSecure();
        session.delete("PLAIN_NAME");
    }

    @After
    public void tearDown() {
        session.clearSecure();
        session.delete("PLAIN_NAME");
    }

    @Test
    public void everyType_roundTrips_alsoInANewProcess() {
        UUID deviceId = UUID.randomUUID();
        Date loginAt = new Date(1_760_000_123_456L);
        Map<String, Long> limits = new LinkedHashMap<>();
        limits.put("daily", 9_007_199_254_740_993L);              // above 2^53: no precision lost

        session.saveStringSecure("API_TOKEN", TOKEN);
        session.saveIntSecure("PIN_TRIES", 3);
        session.saveLongSecure("USER_ID", 123_456_789_012L);
        session.saveFloatSecure("RATIO", 0.25f);
        session.saveDoubleSecure("LAT", -6.200498);
        session.saveBooleanSecure("BIOMETRIC", true);
        session.saveSecure("PROFILE", new Profile("Budi", 2));
        session.saveSecure("LIMITS", limits);
        session.saveCollectionSecure("TEAM", Collections.singletonList(new Profile("Sari", 1)));
        session.saveDecimalSecure("BALANCE", new BigDecimal("1500000.75"));
        session.saveDateSecure("LOGIN_AT", loginAt);
        session.saveSetSecure("SCOPES", new LinkedHashSet<>(Arrays.asList("read", "write")));
        session.saveEnumSecure("ROLE", Role.DRIVER);
        session.saveBytesSecure("SALT", new byte[]{1, 2, 3, -1});
        session.saveUuidSecure("DEVICE_ID", deviceId);

        for (int pass = 0; pass < 2; pass++) {
            assertEquals(TOKEN, session.findStringSecure("API_TOKEN"));
            assertEquals(3, session.findIntSecure("PIN_TRIES"));
            assertEquals(123_456_789_012L, session.findLongSecure("USER_ID"));
            assertEquals(0.25f, session.findFloatSecure("RATIO"), 0f);
            assertEquals(-6.200498, session.findDoubleSecure("LAT"), 0d);
            assertTrue(session.findBooleanSecure("BIOMETRIC"));
            Profile profile = session.findSecure("PROFILE", Profile.class);
            assertEquals("Budi", profile.name);
            assertEquals(2, profile.level);
            Map<String, Long> readLimits = session.findSecure("LIMITS", new TypeToken<Map<String, Long>>() {
            }.getType());
            assertEquals(limits, readLimits);
            List<Profile> team = session.findCollectionSecure("TEAM", Profile.class);
            assertEquals("Sari", team.get(0).name);
            assertEquals(new BigDecimal("1500000.75"), session.findDecimalSecure("BALANCE"));
            assertEquals(loginAt, session.findDateSecure("LOGIN_AT"));
            assertEquals(Arrays.asList("read", "write"), Arrays.asList(session.findSetSecure("SCOPES").toArray()));
            assertEquals(Role.DRIVER, session.findEnumSecure("ROLE", Role.class, Role.SALES));
            assertArrayEquals(new byte[]{1, 2, 3, -1}, session.findBytesSecure("SALT"));
            assertEquals(deviceId, session.findUuidSecure("DEVICE_ID"));
            RefSession.clearSecureMemory();                     // second pass decrypts from the file
        }
    }

    @Test
    public void theFile_holdsNoReadableSecret() {
        session.saveStringSecure("API_TOKEN", TOKEN);
        session.saveLongSecure("USER_ID", 123_456_789_012L);
        Map<String, ?> stored = app.getSharedPreferences("RefSession", Context.MODE_PRIVATE).getAll();
        assertTrue(stored.containsKey("API_TOKEN" + RefSession.SECURE_SUFFIX));
        for (Map.Entry<String, ?> entry : stored.entrySet()) {
            if (!entry.getKey().endsWith(RefSession.SECURE_SUFFIX)) continue;
            String text = String.valueOf(entry.getValue());
            assertTrue(text.startsWith("v1:"));
            assertFalse(text.contains("eyJ"));
            assertFalse(text.contains("123456789012"));
        }
        assertNull(session.findString("API_TOKEN"));             // not under the plain key
    }

    @Test
    public void lostKey_readsAsMissing_andTheValueIsRemoved() throws Exception {
        session.saveStringSecure("API_TOKEN", TOKEN);
        RefSession.clearSecureMemory();
        RefSessionCipher.deleteKey();                            // like a backup restored on another phone

        assertNull(session.findStringSecure("API_TOKEN"));
        assertFalse(session.containsSecure("API_TOKEN"));
        assertFalse(session.contains("API_TOKEN" + RefSession.SECURE_SUFFIX));

        session.saveStringSecure("API_TOKEN", TOKEN);            // a new key is made, saving works again
        assertEquals(TOKEN, session.findStringSecure("API_TOKEN"));
    }

    @Test
    public void clearSecure_forgetsSecretsOnly_andMissingKeysGiveFallbacks() {
        assertNull(session.findStringSecure("API_TOKEN"));
        assertEquals(7, session.findIntSecure("PIN_TRIES", 7));
        assertEquals(Role.SALES, session.findEnumSecure("ROLE", Role.class, Role.SALES));

        session.saveStringSecure("API_TOKEN", TOKEN);
        session.saveIntSecure("PIN_TRIES", 3);
        session.saveString("PLAIN_NAME", "toko");
        session.deleteSecure("PIN_TRIES");
        assertFalse(session.containsSecure("PIN_TRIES"));
        assertTrue(session.containsSecure("API_TOKEN"));

        session.clearSecure();
        assertFalse(session.containsSecure("API_TOKEN"));
        assertEquals("toko", session.findString("PLAIN_NAME"));  // plain values stay
    }
}
