package com.onevour.core.utilities.commons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

/** RefSession on SQLite: values survive a new process, the old SharedPreferences values move over, writes batch. */
@RunWith(AndroidJUnit4.class)
public class RefSessionStoreTest {

    private Application app;

    @Before
    public void setUp() {
        app = ApplicationProvider.getApplicationContext();
        fresh();
    }

    @After
    public void tearDown() {
        fresh();
    }

    /** No database, no old preferences, nothing in memory. */
    private void fresh() {
        RefSessionStore.reloadForTest();
        app.deleteDatabase(RefSessionStore.DATABASE);
        app.getSharedPreferences(RefSessionStore.LEGACY_PREFERENCES, Context.MODE_PRIVATE).edit().clear().commit();
        RefSession.clearSecureMemory();
        ContextHelper.init(app);
    }

    /** As if Android killed the process and the app started again. */
    private RefSession newProcess() {
        RefSessionStore.reloadForTest();
        RefSession.clearSecureMemory();
        ContextHelper.init(app);
        return new RefSession();
    }

    private Map<String, String> rows() {
        Map<String, String> rows = new HashMap<>();
        try (SQLiteDatabase database = SQLiteDatabase.openDatabase(app.getDatabasePath(RefSessionStore.DATABASE).getPath(), null, SQLiteDatabase.OPEN_READONLY);
             Cursor cursor = database.rawQuery("SELECT key, value FROM ref_session", null)) {
            while (cursor.moveToNext()) rows.put(cursor.getString(0), cursor.getString(1));
        }
        return rows;
    }

    @Test
    public void values_surviveANewProcess() {
        RefSession session = new RefSession();
        session.saveString("API_URL", "https://api.example.com");
        session.saveInt("PIN_TRIES", 3);
        session.saveLong("LAST_SYNC", 1_791_595_800_000L);
        session.saveFloat("RATIO", 0.25f);
        session.saveDouble("LAT", -6.200498);
        session.saveBoolean("ONBOARDED", true);
        session.saveStringSecure("API_TOKEN", "eyJ-secret");
        session.delete("API_URL");

        RefSession again = newProcess();
        assertFalse(again.contains("API_URL"));
        assertEquals(3, again.findInt("PIN_TRIES"));
        assertEquals(1_791_595_800_000L, again.findLong("LAST_SYNC"));
        assertEquals(0.25f, again.findFloat("RATIO"), 0f);
        assertEquals(-6.200498, again.findDouble("LAT"), 0d);
        assertTrue(again.findBoolean("ONBOARDED"));
        assertEquals("eyJ-secret", again.findStringSecure("API_TOKEN"));
    }

    @Test
    public void oldSharedPreferences_moveIntoTheDatabase_once() {
        RefSessionStore.reloadForTest();
        SharedPreferences legacy = app.getSharedPreferences(RefSessionStore.LEGACY_PREFERENCES, Context.MODE_PRIVATE);
        legacy.edit()
                .putString("API_TOKEN", "old-token")                    // as an older RefSession left them
                .putInt("PIN_TRIES", 2)
                .putLong("LAST_SYNC", 42L)
                .putBoolean("ONBOARDED", true)
                .putString("USER", "{\"name\":\"Budi\"}")
                .commit();

        RefSession session = newProcess();
        assertEquals("old-token", session.findString("API_TOKEN"));
        assertEquals(2, session.findInt("PIN_TRIES"));
        assertEquals(42L, session.findLong("LAST_SYNC"));
        assertTrue(session.findBoolean("ONBOARDED"));
        assertEquals("{\"name\":\"Budi\"}", session.findString("USER"));
        assertTrue(legacy.getAll().isEmpty());                          // the old file is emptied
        assertTrue(session.flush());
        assertEquals("old-token", rows().get("API_TOKEN"));

        session.saveString("API_TOKEN", "new-token");                   // later runs do not migrate again
        assertEquals("new-token", newProcess().findString("API_TOKEN"));
    }

    @Test
    public void manyWrites_endUpInTheTable_lastOneWins() {
        RefSession session = new RefSession();
        for (int i = 0; i < 200; i++) session.saveInt("COUNTER", i);
        session.saveString("NAME", "toko");
        assertEquals(199, session.findInt("COUNTER"));                  // memory first: no wait
        assertTrue(session.flush());
        Map<String, String> rows = rows();
        assertEquals("199", rows.get("COUNTER"));
        assertEquals("toko", rows.get("NAME"));
    }

    @Test
    public void theDatabase_holdsNoReadableSecret() {
        RefSession session = new RefSession();
        session.saveStringSecure("API_TOKEN", "eyJhbGciOiJIUzI1NiJ9.payload.signature");
        assertTrue(session.flush());
        String stored = rows().get("API_TOKEN" + RefSession.SECURE_SUFFIX);
        assertTrue(stored.startsWith("v1:"));
        assertFalse(stored.contains("eyJ"));
        assertNull(rows().get("API_TOKEN"));
    }
}
