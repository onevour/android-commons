package com.onevour.core.utilities.commons;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

/**
 * Old RefSession (SharedPreferences, commit() on every save) against the new one (SQLite behind a
 * memory copy). Prints the times to logcat, tag RefSessionBenchmark; measures, asserts nothing.
 */
@RunWith(AndroidJUnit4.class)
public class RefSessionBenchmarkTest {

    private static final String TAG = "RefSessionBenchmark";

    private static final String OLD_FILE = "RefSessionBenchmarkOld";

    private static final int WRITES = 200;

    private static final int READS = 1000;

    private static final int ROUNDS = 3;

    private final Application app = ApplicationProvider.getApplicationContext();

    @Test
    public void compare() {
        String bigList = bigJson(50_000);                            // a cached list of ~50 KB, like a menu or master data
        for (int round = 1; round <= ROUNDS; round++) {
            for (boolean withBigList : new boolean[]{false, true}) {
                SharedPreferences old = resetOld(withBigList ? bigList : null);
                RefSession session = resetNew(withBigList ? bigList : null);

                long oldWrite = time(() -> {
                    for (int i = 0; i < WRITES; i++) old.edit().putString("KEY_" + (i % 20), "value " + i).commit();
                });
                long newWrite = time(() -> {
                    for (int i = 0; i < WRITES; i++) session.saveString("KEY_" + (i % 20), "value " + i);
                });
                long newFlush = time(session::flush);

                long oldRead = time(() -> {
                    for (int i = 0; i < READS; i++) old.getString("KEY_" + (i % 20), null);
                });
                long newRead = time(() -> {
                    for (int i = 0; i < READS; i++) session.findString("KEY_" + (i % 20));
                });

                // cold start: the first read after the app starts
                long oldLoad = time(() -> {
                    SharedPreferences fresh = coldOld();
                    fresh.getString("KEY_1", null);
                });
                long newLoad = time(() -> {
                    RefSessionStore.reloadForTest();
                    ContextHelper.init(app);
                    new RefSession().findString("KEY_1");
                });

                Log.i(TAG, String.format(Locale.US,
                        "round %d, %s | %d saves: old %.1f ms, new %.1f ms (+ flush %.1f ms) | %d reads: old %.2f ms, new %.2f ms | first read after start: old %.1f ms, new %.1f ms",
                        round, withBigList ? "file with a 50 KB list" : "small file",
                        WRITES, ms(oldWrite), ms(newWrite), ms(newFlush),
                        READS, ms(oldRead), ms(newRead), ms(oldLoad), ms(newLoad)));
            }
        }
        resetOld(null);
        resetNew(null);
    }

    private SharedPreferences resetOld(String bigList) {
        SharedPreferences old = app.getSharedPreferences(OLD_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = old.edit().clear();
        if (bigList != null) editor.putString("MENU", bigList);
        editor.commit();
        return old;
    }

    private RefSession resetNew(String bigList) {
        RefSessionStore.reloadForTest();
        app.deleteDatabase(RefSessionStore.DATABASE);
        ContextHelper.init(app);
        RefSession session = new RefSession();
        if (bigList != null) session.saveString("MENU", bigList);
        session.flush();
        return session;
    }

    /** SharedPreferences caches a file per process: a new name forces the read from disk. */
    private SharedPreferences coldOld() {
        String copy = OLD_FILE + "Cold" + System.nanoTime();
        SharedPreferences source = app.getSharedPreferences(OLD_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = app.getSharedPreferences(copy, Context.MODE_PRIVATE).edit();
        for (java.util.Map.Entry<String, ?> entry : source.getAll().entrySet()) editor.putString(entry.getKey(), String.valueOf(entry.getValue()));
        editor.commit();
        // a fresh name was cached by the write above; delete and reopen through a new instance is not possible,
        // so measure the parse of an equivalent file read cold through a second, never-opened name
        String cold = copy + "R";
        new java.io.File(app.getDataDir(), "shared_prefs/" + copy + ".xml").renameTo(new java.io.File(app.getDataDir(), "shared_prefs/" + cold + ".xml"));
        return app.getSharedPreferences(cold, Context.MODE_PRIVATE);
    }

    private static String bigJson(int size) {
        StringBuilder json = new StringBuilder("[");
        int i = 0;
        while (json.length() < size) {
            if (i > 0) json.append(',');
            json.append("{\"id\":").append(i).append(",\"name\":\"Produk ").append(i).append("\",\"price\":").append(1000 + i).append('}');
            i++;
        }
        return json.append(']').toString();
    }

    private static long time(Runnable work) {
        long start = System.nanoTime();
        work.run();
        return System.nanoTime() - start;
    }

    private static double ms(long nanos) {
        return nanos / 1_000_000d;
    }
}
